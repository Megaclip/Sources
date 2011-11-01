package com.nitnelave.CreeperHeal;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPhysicsEvent;

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
		if(plugin.preventUpdate.containsKey(event.getBlock().getState()))
			event.setCancelled(true);
	}

}
