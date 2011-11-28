package com.nitnelave.CreeperHeal;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.material.Rails;

public class TNTBreakListener extends BlockListener {

	CreeperHeal plugin;

	public TNTBreakListener (CreeperHeal instance) {
		plugin = instance;
	}

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

	@Override
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

	@Override
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


}
