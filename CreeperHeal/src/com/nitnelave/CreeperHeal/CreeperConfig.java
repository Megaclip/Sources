package com.nitnelave.CreeperHeal;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Logger;

import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;



public class CreeperConfig
{
	
	private final static String[] world_config_nodes = {"Creepers", "TNT", "Ghast", "Magical", "Fire", "restrict-blocks", "restrict-list", "replace-all-tnt", "replace-above-limit-only", "replace-limit", "block-enderman-pickup", "dragons"}; //list of properties for the world config
	protected final Logger log = Logger.getLogger("Minecraft");            //to output messages to the console/log

	
	/**
	 * Config settings
	 */

	protected int interval = 60;                                                    //interval defined in the config, with the default value in milisec
	protected int log_level = 1;                                                            //level of message output of the config, with default value
	protected boolean drop_blocks_replaced = true;        //drop items when blocks are overwritten
	protected int block_interval = 20;        //frequency for replacing blocks, in the case of block_per_block
	protected boolean block_per_block = true;    //as in the config
	protected boolean teleport_on_suffocate = true;    //teleport player in stuck in an explosion getting replaced
	protected int burn_interval = 45;            //interval after which burnt blocks are replaced. default value
	protected boolean drop_not_replaced = true;		//drop destroyed blocks
	protected int drop_chance = 100;					//chance that these blocks drop
	protected boolean opEnforce = true;		//setting to treat ops as admins
	protected boolean cracked = false;		//replace bricks by cracked ones
	protected boolean lockette = false;		//use lockette (lwc has priority)
	protected boolean replaceChests = false;	//immediately replace all chests
	protected boolean replaceProtected = false;	//immediately replace protected chests
	protected String chestProtection = "no";		//no, lwc or lockette
	protected boolean overwrite_blocks;			//which block has the priority in case of a conflict : new or old?
	protected boolean preventBlockFall;			//prevent gravel from dropping if near an explosion
	public int distanceNear = 20;				//range of action of the healnear command
	private CreeperHeal plugin;
	private FileConfiguration configFile;
	protected Map<String, WorldConfig> world_config = Collections.synchronizedMap(new HashMap<String, WorldConfig>());		//config for each world

	private File yml;

	public CreeperConfig(CreeperHeal instance)
    {
		plugin = instance;
		yml =  new File(getDataFolder()+"/config.yml");
		configFile = plugin.getConfig();
		block_interval = getInt("block-per-block-interval", 20);
		
		if (!new File(getDataFolder().toString()).exists() ) {		//create the /CreeperHeal folder
			new File(getDataFolder().toString()).mkdir();
		}

		//File yml = new File(getDataFolder()+"/config.yml");

		if (!yml.exists()) {
			log.warning("[CreeperHeal] Config file not found, creating default.");
			write();        //write the config with the default values.
		}

		File trapFile = new File(getDataFolder() + "/trap.yml");		//get the trap file

		if(!trapFile.exists()) {
			try {
				trapFile.createNewFile();
			}
			catch (IOException ex) {
				log.warning("[CreeperHeal] Cannot create file "+trapFile.getPath());
			}
		}

		plugin.trap_location = loadTraps(trapFile);		//get the traps from the file

		
		load();
		write();

    }

	public void load(){            //reads the config
		log_info("Loading config",1);
		try
        {
	        configFile.load(new File(getDataFolder()+"/config.yml"));
        }
        catch (FileNotFoundException e1)
        {
	        e1.printStackTrace();
        }
        catch (IOException e1)
        {
	        e1.printStackTrace();
        }
        catch (InvalidConfigurationException e1)
        {
	        e1.printStackTrace();
        }

		interval = getInt("wait-before-heal-explosions", 60);        //tries to read the value directly from the config
		log_level = getInt("verbose-level", 1);

		drop_blocks_replaced = getBoolean("drop-overwritten-blocks", true);


		String tmp_str;

		try{
			tmp_str = configFile.getString("replacement-method", "block-per-block").trim();
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Wrong value for replacement method field. Defaulting to block-per-block.");
			log_info(e.getLocalizedMessage(), 1);
			tmp_str = "block-per-block";
		}
		if(!tmp_str.equalsIgnoreCase("all-at-once") && !tmp_str.equalsIgnoreCase("block-per-block"))
			log.warning("[CreeperHeal] Wrong value for replacement method field. Defaulting to block-per-block.");
		block_per_block = (tmp_str.equalsIgnoreCase("all-at-once"))?false:true;

		teleport_on_suffocate = getBoolean("teleport-when-buried", true);

		burn_interval = getInt("wait-before-heal-fire", 45);

		drop_not_replaced = getBoolean("drop-destroyed-blocks", true);

		drop_chance = getInt("drop-destroyed-blocks-chance", 100);

		opEnforce = getBoolean("op-have-all-permissions", true);

		cracked = getBoolean("crack-destroyed-bricks", false);

		overwrite_blocks = getBoolean("overwrite-blocks", true);
		
		preventBlockFall = getBoolean("prevent-block-fall", true);
		
		distanceNear = getInt("distance-near", 20);

		try{
			tmp_str = configFile.getString("chest-protection", "no").trim().toLowerCase();
		}
		catch (Exception e) {
			log.warning("[CreeperHeal] Wrong value for chest protection field. Defaulting to no.");
			log_info(e.getLocalizedMessage(), 1);
			tmp_str = "no";
		}

		if(!tmp_str.equalsIgnoreCase("no") && !tmp_str.equalsIgnoreCase("lwc") && !tmp_str.equalsIgnoreCase("all") && !tmp_str.equalsIgnoreCase("lockette"))
			log.warning("[CreeperHeal] Wrong value for chest protection field. Defaulting to no.");
		else {
			replaceChests = replaceProtected = false;

			if(tmp_str.equals("all"))
				replaceChests = true;
			else if(tmp_str.equals("lwc") || tmp_str.equals("lockette"))
				replaceProtected = true;
		}
		chestProtection = tmp_str;



		world_config.clear();
		for(World w : plugin.getServer().getWorlds()) {
			String name = w.getName();
			loadWorld(name);
		}


	}

	public boolean getBoolean(String path, boolean def) {        //read a boolean from the config
		boolean tmp;
		try {
			tmp = configFile.getBoolean(path, def);
		}
		catch(Exception e) {
			log.warning("[CreeperHeal] Wrong value for " + path + " field. Defaulting to " + Boolean.toString(def));
			tmp = def;
		}
		return tmp;
	}

	public int getInt(String path, int def) {
		int tmp;
		try {
			tmp = configFile.getInt(path, def);
		}
		catch(Exception e) {
			log.warning("[CreeperHeal] Wrong value for " + path + " field. Defaulting to " + Integer.toString(def));
			tmp = def;
		}
		return tmp;
	}



	




	public void write(){            //write the config to a file, with the values used, or the default ones
		log_info("Writing config...", 2);
		File yml = new File(getDataFolder()+"/config.yml");

		if(!yml.exists()){
			new File(getDataFolder().toString()).mkdir();
			try {
				yml.createNewFile();
			}
			catch (IOException ex) {
				log.warning("[CreeperHeal] Cannot create file "+yml.getPath());
			}
		}


		configFile.set("wait-before-heal-explosions", (int) interval);
		configFile.set("replacement-method", block_per_block ? "block-per-block" : "all-at-once");
		configFile.set("block-per-block-interval", block_interval);
		configFile.set("wait-before-heal-fire", burn_interval);
		configFile.set("drop-overwritten-blocks", drop_blocks_replaced);
		configFile.set("drop-destroyed-blocks", drop_not_replaced);
		configFile.set("drop-destroyed-blocks-chance", drop_chance);
		configFile.set("teleport-when-buried", teleport_on_suffocate);
		configFile.set("verbose-level", log_level);
		configFile.set("op-have-all-permissions", opEnforce);
		configFile.set("crack-destroyed-bricks", cracked);
		configFile.set("chest-protection", chestProtection );
		configFile.set("overwrite-blocks", overwrite_blocks);
		configFile.set("prevent-block-fall", preventBlockFall);
		configFile.set("distance-near", distanceNear);



		for(WorldConfig w : world_config.values()) {
			String name = w.getName();

			int k = 0;

			ArrayList<Object> node_list = w.getConfig();

			for(String property : world_config_nodes)
				configFile.set( name + "." + property, node_list.get(k++));
		}

		try
        {
	        configFile.save(yml);
        }
        catch (IOException e)
        {
	        e.printStackTrace();
        }

	}
	
	public WorldConfig loadWorld(World world) {

		return loadWorld(world.getName());
	}


	public WorldConfig loadWorld(String name) {      //loads the world (for example, the first we need it)

		WorldConfig returnValue = world_config.get(name);   

		if(returnValue == null){
			log_info("Loading world: "+name, 1);
			boolean creeper = getBoolean(name + ".Creepers", true);
			boolean tnt = getBoolean(name + ".TNT", true);
			boolean fire = getBoolean(name + ".Fire", true);

			boolean ghast = getBoolean(name + ".Ghast", true);

			boolean magical = getBoolean(name + ".Magical", false );

			boolean replace_tnt = getBoolean(name + ".replace-all-tnt", false);

			boolean replaceAbove = getBoolean(name + ".replace-above-limit-only", false);

			int replaceLimit = getInt(name + ".replace-limit", 64);

			boolean enderman = getBoolean(name + ".block-enderman-pickup", false);
			
			boolean dragons = getBoolean(name + ".dragons", false);

			String restrict_blocks;

			try{

				restrict_blocks = configFile.getString(name + ".restrict-blocks", "false").trim();

			}

			catch (Exception e) {

				log.warning("[CreeperHeal] Wrong value for " + name + ".restrict-blocks field. Defaulting to false.");

				log_info(e.getLocalizedMessage(), 1);

				restrict_blocks = "false";

			}        //if not a valid value

			if(!restrict_blocks.equalsIgnoreCase("false") && !restrict_blocks.equalsIgnoreCase("whitelist") && !restrict_blocks.equalsIgnoreCase("blacklist")) {

				log.warning("[CreeperHeal] Wrong value for " + name + ".restrict-blocks field. Defaulting to false.");

				restrict_blocks = "false";

			}

			ArrayList<BlockId> restrict_list  = new ArrayList<BlockId>();

			try{

				String tmp_str1 = configFile.getString(name + ".restrict-list", "").trim();

				String[] split = tmp_str1.split(",");

				if(split!=null){        //split the list into single strings of integer

					for(String elem : split) {

						restrict_list.add(new BlockId(elem));

					}

				}

				else

					log_info("[CreeperHeal] Empty restrict-list for world " + name, 1);

			}

			catch (Exception e) {

				log.warning("[CreeperHeal] Wrong values for restrict-list field for world " + name);

				restrict_list.clear();

				restrict_list.add(new BlockId(0));

			}

			returnValue = new WorldConfig(name, creeper, tnt, ghast, fire, magical, replace_tnt, restrict_blocks, restrict_list, replaceAbove, replaceLimit, enderman, dragons);

			world_config.put(name, returnValue);
			return returnValue;
		}

		return returnValue;
	}

	public Map<String, String> loadTraps(File file) {     //reads the traps from the file
		Map<String, String> trap_location = Collections.synchronizedMap(new HashMap<String, String>());;
		Scanner scanner = null;
		int count = 0;
		try {
			scanner = new Scanner(new FileReader(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine();
			String[] args = line.split(":");
			if(!(args.length == 2))
				continue;
			trap_location.put(args[0], args[1]);
			count++;
		}
		log_info("[CreeperHeal] Loaded " + count + " traps",1);
		return trap_location;
	}

	public void saveTraps(Map<String, String> trap_location) {      //write the list to the file
		File trapFile = new File(getDataFolder() + "/trap.yml");
		BufferedWriter out;

		trapFile.delete();

		try {
			trapFile.createNewFile();
			out = new BufferedWriter(new FileWriter(trapFile));
			for(String loc : trap_location.keySet()) {
				out.write(loc + ":" + trap_location.get(loc));
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}
	
	private File getDataFolder()
	{
		return plugin.getDataFolder();
	}
	
	public void log_info(String msg, int level)
	{
		if(level<=log_level)
			log.info("[CreeperHeal] "+msg);
	}
	
	
	
}
