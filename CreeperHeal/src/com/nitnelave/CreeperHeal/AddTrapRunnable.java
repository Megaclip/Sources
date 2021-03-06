package com.nitnelave.CreeperHeal;

import java.util.Date;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class AddTrapRunnable implements Runnable{

	private Date date;
	private Block block;
	private CreeperHeal plugin;
	private Material type;
	
	public AddTrapRunnable(Date d, Block b, CreeperHeal p, Material t)
	{
		date = d;
		block = b;
		plugin = p;
		type = t;
	}
	
	
	@Override
	public void run() {
		BlockState tmp_state = block.getState();

		block.setType(type);                            //set the block to tnt

		List<BlockState> list = plugin.map.get(date);
		list.add(block.getState());                //record it

		tmp_state.update(true);        //set it back to what it was
		
	}

}
