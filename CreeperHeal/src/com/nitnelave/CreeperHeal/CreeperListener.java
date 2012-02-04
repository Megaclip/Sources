package com.nitnelave.CreeperHeal;



import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.EndermanPickupEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.material.Rails;


public class CreeperListener implements Listener{

	private static CreeperHeal plugin;


	public CreeperListener(CreeperHeal instance)        //declaration of the plugin dependence, or something like that
	{
		plugin = instance;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityExplode(EntityExplodeEvent event) {//explosion
		WorldConfig world = getWorld(event.getLocation().getWorld());

		if(!event.isCancelled()) {        //if there actually is an explosion
			Entity entity = event.getEntity();
			String should = shouldReplace(entity, world);
			if(!should.equalsIgnoreCase("false"))
				recordBlocks(event, world, should);
		}

	}

	private void recordBlocks(EntityExplodeEvent event, WorldConfig world, String should) {
		plugin.recordBlocks(event, world, should);
	}

	private WorldConfig getWorld(World w) {
		return plugin.loadWorld(w);
	}


	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityDamage(EntityDamageEvent event)
	{
		if(event instanceof EntityDamageByEntityEvent)
		{
			EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)event;
			Entity entity = e.getDamager();
			WorldConfig world = getWorld(entity.getWorld());

			if(!shouldReplace(entity, world).equalsIgnoreCase("false"))         //if it's a creeper, and creeper explosions are recorded
				plugin.checkForPaintings(e);
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		plugin.log_info("block_break!", 3);
		if(!(event.isCancelled())) {
			Block block = event.getBlock();
			if(block.getType() == Material.TNT) {
				plugin.log_info("breaking tnt", 2);
				if(plugin.isTrap(block)){
					event.setCancelled(!plugin.deleteTrap(event.getPlayer()));
					plugin.log_info("breaking trap", 2);
				}
			}
		}
	}

	@EventHandler
	public void onBlockPhysics(BlockPhysicsEvent event)
	{
		Block b = event.getBlock();
		if(b.getState() instanceof Rails)
		{
			if(plugin.preventUpdate.containsKey(b.getState()))
				event.setCancelled(true);
		}
		else if(b.getType() == Material.VINE)
		{
			Location vineLoc = b.getLocation();
			World w = vineLoc.getWorld();
			for(Location loc : plugin.explosionList.keySet())
			{
				if(loc.getWorld() == w)
				{
					if(loc.distance(vineLoc) < 20)
					{
						event.setCancelled(true);
						return;
					}
				}
			}
			for(Location loc : plugin.fireList.keySet())
			{
				if(loc.getWorld() == w)
				{
					if(loc.distance(vineLoc) < 10)
					{
						event.setCancelled(true);
						return;
					}
				}
			}
		}
		else if(CreeperHeal.blocks_physics.contains(b.getTypeId()))
		{
			Location bLoc = b.getLocation();
			World w = bLoc.getWorld();
			Set<Location> set = plugin.preventBlockFall.keySet();
			try{
				if(plugin.config.preventBlockFall)
					set.addAll(plugin.explosionList.keySet());
			}catch(UnsupportedOperationException e){}
			for(Location loc : plugin.preventBlockFall.keySet())
			{
				if(loc.getWorld() == w)
				{
					if(loc.distance(bLoc) < 10)
					{
						event.setCancelled(true);
						return;
					}
				}
			}
		}
	}

	@EventHandler
	public void onLeavesDecay(LeavesDecayEvent e)
	{
		Location leafLoc = e.getBlock().getLocation();
		World w = leafLoc.getWorld();
		for(Location loc : plugin.explosionList.keySet())
		{
			if(loc.getWorld() == w)
			{
				if(loc.distance(leafLoc) < 20)
				{
					e.setCancelled(true);
					return;
				}
			}
		}
		for(Location loc : plugin.fireList.keySet())
		{
			if(loc.getWorld() == w)
			{
				if(loc.distance(leafLoc) < 5)
				{
					e.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBurn(BlockBurnEvent event) {        //no need to check for the setting, the listener only gets declared if it is set to true
		WorldConfig world = plugin.loadWorld( event.getBlock().getLocation().getWorld());

		if(!world.fire.equalsIgnoreCase("false"))
			plugin.record_burn(event.getBlock());
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onEndermanPickup(EndermanPickupEvent event) {//explosion
		WorldConfig world = getWorld(event.getBlock().getWorld());

		if(world.enderman)
			event.setCancelled(true);
	}


	private String shouldReplace(Entity entity, WorldConfig world)
	{

		if(entity != null) {

			if( entity instanceof Creeper)         //if it's a creeper, and creeper explosions are recorded
			{
				if(world.replaceAbove)
				{
					if(CreeperUtils.isAbove(entity, world.replaceLimit))
						return world.creepers;
					return "false";
				}
				return world.creepers;
			}
			else if(entity instanceof TNTPrimed)                 //tnt -- it checks if it's a trap.
				if(world.replaceAbove){
					if(CreeperUtils.isAbove(entity, world.replaceLimit))
						return world.tnt;
					return "false";
				}
				else
					return world.tnt;

			else if(entity instanceof Fireball)         //fireballs (shot by ghasts)
				if(world.replaceAbove){
					if(CreeperUtils.isAbove(entity, world.replaceLimit))
						return world.ghast;
					return "false";
				}
				else
					return world.ghast;

			else if(entity instanceof EnderDragon)

				return world.dragons;

			else if(!(entity instanceof Creeper) && !(entity instanceof TNTPrimed) && !(entity instanceof Fireball) && !(entity instanceof EnderDragon))        //none of it, another custom entity

				return world.magical;

		}
		else
			return world.magical;

		return "false";
	}


}
