package com.nitnelave.CreeperHeal;

import java.util.Date;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

public class AddTrapRunnable implements Runnable{

	private Date date;
	private Block block;
	private CreeperHeal plugin;
	
	public AddTrapRunnable(Date d, Block b, CreeperHeal p)
	{
		date = d;
		block = b;
		plugin = p;
	}
	
	
	@Override
	public void run() {
		BlockState tmp_state = block.getState();

		block.setType(Material.TNT);                            //set the block to tnt

		plugin.map.get(date).add(block.getState());                //record it

		tmp_state.update(true);        //set it back to what it was
		
	}

}
