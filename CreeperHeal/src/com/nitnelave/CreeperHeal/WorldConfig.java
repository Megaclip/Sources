package com.nitnelave.CreeperHeal;

import java.util.ArrayList;


public class WorldConfig {

	public boolean creepers, tnt, fire, ghast, magical, replace_tnt, replaceAbove, enderman, dragons;
	public String restrict_blocks, name;
	public int replaceLimit;
	public ArrayList<BlockId> block_list = new ArrayList<BlockId>();
	


	public WorldConfig(String world_name) {
		creepers = tnt = ghast = fire = true;
		magical = replace_tnt = replaceAbove = enderman = dragons = false;
		restrict_blocks = "false";
		block_list = new ArrayList<BlockId>();        //sample whitelist
		int[] tmp_list = { 1,2,3,9,11,12,13,14,15,16,17,18,21,24,31,32,37,38,39,40,48,49,56,73,79,81,82,86,87,88,89 };
		for(int k : tmp_list)
			block_list.add(new BlockId(k));
		name = world_name;

	}

	public WorldConfig(String world_name, boolean tmp_creeper, boolean tmp_tnt, boolean tmp_ghast, boolean tmp_fire, boolean tmp_magical, boolean tmp_replace_tnt, String restrictBlocks, ArrayList<BlockId> blockList, boolean tmp_replaceAbove, int tmp_replaceLimit, boolean tmp_enderman, boolean dragons) {

		creepers = tmp_creeper;

		tnt = tmp_tnt;

		ghast = tmp_ghast;

		fire = tmp_fire;

		magical = tmp_magical;

		replace_tnt = tmp_replace_tnt;

		restrict_blocks = restrictBlocks;
		
		replaceAbove = tmp_replaceAbove;
		
		replaceLimit = tmp_replaceLimit;
		
		block_list = new ArrayList<BlockId>(blockList);

		name = world_name;
		
		enderman = tmp_enderman;
		
		this.dragons = dragons;

	}

	public ArrayList<Object> getConfig() {

		String blocklist = "";
		for(BlockId block : block_list)
			blocklist += block.toString() + ", ";
		
		blocklist = blocklist.substring(0, blocklist.length() - 2);

		ArrayList<Object> list = new ArrayList<Object>();
		list.add(creepers);
		list.add(tnt);
		list.add(ghast);
		list.add(magical);
		list.add(fire);
		list.add(restrict_blocks);
		list.add(blocklist);
		list.add(replace_tnt);
		list.add(replaceAbove);
		list.add(replaceLimit);
		list.add(enderman);
		list.add(dragons);
		
		return list;
		
	}

	public String getName() {
		return name;
	}

}