package com.nitnelave.CreeperHeal;

import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import org.bukkit.Art;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.material.Rails;

public class CreeperUtils
{
	public static void checkForAscendingRails(BlockState blockState, Map<BlockState, Date> preventUpdate)
	{
		BlockFace[] cardinals = {BlockFace.EAST, BlockFace.WEST, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.UP};
		Block block = blockState.getBlock();
		for(BlockFace face : cardinals)
		{
			Block tmp_block = block.getRelative(face);
			if(tmp_block.getState() instanceof Rails)
			{
				byte data = tmp_block.getData();
				if(data>1 && data < 6)
				{
					BlockFace facing = null;
					if(data == 2)
						facing = BlockFace.EAST;
					else if(data == 3)
						facing = BlockFace.WEST;
					else if(data == 4)
						facing = BlockFace.NORTH;
					else if(data == 5)
						facing = BlockFace.SOUTH;
					if(tmp_block.getRelative(facing).getType() == Material.AIR)
						preventUpdate.put(tmp_block.getState(), new Date());
				}
			}
		}
	}

	public static boolean check_free_horizontal(World w, int x, int y, int z, LivingEntity en) {        //checks one up and down, to broaden the scope
		for(int k = -1; k<2; k++){
			if(check_free(w, x, y+k, z, en))
				return true;  //found a spot
		}
		return false;
	}

	public static boolean check_free(World w, int x, int y, int z, LivingEntity en) {
		Block block = w.getBlockAt(x, y, z);
		if(CreeperHeal.blocks_non_solid.contains(block.getTypeId()) && CreeperHeal.blocks_non_solid.contains(block.getRelative(0, 1, 0).getTypeId()) && !CreeperHeal.blocks_non_solid.contains(block.getRelative(0, -1, 0).getTypeId())) {
			Location loc = new Location(w, x, y+0.5, z+0.5);
			loc.setYaw(en.getLocation().getYaw());
			loc.setPitch(en.getLocation().getPitch());
			en.teleport(loc);            //if there's ground under and space to breathe, put the player there
			return true;
		}
		return false;
	}

	public static void check_player_suffocate(LivingEntity en) {
		Location loc = en.getLocation();
		int x =loc.getBlockX();        //get the player's coordinates in ints, to have the block he's standing on
		int y =loc.getBlockY();
		int z =loc.getBlockZ();
		World w = en.getWorld();
		if(!CreeperHeal.blocks_non_solid.contains(loc.getBlock().getTypeId()) || !CreeperHeal.blocks_non_solid.contains(loc.getBlock().getRelative(0, 1, 0).getTypeId())) {
			for(int k =1; k + y < 127; k++) {        //all the way to the sky, checks if there's some room up or around

				if(check_free(w, x, y+k, z, en))
					break;

				if(check_free_horizontal(w, x+k, y, z, en))
					break;

				if(check_free_horizontal(w, x-k, y, z, en))
					break;

				if(check_free_horizontal(w, x, y, z+k, en))
					break;

				if(check_free_horizontal(w, x, y, z-k, en))
					break;

			}

		}

	}

	public static Location getAttachingBlock(Location loc, Art art, BlockFace face)
	{
		if(art.getBlockHeight() + art.getBlockWidth() < 5)
		{
			int i = 0, j = 0, k = art.getBlockWidth() - 1;
			switch(face){
				case EAST:
					break;
				case WEST:
					i = -k;
					break;
				case NORTH:
					j = -k;
					break;
				case SOUTH:
					break;
			}
			loc.add(i, 1-art.getBlockHeight(), j);
		}
		else 
		{ 
			if(art.getBlockHeight() == 4)
				loc.add(0, -1, 0);
			if(art.getBlockWidth() == 4)
			{
				switch(face){
					case EAST:
						break;
					case WEST:
						loc.add(-1, 0, 0);
						break;
					case NORTH:
						loc.add(0, 0, -1);
						break;
					case SOUTH:
						break;
				}
			}
		}

		return loc;
	}




	public static boolean isAbove(Entity entity, int replaceLimit) {       //the entity that exploded was above the limit
		return entity.getLocation().getBlockY()>= replaceLimit;
	}

	public static String locToString(Location loc) {       //location to file-friendly string
		return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
	}

	public static String locToString(Block block) {        
		return block.getWorld().getName() + ";" + block.getX() + ";" + block.getY() + ";" + block.getZ();

	}


	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
	
	public static String shouldReplace(Entity entity, WorldConfig world)
	{

		if(entity != null) {

			if( entity instanceof Creeper)         //if it's a creeper, and creeper explosions are recorded
			{
				if(world.replaceAbove)
				{
					if(isAbove(entity, world.replaceLimit))
						return world.creepers;
					return "false";
				}
				return world.creepers;
			}
			else if(entity instanceof TNTPrimed)                 //tnt -- it checks if it's a trap.
				if(world.replaceAbove){
					if(isAbove(entity, world.replaceLimit))
						return world.tnt;
					return "false";
				}
				else
					return world.tnt;

			else if(entity instanceof Fireball)         //fireballs (shot by ghasts)
				if(world.replaceAbove){
					if(isAbove(entity, world.replaceLimit))
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
