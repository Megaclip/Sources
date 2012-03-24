package com.nitnelave.CreeperHeal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import net.minecraft.server.EntityPainting;
import net.minecraft.server.EnumArt;
import net.minecraft.server.WorldServer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandMap;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
//import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.yi.acru.bukkit.Lockette.Lockette;

import com.garbagemule.MobArena.MobArenaHandler;
import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;



public class CreeperHeal extends JavaPlugin {
	/**
	 * Constants
	 */


	protected final static ArrayList<Integer> blocks_physics = new ArrayList<Integer>(Arrays.asList(12,13,88));                        //sand gravel, soulsand fall
	protected final static ArrayList<Integer> blocks_last = new ArrayList<Integer>(Arrays.asList(6,18,26,27,28,31,32,37,38,39,40,50,55,59,63,64,65,66,68,69,70,71,72,75,76,77,81,83,93,94,96,104,105,106,115));  //blocks dependent on others. to put in last
	private final static ArrayList<Integer> blocks_dependent_down = new ArrayList<Integer>(Arrays.asList(6,26,27,28,31,32,37,38,39,40,55,59,63,64,66,70,71,72,78,93,94,104,105,115));
	protected final static ArrayList<Integer> blocks_non_solid = new ArrayList<Integer>(Arrays.asList(0,6,8,9,26,27,28,30,31,37,38,39,40, 50,55,59,63,64,65,66,68,69,70,71,72,75,76,77,78,83,90,93,94,96));   //the player can breathe
	private final static ArrayList<Integer> empty_blocks = new ArrayList<Integer>(Arrays.asList(0,8,9,10,11, 51, 78));
	protected static HashSet<Byte> transparent_blocks = null;			//blocks that you can aim through while creating a trap.

	/**
	 * Static constructor.
	 */
	static {
		Byte[] elements = {0, 6, 8, 9, 10, 11, 18, 20, 26, 27, 28, 30, 31, 32, 37, 38, 39, 40, 44, 50, 51, 55, 59, 63, 65, 66, 68, 69, 70, 72, 75, 76, 77, 78, 83, 93, 94, 96, 101, 102, 104, 105, 106, 111, 115, 117};
		transparent_blocks = new HashSet<Byte>(Arrays.asList(elements));
	}


	/**
	 * Listeners
	 */

	protected CreeperListener listener = new CreeperListener(this);                        //listener for explosions
	private FancyListener fancyListener = new FancyListener(this);

	/**
	 * HashMaps
	 */

	protected Map<Date, List<BlockState>> map = Collections.synchronizedMap(new HashMap<Date, List<BlockState>>());        //hashmap storing the list of blocks destroyed in an explosion
	private Map<Date, BlockState> map_burn = Collections.synchronizedMap(new HashMap<Date, BlockState>());                //same for burnt blocks
	private Map<Location, ItemStack[]> chest_contents = Collections.synchronizedMap(new HashMap<Location, ItemStack[]>());         //stores the chests contents
	private Map<Location, String[]> sign_text = Collections.synchronizedMap(new HashMap<Location, String[]>());                    //stores the signs text
	private Map<Location, Byte> note_block = Collections.synchronizedMap(new HashMap<Location, Byte>());								//stores the note blocks' notes
	private Map<Location, String> mob_spawner = Collections.synchronizedMap(new HashMap<Location, String>());						//stores the mob spawners' type
	protected Map<String, String> trap_location;					//list of all the trap blocks
	private Map<Painting, Date> paintings = Collections.synchronizedMap(new HashMap<Painting, Date>());					//paintings to be replaced
	private Map<Location, BlockState> toReplace = Collections.synchronizedMap(new HashMap<Location,BlockState>());		//blocks to be replaced immediately after an explosion
	protected Map<BlockState, Date> preventUpdate = Collections.synchronizedMap(new HashMap<BlockState, Date>());
	protected Map<Location, Date> explosionList = Collections.synchronizedMap(new HashMap<Location, Date>());
	protected Map<Location, Date> fireList = Collections.synchronizedMap(new HashMap<Location, Date>());
	protected Map<Location, Date> preventBlockFall = Collections.synchronizedMap(new HashMap<Location, Date>());



	/**
	 * Handlers for misc. plugins
	 */

	private MobArenaHandler maHandler = null;		//handler to detect mob arenas
	private LWC lwc = null;			//handler for LWC protection




	protected final Logger log = Logger.getLogger("Minecraft");            //to output messages to the console/log
	protected CreeperConfig config;
	protected CreeperCommandManager commandExecutor;
	private CreeperDrop creeperDrop;





	public void onEnable() {

		config = new CreeperConfig(this);

		commandExecutor = new CreeperCommandManager(this);
		CommandMap commandMap = null;
		try{
			Field field = SimplePluginManager.class.getDeclaredField("commandMap");
			field.setAccessible(true);
			commandMap = (CommandMap)(field.get(getServer().getPluginManager()));
		}catch(NoSuchFieldException e){
			e.printStackTrace();
		}
		catch(IllegalAccessException e){
			e.printStackTrace();
		}

		String[] aliases = {"CreeperHeal",config.alias};
		CreeperCommand com = new CreeperCommand(aliases, "", "", commandExecutor);

		commandMap.register("_", com);

		creeperDrop = new CreeperDrop(this);



		/*
		 * Recurrent tasks
		 */

		int tmp_period = 20;        //register the task to go every "period" second if all at once
		if(config.block_per_block)                    //or every "block_interval" ticks if block_per_block
			tmp_period = config.block_interval;
		if( getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				check_replace(config.block_per_block);        //check to replace explosions/blocks
			}}, 200, tmp_period) == -1)
			log.warning("[CreeperHeal] Impossible to schedule the re-filling task. Auto-refill will not work");

		if( getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				replace_burnt();
			}}, 200, config.block_interval) == -1)
			log.warning("[CreeperHeal] Impossible to schedule the replace-burnt task. Burnt blocks replacement will not work");

		if( getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			public void run() {
				cleanMaps();
			}}, 200, 200) == -1)
			log.warning("[CreeperHeal] Impossible to schedule the map-cleaning task. Map cleaning will not work");

		/*
		 * Connection with the other plugins
		 */

		PluginManager pm = getServer().getPluginManager(); 



		Plugin lwcPlugin = pm.getPlugin("LWC");
		if(lwcPlugin != null) {
			lwc = ((LWCPlugin) lwcPlugin).getLWC();
			log_info("Successfully hooked in LWC",0);
		}

		Plugin lockettePlugin = pm.getPlugin("Lockette");
		if(lockettePlugin!=null){
			config.lockette  = true;
			log_info("Successfully detected Lockette",0);
		}

		PluginDescriptionFile pdfFile = this.getDescription();

		Plugin mobArena = pm.getPlugin("MobArena");
		if(mobArena != null) {
			maHandler = new MobArenaHandler();
			log_info("Successfully hooked in MobArena",0);
		}


		pm.registerEvents(listener, this);

		if(!(config.lightweight))
			pm.registerEvents(fancyListener, this);



		log.info("[CreeperHeal] version "+pdfFile.getVersion()+" by nitnelave is enabled");
	}




	public void scheduleTimeRepairs()
	{
		if(getServer().getScheduler().scheduleAsyncRepeatingTask(this, new Runnable() {
			public void run() {
				checkReplaceTime();
			}}, 200, 1200) == -1)

			log.warning("[CreeperHeal] Impossible to schedule the time-repair task. Time repairs will not work");

	}






	protected void cleanMaps()
	{
		Date now = new Date();
		Date delay = new Date(now.getTime() - 7500*config.block_interval);
		Iterator<Date> iter = preventUpdate.values().iterator();
		while(iter.hasNext())
		{
			try{
				Date date = iter.next();
				if(date.before(delay))
					iter.remove();
			}catch(ConcurrentModificationException e){}
		}
		iter = preventBlockFall.values().iterator();
		while(iter.hasNext())
		{
			try{
				Date date = iter.next();
				if(date.before(delay))
					iter.remove();
			}catch(ConcurrentModificationException e){}
		}
		iter = explosionList.values().iterator();
		delay = new Date(now.getTime() - 1000*config.interval - 10000*config.block_interval);
		while(iter.hasNext())
		{
			try{
				Date date = iter.next();
				if(date.before(delay))
					iter.remove();
			}catch(ConcurrentModificationException e){}
		}
		iter = fireList.values().iterator();
		delay = new Date(now.getTime() - 1000 * config.burn_interval);
		while(iter.hasNext())
		{
			try{
				Date date = iter.next();
				if(date.before(delay))
					iter.remove();
			}catch(ConcurrentModificationException e){}
		}


	}




	public void onDisable() {
		for(WorldConfig w : config.world_config.values()) {
			force_replace(0, w);        //replace blocks still in memory, so they are not lost
			force_replace_burnt(0, w);    //same for burnt_blocks
		}
		config.saveTraps(trap_location);
		log.info("[CreeperHeal] Disabled");
	}









	public void recordBlocks(EntityExplodeEvent event, WorldConfig world, String should) {        //record the list of blocks of an explosion, from bottom to top
		Date now = new Date();
		while(map.containsKey(now))
			now = new Date(now.getTime() + 1);
		event.setYield(0);
		List<Block> list = event.blockList();            //the list declared by the explosion
		List<BlockState> list_state = new ArrayList<BlockState>();        //the list of blockstate we'll be keeping afterward

		explosionList.put(event.getLocation(), now);

		if(maHandler != null) 
		{
			if (maHandler.inRegion(event.getLocation())) 
				return;		//Explosion inside a mob arena
		}

		if(event.getEntity() instanceof TNTPrimed) 
		{            //to replace the tnt that just exploded
			Entity entity = event.getEntity();
			Block block = entity.getLocation().getBlock();

			log_info("explosion at " + block.getX() + ";" + block.getY() + ";" + block.getZ(), 2);
			if(world.replace_tnt || isTrap(block)) 
				getServer().getScheduler().scheduleSyncDelayedTask(this, new AddTrapRunnable(now, block,this, Material.TNT));
		}

		for(Block block : list)     //cycle through the blocks declared destroyed
		{
			int type_id = block.getTypeId();
			if (type_id == 0)
				continue;
			byte data = block.getData();
			if((world.restrict_blocks.equalsIgnoreCase("whitelist") && world.block_list.contains(new BlockId(type_id, data))
					|| (world.restrict_blocks.equalsIgnoreCase("blacklist") && !world.block_list.contains(new BlockId(type_id, data))
							|| world.restrict_blocks.equalsIgnoreCase("false"))))       
				//if the block is to be replaced
			{

				if(config.replaceProtected && isProtected(block))
					toReplace.put(block.getLocation(),block.getState());    //replace immediately


				if(block.getState() instanceof InventoryHolder)         //save the inventory
				{
					Inventory inv = ((InventoryHolder) block.getState()).getInventory();
					CreeperChest d = scanForNeighborChest(block.getState());
					if(d != null)
					{
						Inventory otherInv = d.right?((DoubleChestInventory)inv).getLeftSide():((DoubleChestInventory)inv).getRightSide();
						Inventory mainInv = d.right?((DoubleChestInventory)inv).getRightSide():((DoubleChestInventory)inv).getLeftSide();
						chest_contents.put(d.chest.getLocation(), otherInv.getContents());
						chest_contents.put(block.getLocation(), mainInv.getContents()); 

						if(config.replaceProtected && isProtected(block))
							toReplace.put(d.chest.getLocation(), d.chest.getState());
						if(config.replaceChests)
						{
							toReplace.put(d.chest.getLocation(), d.chest.getState());    //replace immediately
							toReplace.put(block.getLocation(),block.getState());    //replace immediately
						}
						list_state.add(d.chest.getState());
						inv.clear();
						d.chest.setTypeIdAndData(0, (byte)0, false);

					}
					else
					{
						chest_contents.put(block.getLocation(), inv.getContents()); 
						inv.clear();
						if(config.replaceChests)
							toReplace.put(block.getLocation(),block.getState());    //replace immediately
					}
				}
				else if(block.getState() instanceof Sign)                //save the text
					sign_text.put(block.getLocation(), ((Sign)block.getState()).getLines());

				else if(block.getState() instanceof NoteBlock) 
					note_block.put(block.getLocation(), ((NoteBlock)(block.getState())).getRawNote());

				else if(block.getState() instanceof CreatureSpawner) 
					mob_spawner.put(block.getLocation(), ((CreatureSpawner)(block.getState())).getCreatureTypeName());

				switch (block.getType()) 
				{       
					case IRON_DOOR_BLOCK :                //in case of a door or bed, only store one block to avoid dupes
					case WOODEN_DOOR :
						if(block.getData() < 8) 
						{
							list_state.add(block.getState());
							block.setTypeIdAndData(0, (byte)0, false);
							block.getRelative(BlockFace.UP).setTypeIdAndData(0, (byte)0, false);
						}
						break;
					case BED_BLOCK :
						if(data < 8) 
						{
							list_state.add(block.getState());
							BlockFace face;
							if(data == 0)            //facing the right way
								face = BlockFace.WEST;
							else if(data == 1)
								face = BlockFace.NORTH;
							else if(data == 2)
								face = BlockFace.EAST;
							else
								face = BlockFace.SOUTH;
							block.setTypeIdAndData(0, (byte)0, false);
							block.getRelative(face).setTypeIdAndData(0, (byte)0, false);
						}
						break;
					case AIR :                        //don't store air
					case OBSIDIAN :
					case BEDROCK :
						break;
					case FIRE :                        //or fire
					case PISTON_EXTENSION :				//pistons are special, don't store this part
						block.setData((byte) 0);
						break;
					case TNT :      //add the traps triggered to the list of blocks to be replaced
						if(isTrap(block) || loadWorld(block.getWorld()).replace_tnt)
							getServer().getScheduler().scheduleSyncDelayedTask(this, new AddTrapRunnable(now, block,this, Material.TNT));
						break;
					case SMOOTH_BRICK :
					case BRICK_STAIRS :
						if(config.cracked  && block.getData() == (byte)0)
							block.setData((byte) 2);        //crack the bricks if the setting is right
					default :                        //store the rest
						list_state.add(block.getState());
						block.setTypeIdAndData(0, (byte)0, false);
						break;
				}

			}
			else if(config.drop_not_replaced)      //the block should not be replaced, check if it drops
			{
				Random generator = new Random();
				if(generator.nextInt(100) < config.drop_chance)        //percentage
					dropBlock(block.getState());
				block.setTypeIdAndData(0, (byte)0, false);

			}
		}

		/*if(!config.lightweight)
		{
			list_state = detect_dropped_redstone(now, event.getEntity(), list_state);
		}*/



		getServer().getScheduler().scheduleSyncDelayedTask(this,new Runnable(){public void run() {replaceProtected();}});       //immediately replace the blocks marked for immediate replacement




		Iterator<BlockState> iter = list_state.iterator();
		while(iter.hasNext())
		{
			BlockState state = iter.next();
			if(toReplaceContains(state.getBlock().getLocation()))       //remove the dupes already stored in the immediate
				iter.remove();
		}

		BlockState[] tmp_array = list_state.toArray(new BlockState[list_state.size()]);        //sort through an array (bottom to top, dependent blocks in last), then store back in the list
		Arrays.sort(tmp_array, new CreeperComparator());
		list_state.clear();
		for(BlockState block : tmp_array) 
		{
			list_state.add(block);
		}




		if(should.equalsIgnoreCase("true"))
			map.put(now, list_state);        //store in the global hashmap, with the time it happened as a key
		else
			map.put(new Date(now.getTime() + 1200000), list_state);

		log_info("EXPLOSION!", 3);





	}







	public void check_replace(boolean block_per_block) {        //check to see if any block has to be replaced
		Date now = new Date();

		log_info("Replacing blocks...", 3);
		Date[] keyset = map.keySet().toArray(new Date[map.keySet().size()]);
		for(Date time : keyset) {
			if(new Date(time.getTime() + config.interval * 1000).before(now)) {        //if enough time went by
				if(!block_per_block){        //all blocks at once
					replace_blocks(map.get(time));        //replace the blocks
					map.remove(time);                    //remove the explosion from the record
					log_info("Blocks replaced!", 2);
					replacePaintings(time);
				}
				else {            //block per block
					if(!map.get(time).isEmpty())        //still some blocks left to be replaced
						replace_one_block(map.get(time));        //replace one
					if(map.get(time).isEmpty())         //if empty, remove from list
					{
						map.remove(time);
						replacePaintings(time);
					}
					log_info("blocks replaced!", 3);
				}

			}
		}   


	}



	private void replace_one_block(List<BlockState> list) {        //replace one block (block per block)

		replace_blocks(list.get(0));        //blocks are sorted, so get the first
		if(!list.isEmpty())
		{
			check_player_one_block(list.get(0).getBlock().getLocation());
			list.remove(0);
		}


	}

	public void check_player_one_block(Location loc) {      //get the living entities around to save thoses who are suffocating
		if(config.teleport_on_suffocate) {
			Entity[] play_list = loc.getBlock().getChunk().getEntities();
			if(play_list.length!=0) {
				for(Entity en : play_list) {
					if(en instanceof LivingEntity) {
						if(loc.distance(en.getLocation()) < 2)
							CreeperUtils.check_player_suffocate((LivingEntity)en);
					}
				}
			}
		}
	}



	public void force_replace(long since, WorldConfig world)         //force replacement of all the explosions since x seconds
	{
		Date now = new Date();

		Iterator<Date> iterator = map.keySet().iterator();
		while(iterator.hasNext()) 
		{
			Date time = iterator.next();
			if(new Date(time.getTime() + since).after(now) || since == 0)         //if the explosion happened since x seconds
			{
				List<BlockState> list = map.get(time);
				if(!list.isEmpty() && list.get(0).getWorld().getName().equals( world.getName())) 
				{
					replace_blocks(map.get(time));
					log_info("Blocks replaced!", 2);
					replacePaintings(time);
					iterator.remove();
				}
			}
		}
		if(since == 0)
			force_replace_burnt(0L, world);
	}


	private void replace_blocks(List<BlockState> list) {    //replace all the blocks in the given list
		if(list == null)
			return;
		while(!list.isEmpty()){            //replace all non-physics non-dependent blocks
			Iterator<BlockState> iter = list.iterator();
			while (iter.hasNext()){
				BlockState block = iter.next();
				if(!blocks_physics.contains(block.getTypeId())){
					block_state_replace(block);
					iter.remove();
				}
			}
			iter = list.iterator();
			while (iter.hasNext()){        //then all physics
				BlockState block = iter.next();
				if(blocks_physics.contains(block.getTypeId())){
					block_state_replace(block);
					iter.remove();
				}
			}

		}
		if(config.teleport_on_suffocate) {            //checks for players suffocating anywhere
			Player[] player_list = getServer().getOnlinePlayers();
			for(Player player : player_list) {
				CreeperUtils.check_player_suffocate(player);
			}
		}

	}





	private void replace_blocks(BlockState block) {        //if there's just one block, no need to go over all this
		block_state_replace(block);
	}


	public void block_state_replace(BlockState blockState)
	{
		Block block = blockState.getBlock();
		int block_id = block.getTypeId();
		//int tmp_id = 0;

		if(!config.overwrite_blocks && !empty_blocks.contains(block_id)) {        //drop an item on the spot
			if(config.drop_blocks_replaced)
				dropBlock(blockState);
			return;
		}
		else if(config.overwrite_blocks && !empty_blocks.contains(block_id) && config.drop_blocks_replaced)
		{
			dropBlock(block.getState());
		}



		if(blocks_dependent_down.contains(blockState.getTypeId()) && blockState.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)
			delay_replacement(blockState);
		else if(blockState instanceof Attachable && blockState.getBlock().getRelative(((Attachable) blockState).getAttachedFace()).getType() == Material.AIR)
			delay_replacement(blockState);
		else
		{
			if (blockState.getType() == Material.WOODEN_DOOR || blockState.getType() == Material.IRON_DOOR_BLOCK)         //if it's a door, put the bottom then the top (which is unrecorded)
			{
				blockState.update(true);
				block.getRelative(BlockFace.UP).setTypeIdAndData(blockState.getTypeId(), (byte)(blockState.getRawData() + 8), false);
			}
			else if(blockState.getType() == Material.BED_BLOCK) 
			{        //put the head, then the feet
				byte data = blockState.getRawData();
				BlockFace face;
				if(data == 0)            //facing the right way
					face = BlockFace.WEST;
				else if(data == 1)
					face = BlockFace.NORTH;
				else if(data == 2)
					face = BlockFace.EAST;
				else
					face = BlockFace.SOUTH;
				blockState.update(true);
				block.getRelative(face).setTypeIdAndData(blockState.getTypeId(), (byte)(data + 8), false);    //feet
			}
			else if(blockState.getType() == Material.PISTON_MOVING_PIECE) {}
			else if(blockState.getType() == Material.RAILS || blockState.getType() == Material.POWERED_RAIL || blockState.getType() == Material.DETECTOR_RAIL)
				getServer().getScheduler().scheduleSyncDelayedTask(this, new ReorientRails(blockState));//enforce the rails' direction, as it sometimes get messed up by the other rails around
			else if(blocks_physics.contains(blockState.getTypeId()))
			{
				preventBlockFall.put(blockState.getBlock().getLocation(), new Date());
				Block tmp_block = block.getRelative(BlockFace.DOWN);
				if(empty_blocks.contains(tmp_block.getTypeId()))
				{
					BlockState tmpState = tmp_block.getState();
					tmp_block.setTypeId(4, false);
					blockState.update(true);
					getServer().getScheduler().scheduleSyncDelayedTask(this, new ReplaceBlockRunnable(tmpState), 2);
				}
				else
					blockState.update(true);

			}
			else         //rest of it, just normal
			{
				try{
					blockState.update(true);
				}
				catch(NullPointerException e)
				{
					log.info(blockState.getType().toString());
				}
			}
		}

		CreeperUtils.checkForAscendingRails(blockState, preventUpdate);
		if(blockState instanceof InventoryHolder) {            //if it's a chest, put the inventory back
			CreeperChest d = scanForNeighborChest(block.getState());
			if(d != null)
			{
				Inventory i = ((InventoryHolder) block.getState()).getInventory();
				ItemStack[] both;
				if(d.right)
					both = concat(((DoubleChestInventory)i).getLeftSide().getContents() , chest_contents.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ())));
				else
					both = concat(chest_contents.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ())), ((DoubleChestInventory)i).getRightSide().getContents());
				i.setContents(both);
				chest_contents.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
			}
			else
			{
				((InventoryHolder) block.getState()).getInventory().setContents( chest_contents.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ())));
				chest_contents.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
			}
		}
		else if(blockState instanceof Sign) {                    //if it's a sign... no I'll let you guess
			Sign state = (Sign) block.getState();
			int k = 0;

			for(String line : sign_text.get(block.getLocation())) {
				state.setLine(k++, line);
			}
			state.update(true);
			sign_text.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));

		}
		else if(blockState instanceof NoteBlock) {
			((NoteBlock)block.getState()).setRawNote( note_block.get(block.getLocation()));
			note_block.remove(block.getLocation());
		}
		else if(blockState instanceof CreatureSpawner) {
			((CreatureSpawner)block.getState()).setCreatureTypeByName( mob_spawner.get(block.getLocation()));
			mob_spawner.remove(block.getLocation());
		}

	}




	public static <T> T[] concat(T[] first, T[] second) {
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}




	private void delay_replacement(BlockState blockState)	//the block is dependent on a block that is just air. Schedule it for a later replacement
	{
		delay_replacement(blockState, 0);
	}

	protected void delay_replacement(BlockState blockState, int count)
	{
		getServer().getScheduler().scheduleSyncDelayedTask(this, new DelayReplacement(this, blockState, count), config.block_interval);
	}




	public void record_burn(Block block) {            //record a burnt block
		if(block.getType() != Material.TNT) {        //unless it's TNT triggered by fire
			Date now = new Date();
			map_burn.put(now, block.getState());
			fireList.put(block.getLocation(), now);
			BlockState block_up = block.getRelative(BlockFace.UP).getState();
			if(blocks_last.contains(block_up.getTypeId())) {        //the block above is a dependent block, store it, but one interval after
				map_burn.put(new Date(now.getTime() + config.burn_interval*1000), block_up);
				if(block_up instanceof Sign) {                //as a side note, chests don't burn, but signs are dependent
					sign_text.put(new Location(block_up.getWorld(), block_up.getX(), block_up.getY(), block_up.getZ()), ((Sign)block_up).getLines());
				}
				try{
					block_up.getBlock().setTypeIdAndData(0, (byte)0, false);
				}
				catch(IndexOutOfBoundsException e) {
					log_info(e.getLocalizedMessage(), 1);
				}
			}
		}
	}

	public void replace_burnt() {        //checks for burnt blocks to replace, with an override for onDisable()
		Date[] keyset = map_burn.keySet().toArray(new Date[map_burn.keySet().size()]);

		Date now = new Date();
		for(Date time : keyset) {
			if((new Date(time.getTime() + config.burn_interval * 1000).before(now))) {        //if enough time went by
				BlockState block = map_burn.get(time);
				replace_blocks(block);
				map_burn.remove(time);
			}
		}
	}

	public void force_replace_burnt(long since, WorldConfig world_config) {     //replace all of the burnt blocks since "since"
		boolean force = false;
		if(since == 0)
			force = true;
		World world = getServer().getWorld(world_config.getName());

		Date[] keyset = map_burn.keySet().toArray(new Date[map_burn.keySet().size()]);

		Date now = new Date();
		for(Date time : keyset) {
			BlockState block = map_burn.get(time);
			if(block.getWorld() == world && (new Date(time.getTime() + since * 1000).after(now) || force)) {        //if enough time went by

				replace_blocks(block);        //replace the non-dependent block
				map_burn.remove(time);
			}
		}
	}



	public void log_info(String msg, int level) {        //logs a message, according to the log_level
		config.log_info(msg, level);
	}


	public void dropBlock(BlockState blockState)
	{

		Location loc = blockState.getBlock().getLocation();
		World w = loc.getWorld();

		ItemStack drop = creeperDrop.getDrop(blockState);
		if(drop != null)
			w.dropItemNaturally(loc, drop);

		if(blockState instanceof InventoryHolder)        //in case of a chest, drop the contents on the ground as well
		{
			ItemStack[] stacks = chest_contents.get(loc);
			if(stacks!=null)
			{
				for(ItemStack stack : stacks)
				{
					if(stack !=null)
						w.dropItemNaturally(loc, stack);
				}
				chest_contents.remove(loc);
			}

		}
		else if(blockState instanceof Sign)         //for the rest, just delete the reference
			sign_text.remove(loc);

		else if(blockState instanceof NoteBlock) 
			note_block.remove(loc);

		else if(blockState instanceof CreatureSpawner) 
			mob_spawner.remove(loc);
	}



	public boolean isTrap(Location loc) {
		return (getTrapOwner(loc) != null);
	}

	public boolean isTrap(Block block) {
		return (getTrapOwner(block) != null);
	}

	public boolean isTrap(Entity en){
		return (getTrapOwner(en.getLocation().getBlock().getLocation()) != null);     //if the name of the owner of the trap is null, then there is no trap!
	}



	public Location stringToLoc(String str) {       //opposite
		String[] args = str.split(";");
		World w = getServer().getWorld(args[0]);
		int x = Integer.parseInt(args[1]);
		int y = Integer.parseInt(args[2]);
		int z = Integer.parseInt(args[3]);
		return new Location(w, x, y, z);
	}

	public String getTrapOwner(Location loc) {
		return trap_location.get(CreeperUtils.locToString(loc));
	}

	public String getTrapOwner(Block block) {
		return trap_location.get(CreeperUtils.locToString(block));
	}


	public void createTrap(Location loc, String name)
	{
		trap_location.put(CreeperUtils.locToString(loc), name);
	}

	public boolean deleteTrap(Player player)
	{
		return commandExecutor.deleteTrap(player);
	}



	public void deleteTrap(Location loc){
		trap_location.remove(CreeperUtils.locToString(loc));
	}



	private boolean isProtected(Block block){       //is the block protected?
		if(lwc!=null){                      //lwc gets the priority. BECAUSE!
			return (lwc.findProtection(block)!=null);
		}
		else if(config.lockette){                  //and then lockette
			return Lockette.isProtected(block);
		}
		else return false;
	}


	protected void replaceProtected() {         //replace the blocks that should be immediately replaced after an explosion
		Iterator<BlockState> iter = toReplace.values().iterator();
		while(iter.hasNext())
			block_state_replace(iter.next());


		toReplace.clear();

	}


	private boolean toReplaceContains(Location location) {      //check if a block is already included in the list of blocks to be immediately replaced
		return toReplace.containsKey(location);
	}



	private void replacePaintings(Date time)
	{
		Iterator<Painting> iter = paintings.keySet().iterator();
		log_info("replacing paintings",2);
		while(iter.hasNext())
		{

			Painting painting = iter.next();
			Date date = paintings.get(painting);
			if(Math.abs(date.getTime() - time.getTime()) < 500 || map.size() == 0);
			{
				log_info("painting : right time!",3);
				replacePainting(painting);
				iter.remove();
			}
		}
	}

	private void replacePainting(Painting painting) {
		BlockFace face = painting.getAttachedFace().getOppositeFace();
		Location loc = painting.getLocation().getBlock().getRelative(face.getOppositeFace()).getLocation();
		CraftWorld w = (CraftWorld) loc.getWorld();

		loc = CreeperUtils.getAttachingBlock(loc, painting.getArt(), face);

		int dir;
		switch(face) {
			case EAST:
			default:
				dir = 0;
				break;
			case NORTH:
				dir = 1;
				break;
			case WEST:
				dir = 2;
				break;
			case SOUTH:
				dir = 3;;
				break;
		}

		EntityPainting paint = new EntityPainting(w.getHandle(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), dir);
		EnumArt[] array = EnumArt.values();
		paint.art = array[painting.getArt().getId()];
		paint.setDirection(paint.direction);
		if (!(paint).survives()) {
			paint = null;
			w.dropItemNaturally(loc, new ItemStack(321, 1));
			return;
		}
		w.getHandle().addEntity(paint);

	}



	public void checkForPaintings(EntityDamageByEntityEvent event, String should)
	{
		Entity en = event.getEntity();

		if(en instanceof Painting)
		{
			if(event.getDamager() instanceof TNTPrimed)
			{
				log_info("painting!",3);
				Date time = new Date();
				if(should.equalsIgnoreCase("time"))
					time = new Date(time.getTime() + 1200000);

				paintings.put((Painting)en, time);
				WorldServer w = ((CraftWorld)en.getWorld()).getHandle();
				w.getEntity(en.getEntityId()).dead = true;
			}
		}

	}


	private void replace_paintings()
	{
		for(Painting p : paintings.keySet())
		{
			replacePainting(p);
		}
		paintings.clear();
	}






	public WorldConfig loadWorld(World w)
	{
		return config.loadWorld(w);
	}





	public void replaceNear(Player target)
	{
		int k = config.distanceNear;
		Location playerLoc = target.getLocation();

		World w = playerLoc.getWorld();
		Iterator<Location> iter = explosionList.keySet().iterator();
		while (iter.hasNext())
		{
			Location loc = iter.next();
			if(loc.getWorld() == w)
			{
				if(loc.distance(playerLoc) < k)
				{
					Date time = explosionList.get(loc);
					List<BlockState> list = map.get(time);
					replace_blocks(list);
					map.remove(time);
					iter.remove();
				}
			}
		}

	}

	protected void checkReplaceTime()
	{
		for(WorldConfig w : config.world_config.values()) {
			long time = getServer().getWorld(w.name).getTime();
			if(w.repairTime != -1 && ((Math.abs(w.repairTime - time) < 600) || (Math.abs(Math.abs(w.repairTime - time) - 24000)) < 600)){
				force_replace(0, w);        
				force_replace_burnt(0, w);
				replace_paintings();
			}
		}
	}




	public static CreeperChest scanForNeighborChest(World world, int x, int y, int z, short d) //given a chest, scan for double, return the Chest
	{
		Block neighbor;
		if(d <= 3)
		{
			neighbor = world.getBlockAt(x - 1, y, z);
			if (neighbor.getType().equals(Material.CHEST)) {
				return new CreeperChest(neighbor, d == 2 ? false : true);
			}
			neighbor = world.getBlockAt(x + 1, y, z);
			if (neighbor.getType().equals(Material.CHEST)) {
				return new CreeperChest(neighbor, d == 3 ? false : true);
			}
		}
		else
		{
			neighbor = world.getBlockAt(x, y, z - 1);
			if (neighbor.getType().equals(Material.CHEST)) {
				return new CreeperChest(neighbor, d == 5 ? false : true);
			}
			neighbor = world.getBlockAt(x, y, z + 1);
			if (neighbor.getType().equals(Material.CHEST)) {
				return new CreeperChest(neighbor, d == 4 ? false : true);
			}
		}
		return null;
	}

	public static CreeperChest scanForNeighborChest(BlockState block)
	{
		return scanForNeighborChest(block.getWorld(), block.getX(), block.getY(), block.getZ(), block.getRawData());
	}



	/*private List<BlockState> detect_dropped_redstone(Date now,
			Entity entity, List<BlockState> block_list)
			{
		List<Entity> entityList = entity.getNearbyEntities(10, 10, 10);

		for (Entity e : entityList) {
			if(e instanceof Item)
			{
				ItemStack itemStack = ((Item)e).getItemStack();
				if( itemStack.getType() == Material.REDSTONE && itemStack.getAmount() == 1 && e.getTicksLived() < 10)
				{
					Block b = e.getLocation().getBlock();
					while(b.getType() != Material.AIR)
					{
						b = b.getRelative(BlockFace.UP);
					}

					BlockState blockState = b.getState();
					blockState.setType(Material.REDSTONE);
					blockState.setRawData((byte) 0);
					block_list.add(blockState);
					//getServer().getScheduler().scheduleSyncDelayedTask(this, new AddTrapRunnable(now, b,this, Material.REDSTONE));

					e.remove();
				}
			}
		}

		return block_list;

			}*/






}