package com.nitnelave.CreeperHeal;

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
import org.bukkit.block.ContainerBlock;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.NoteBlock;
import org.bukkit.block.Sign;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Attachable;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
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
	private final static ArrayList<Integer> empty_blocks = new ArrayList<Integer>(Arrays.asList(0,8,9,10,11));
	protected static HashSet<Byte> transparent_blocks = null;			//blocks that you can aim through while creating a trap.
	private static final Map<Integer,Integer> blockDrops = new HashMap<Integer,Integer>();	//map to get the drop of a block

	/**
	 * Static constructor.
	 */
	static {
		int[] ids = {1,2,3,4,5,6,12,13,14,15,16,17,19,21,22,23,24,25,26,27,28,29,30,31,32,33,35,37,38,39,40,41,42,43,44,45,46,47,48,49,50,
				53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,80,81,82,83,84,85,86,87,88,89,91,93,94,
				95,96,97,98,99,100,101,103,104,105,107,108,109,110,111,112,113,114,115,116,117,118,121};
		int[] drops = {4,3,3,4,5,6,12,13,14,15,263,17,19,351,22,23,24,25,355,27,28,29,30,31,32,33,35,37,38,39,40,41,42,43,44,45,46,47,48,49,
				50,53,54,331,264,57,58,295,3,61,61,323,324,65,66,67,323,69,70,330,72,331,331,76,76,77,332,80,81,82,338,84,85,86,87,88,348,
				91,356,356,95,96,4,98,39,40,101,103,361,362,107,108,109,3,111,112,113,114,115,116,117,118,4};
		for(int i = 0; i< ids.length; i++)
			blockDrops.put(ids[i], drops[i]);

		Byte[] elements = {0, 6, 8, 9, 10, 11, 18, 20, 26, 27, 28, 30, 31, 32, 37, 38, 39, 40, 44, 50, 51, 55, 59, 63, 65, 66, 68, 69, 70, 72, 75, 76, 77, 78, 83, 93, 94, 96, 101, 102, 104, 105, 106, 111, 115, 117};
		transparent_blocks = new HashSet<Byte>(Arrays.asList(elements));
	}


	/**
	 * Listeners
	 */

	protected CreeperListener listener = new CreeperListener(this);                        //listener for explosions
	private FireListener fire_listener = new FireListener(this);                        //listener for fire
	private TNTBreakListener block_listener = new TNTBreakListener(this);				//catches the block break to prevent trap destruction
	private EnderListener ender_listener = new EnderListener(this);						//catches the enderman pickup event to cancel it


	/**
	 * HashMaps
	 */

	protected Map<Date, List<BlockState>> map = Collections.synchronizedMap(new HashMap<Date, List<BlockState>>());        //hashmap storing the list of blocks destroyed in an explosion
	private Map<Date, BlockState> map_burn = Collections.synchronizedMap(new HashMap<Date, BlockState>());                //same for burnt blocks
	private Map<Location, ItemStack[]> chest_contents = Collections.synchronizedMap(new HashMap<Location, ItemStack[]>());         //stores the chests contents
	private Map<Location, String[]> sign_text = Collections.synchronizedMap(new HashMap<Location, String[]>());                    //stores the signs text
	private Map<Location, Byte> note_block = Collections.synchronizedMap(new HashMap<Location, Byte>());								//stores the note blocks' notes
	private Map<Location, String> mob_spawner = Collections.synchronizedMap(new HashMap<Location, String>());						//stores the mob spawners' type
	private Map<String, String> trap_location = Collections.synchronizedMap(new HashMap<String, String>());					//list of all the trap blocks
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




	public void onEnable() {



		config = new CreeperConfig(this);

		commandExecutor = new CreeperCommandManager(this);

		getCommand("CreeperHeal").setExecutor(commandExecutor);

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


		/*
		 * Registering events
		 */



		pm.registerEvent(Event.Type.BLOCK_BREAK, block_listener, Event.Priority.Normal, this);

		pm.registerEvent(Event.Type.ENTITY_EXPLODE, listener, Event.Priority.Monitor, this);

		pm.registerEvent(Event.Type.BLOCK_BURN, fire_listener, Event.Priority.Monitor, this);

		pm.registerEvent(Event.Type.ENDERMAN_PICKUP, ender_listener, Event.Priority.High, this);

		pm.registerEvent(Event.Type.ENTITY_DAMAGE, listener, Event.Priority.High, this);

		pm.registerEvent(Event.Type.BLOCK_PHYSICS, block_listener, Event.Priority.Normal, this);

		pm.registerEvent(Event.Type.LEAVES_DECAY, block_listener, Event.Priority.Normal, this);

		log.info("[CreeperHeal] version "+pdfFile.getVersion()+" by nitnelave is enabled");
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
		while(iter.hasNext())
		{
			try{
				Date date = iter.next();
				if(date.before(now))
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









	public void recordBlocks(EntityExplodeEvent event, WorldConfig world) {        //record the list of blocks of an explosion, from bottom to top
		Date now = new Date();
		while(map.containsKey(now))
			now = new Date(now.getTime() + 1);
		event.setYield(0);
		List<Block> list = event.blockList();            //the list declared by the explosion
		List<BlockState> list_state = new ArrayList<BlockState>();        //the list of blockstate we'll be keeping afterward

		explosionList.put(event.getLocation(), new Date(now.getTime() + 1000*config.interval + 50*list.size()*config.block_interval));

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
				getServer().getScheduler().scheduleSyncDelayedTask(this, new AddTrapRunnable(now, block,this));
		}

		for(Block block : list)     //cycle through the blocks declared destroyed
		{
			int type_id = block.getTypeId();
			byte data = block.getData();
			if((world.restrict_blocks.equalsIgnoreCase("whitelist") && world.block_list.contains(new BlockId(type_id, data))
					|| (world.restrict_blocks.equalsIgnoreCase("blacklist") && !world.block_list.contains(new BlockId(type_id, data))
							|| world.restrict_blocks.equalsIgnoreCase("false"))))       
				//if the block is to be replaced
			{

				if(config.replaceProtected && isProtected(block))
					toReplace.put(block.getLocation(),block.getState());    //replace immediately

				if(block.getState() instanceof ContainerBlock) 
				{        //save the inventory
					chest_contents.put(block.getLocation(), ((ContainerBlock) block.getState()).getInventory().getContents().clone());
					((ContainerBlock) block.getState()).getInventory().clear();
					if(config.replaceChests)
						toReplace.put(block.getLocation(),block.getState());
				}
				else if(block.getState() instanceof Sign)                //save the text
					sign_text.put(block.getLocation(), ((Sign)block.getState()).getLines());

				else if(block.getState() instanceof NoteBlock) 
					note_block.put(block.getLocation(), ((NoteBlock)(block.getState())).getRawNote());

				else if(block.getState() instanceof CreatureSpawner) 
					mob_spawner.put(block.getLocation(), ((CreatureSpawner)(block.getState())).getCreatureTypeId());


				switch (block.getType()) 
				{       
					case IRON_DOOR :                //in case of a door or bed, only store one block to avoid dupes
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
							getServer().getScheduler().scheduleAsyncDelayedTask(this, new AddTrapRunnable(now, block,this));
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




		map.put(now, list_state);        //store in the global hashmap, with the time it happened as a key

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
		paint.e = array[painting.getArt().getId()];
		paint.b(paint.a);
		if (!(paint).j()) {
			paint = null;
			w.dropItemNaturally(loc, new ItemStack(321, 1));
			return;
		}
		w.getHandle().addEntity(paint);

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



	public void force_replace(long since, WorldConfig world) {        //force replacement of all the explosions since x seconds
		Date now = new Date();

		Iterator<Date> iterator = map.keySet().iterator();
		while(iterator.hasNext()) {
			Date time = iterator.next();
			if(new Date(time.getTime() + since).after(now) || since == 0) {        //if the explosion happened since x seconds
				if(map.get(time).get(0).getWorld().getName().equals( world.getName())) {
					List<BlockState> list = map.get(time);
					if(!list.isEmpty())
					{
						replace_blocks(map.get(time));
						log_info("Blocks replaced!", 2);
						replacePaintings(time);
					}
					iterator.remove();

				}
			}
		}
		if(since == 0)
			force_replace_burnt(0L, world);
	}


	private void replace_blocks(List<BlockState> list) {    //replace all the blocks in the given list
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

		if(!config.overwrite_blocks && block_id != 0) {        //drop an item on the spot
			if(config.drop_blocks_replaced)
				dropBlock(blockState);
			return;
		}
		else if(config.overwrite_blocks && block_id != 0 && config.drop_blocks_replaced)
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
				getServer().getScheduler().scheduleAsyncDelayedTask(this, new ReorientRails(blockState));//enforce the rails' direction, as it sometimes get messed up by the other rails around
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
				blockState.update(true);
		}

		CreeperUtils.checkForAscendingRails(blockState, preventUpdate);
		if(block.getState() instanceof ContainerBlock) {            //if it's a chest, put the inventory back
			((ContainerBlock) block.getState()).getInventory().setContents( chest_contents.get(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ())));
			chest_contents.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));
		}
		else if(block.getState() instanceof Sign) {                    //if it's a sign... no I'll let you guess
			int k = 0;

			for(String line : sign_text.get(block.getLocation())) {
				((Sign) block.getState()).setLine(k++, line);
			}
			sign_text.remove(new Location(block.getWorld(), block.getX(), block.getY(), block.getZ()));

		}
		else if(block.getState() instanceof NoteBlock) {
			((NoteBlock)block.getState()).setRawNote( note_block.get(block.getLocation()));
			note_block.remove(block.getLocation());
		}
		else if(block.getState() instanceof CreatureSpawner) {
			((CreatureSpawner)block.getState()).setCreatureTypeId( mob_spawner.get(block.getLocation()));
			mob_spawner.remove(block.getLocation());
		}

	}








	private void delay_replacement(BlockState blockState)	//the block is dependent on a block that is just air. Schedule it for a later replacement
	{
		delay_replacement(blockState, 0);
	}

	protected void delay_replacement(BlockState blockState, int count)
	{
		getServer().getScheduler().scheduleAsyncDelayedTask(this, new DelayReplacement(this, blockState, count), config.block_interval);
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

	private void dropBlock(BlockState blockState) {         //drops the resource associated with the given blockState exploded
		int type_id = blockState.getTypeId();
		byte data = blockState.getRawData();
		Location loc = blockState.getBlock().getLocation();
		World w = loc.getWorld();
		if(blockDrops.containsKey(type_id)){
			int type_drop = blockDrops.get(type_id);
			int number_drops = 1;
			Random generator = new Random();
			if(type_id == 21)
				number_drops = generator.nextInt(5) + 4;
			else if(type_drop == 331)
				number_drops = generator.nextInt(2) + 4;
			else if(type_drop == 337)
				number_drops = 4;
			else if(type_drop == 348)
				number_drops = generator.nextInt(3) + 2;
			else if(type_id == 99 || type_id == 100)
				number_drops = generator.nextInt(3);


			w.dropItemNaturally(loc, new ItemStack(type_drop, number_drops, data));
		}
		if(blockState instanceof ContainerBlock)        //in case of a chest, drop the contents on the ground as well
		{
			ItemStack[] stacks = chest_contents.get(loc);
			if(chest_contents!=null)
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
		return toReplace.get(location) != null;
	}


	public void checkForPaintings(EntityDamageByEntityEvent event)
	{
		Entity en = event.getEntity();

		if(en instanceof Painting)
		{
			if(event.getDamager() instanceof TNTPrimed)
			{
				log_info("painting!",3);

				paintings.put((Painting)en, new Date());
				WorldServer w = ((CraftWorld)en.getWorld()).getHandle();
				w.getEntity(en.getEntityId()).dead = true;
			}
		}

	}


	public WorldConfig loadWorld(World w)
	{
		return config.loadWorld(w);
	}


}