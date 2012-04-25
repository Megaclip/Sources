package com.nitnelave.CreeperHeal;

import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityExplodeEvent;

public class CreeperHandler
{

	private CreeperHeal plugin;
	
	
	public CreeperHandler(CreeperHeal instance)
    {
	   plugin = instance;
    }
	
	public void recordBlocks(List<Block> list, Entity source)
	{
		plugin.recordBlocks(list, source.getLocation(), source, CreeperUtils.shouldReplace(source, plugin.loadWorld(source.getWorld())));
	}
	
	public void recordBlocks(List<Block> list, Entity source, String should)
	{
		plugin.recordBlocks(list, source.getLocation(), source, should);
	}

	public void recordBlocks(EntityExplodeEvent event, WorldConfig world, String should) 
	{
		plugin.recordBlocks(event, world, should);
	}

}
