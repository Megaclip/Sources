package com.nitnelave.CreeperHeal;

import org.bukkit.Location;
import org.bukkit.Material;
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
			if(plugin.preventUpdate.containsKey(event.getBlock().getState()))
				event.setCancelled(true);
		}
		else if(b.getType() == Material.VINE)
		{
			Location vineLoc = event.getBlock().getLocation();
			for(Location loc : plugin.explosionList.keySet())
			{
				if(loc.distance(vineLoc) < 20)
				{
					event.setCancelled(true);
					return;
				}
			}
			for(Location loc : plugin.fireList.keySet())
			{
				if(loc.distance(vineLoc) < 10)
				{
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@Override
	public void onLeavesDecay(LeavesDecayEvent e)
	{
		Location leafLoc = e.getBlock().getLocation();
		for(Location loc : plugin.explosionList.keySet())
		{
			if(loc.distance(leafLoc) < 20)
			{
				e.setCancelled(true);
				return;
			}
		}
		for(Location loc : plugin.fireList.keySet())
		{
			if(loc.distance(leafLoc) < 5)
			{
				e.setCancelled(true);
				return;
			}
		}
	}


}
