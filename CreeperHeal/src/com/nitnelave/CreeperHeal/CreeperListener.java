package com.nitnelave.CreeperHeal;



import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityListener;


public class CreeperListener extends EntityListener{

	private static CreeperHeal plugin;


	public CreeperListener(CreeperHeal instance)        //declaration of the plugin dependence, or something like that
	{
		plugin = instance;

	}

	public void onEntityExplode(EntityExplodeEvent event) {//explosion
		WorldConfig world = getWorld(event.getLocation().getWorld());

		if(!event.isCancelled()) {        //if there actually is an explosion
			Entity entity = event.getEntity();
			if(shouldReplace(entity, world))
				recordBlocks(event, world);
		}
		
	}

	private void recordBlocks(EntityExplodeEvent event, WorldConfig world) {
		plugin.recordBlocks(event, world);
	}

	private WorldConfig getWorld(World w) {
		return plugin.loadWorld(w);
	}


	@Override
	public void onEntityDamage(EntityDamageEvent event)
	{
		if(event instanceof EntityDamageByEntityEvent)
		{
			EntityDamageByEntityEvent e = (EntityDamageByEntityEvent)event;
			Entity entity = e.getDamager();
			WorldConfig world = getWorld(entity.getWorld());

			if(shouldReplace(entity, world))         //if it's a creeper, and creeper explosions are recorded
				plugin.checkForPaintings(e);
		}
	}
	
	private boolean shouldReplace(Entity entity, WorldConfig world)
	{

		if(entity != null) {

			if( entity instanceof Creeper && world.creepers)         //if it's a creeper, and creeper explosions are recorded
				return true;
			else if(entity instanceof TNTPrimed && (world.tnt /*|| plugin.isTrap(entity)*/))                 //tnt -- it checks if it's a trap.
				if(world.replaceAbove){
					if(CreeperUtils.isAbove(entity, world.replaceLimit))
						return true;
				}
				else
					return true;

			else if(entity instanceof Fireball && world.ghast)         //fireballs (shot by ghasts)

				return true;

			else if(!(entity instanceof Creeper) && !(entity instanceof TNTPrimed) && !(entity instanceof Fireball) && world.magical)        //none of it, another custom entity

				return true;

		}

		else if(world.magical) {

			return true;

		}      
		return false;
	}
	

}
