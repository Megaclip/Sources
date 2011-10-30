package com.nitnelave.CreeperHeal;



import org.bukkit.World;
import org.bukkit.event.entity.EndermanPickupEvent;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.painting.PaintingBreakEvent;


public class EnderListener extends EntityListener{

	private static CreeperHeal plugin;


	public EnderListener(CreeperHeal instance)        //declaration of the plugin dependence, or something like that
	{
		plugin = instance;

	}

	public void onEndermanPickup(EndermanPickupEvent event) {//explosion
		WorldConfig world = getWorld(event.getBlock().getWorld());
		
		if(world.enderman)
			event.setCancelled(true);
	}

	private WorldConfig getWorld(World w) {
		return plugin.loadWorldConfig(w);
	}

	public void onPaintingBreak(PaintingBreakEvent event)
	{
		if(event.getCause() == PaintingBreakEvent.RemoveCause.ENTITY)
			plugin.record_painting(event.getPainting());
	}

}
