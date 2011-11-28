package com.nitnelave.CreeperHeal;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

public class CreeperCommandManager implements CommandExecutor
{
	private CreeperHeal plugin;
	public PermissionHandler permissions = null;    //permission stuff


	public CreeperCommandManager(CreeperHeal instance)
	{
		plugin = instance;
		setup_permissions();

	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {

		if(args.length != 0) 
		{        //if it's just /ch, display help

			WorldConfig current_world = getConfig().world_config.get(args[args.length - 1]);   //the last argument can be a world

			if(current_world == null) 
			{		//if the last argument was not a world

				if(sender instanceof Player)
					current_world = plugin.loadWorld( ((Player)sender).getWorld());		//get the player's world
				else
				{										//or get the first (normal) world
					current_world = plugin.loadWorld(plugin.getServer().getWorlds().get(0));
					sender.sendMessage("No world specified, defaulting to " + current_world.getName());
				}
			}

			String cmd = args[0];	//command argument


			if(cmd.equalsIgnoreCase("creeper"))
				current_world.creepers = booleanCmd(current_world.creepers, args, "Creepers explosions", sender);

			else if(cmd.equalsIgnoreCase("TNT"))        //same as above
				current_world.tnt = booleanCmd(current_world.tnt, args, "TNT explosions", sender);

			else if(cmd.equalsIgnoreCase("fire"))
				current_world.fire = booleanCmd(current_world.fire, args, "Burnt blocks", sender);

			else if(cmd.equalsIgnoreCase("ghast"))
				current_world.ghast = booleanCmd(current_world.ghast, args, "Ghast fireballs explosions", sender);

			else if(cmd.equalsIgnoreCase("magical"))
				current_world.magical = booleanCmd(current_world.magical, args, "Magical explosions", sender);

			else if(cmd.equalsIgnoreCase("interval"))
				getConfig().interval = integerCmd(getConfig().interval, args, "block destroyed in an explosion", sender);

			else if(cmd.equalsIgnoreCase("burnInterval"))
				getConfig().burn_interval = integerCmd(getConfig().burn_interval, args, "burnt block", sender);

			else if(cmd.equalsIgnoreCase("forceHeal") || cmd.equalsIgnoreCase("heal"))
				forceCmd(args, "explosions", sender, current_world);

			else if(cmd.equalsIgnoreCase("healBurnt"))
				forceCmd(args, "burnt blocks", sender, current_world);

			else if(cmd.equalsIgnoreCase("trap")) {
				if(args.length == 2 && sender instanceof Player) 
				{
					if(args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase("make"))
						createTrap((Player)sender);
					else if(args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase("delete"))
						deleteTrap((Player)sender);
					else
						sender.sendMessage("/ch trap (create|remove)");
				}
				else if(args.length != 2)
					sender.sendMessage("/ch trap (create|remove)");		//misused the command, display the help
				else if(!(sender instanceof Player))
					sender.sendMessage("Player only command");
			}

			else if(cmd.equalsIgnoreCase("reload"))
				getConfig().load();

			else if(cmd.equalsIgnoreCase("help"))
				sendHelp(sender);

			else 
			{        // /ch something gets back to the help
				sender.sendMessage("/ch help");
				return true;
			}

			getConfig().write();		//in case of a change of setting via a command, write it to the file
		}
		else {
			sender.sendMessage("/ch help");
			return true;
		}

		return true;		//always return true as I display my own help
	}


	private void sendHelp(CommandSender sender) {		//displays the help according to the permissions of the player
		sender.sendMessage("CreeperHeal -- Repair explosions damage and make traps");
		sender.sendMessage("--------------------------------------------");
		String green = ChatColor.GREEN.toString();
		String purple = ChatColor.DARK_PURPLE.toString();
		boolean admin = true;
		boolean heal = true;
		boolean trap = true;

		if(sender instanceof Player){
			Player player = (Player) sender;
			admin = checkPermissions("admin", player);
			heal = checkPermissions("heal", player);
			trap = checkPermissions("trap.create", player) || checkPermissions("trap.*", player);
		}

		if(!(admin || heal || trap))
			sender.sendMessage(purple + "You do not have access to any of the CreeperHeal commands");

		if(admin){
			sender.sendMessage(green + "/ch reload :" + purple + " reloads the config from the file.");
			sender.sendMessage(green + "/ch creeper (on|off) (world) :" + purple + " toggles creeper explosion replacement");
			sender.sendMessage(green + "/ch TNT (on|off) (world) :" + purple + " same for TNT");
			sender.sendMessage(green + "/ch Ghast (on|off) (world) :" + purple + " same for Ghast fireballs");
			sender.sendMessage(green + "/ch magical (on|off) :" + purple + " same for \"magical\" explosions.");
			sender.sendMessage(green + "/ch fire (on|off) (world) :" + purple + " same for fire");
			sender.sendMessage(green + "/ch interval [seconds] :" + purple + " Sets the interval before an explosion is replaced to x seconds");
			sender.sendMessage(green + "/ch burnInterval [seconds] :" + purple + " Sets the interval before a block burnt is replaced to x seconds");
		}

		if(heal || admin){
			sender.sendMessage(green + "/ch heal (seconds) (world) :" + purple + " Heals all explosions in the last x seconds, or all if x is not specified.");
			sender.sendMessage(green + "/ch healBurnt (seconds) (world) :" + purple + " Heal all burnt blocks since x seconds, or all if x is not specified.");
		}

		if(trap){
			sender.sendMessage(green + "/ch trap (create|delete) :" + purple + " creates/removes a trap from the tnt block in front of you.");
		}


	}


	private boolean booleanCmd(boolean current, String[] args, String msg, CommandSender sender) 
	{		//changes a setting true/false
		if(sender instanceof Player) 
		{
			if(!checkPermissions("admin", (Player)sender)) {
				sender.sendMessage(ChatColor.RED + "You don’t have the permission");
				return current;
			}
		}
		boolean return_value = false;

		if(args.length == 1)
			return_value = !current;
		else if(args[1].equalsIgnoreCase("on"))
			return_value = true;
		else if(args[1].equalsIgnoreCase("off"))
			return_value = false;
		else {
			sender.sendMessage("/ch " + args[0] + " (on|off)");
			sender.sendMessage("Toggles " + msg + " replacement on/off");
			return current;
		}
		sender.sendMessage(ChatColor.GREEN + msg + " replacement set to : "+Boolean.toString(return_value));
		return return_value;

	}

	private int integerCmd(int current, String[] args, String msg, CommandSender sender) 
	{		//changes a setting with a number
		if(sender instanceof Player) {
			if(!checkPermissions("admin", (Player) sender)) {
				sender.sendMessage(ChatColor.RED + "You don’t have the permission");
				return current;
			}
		}
		if(args.length == 2){
			int interval = 0;
			try {
				interval = Integer.parseInt(args[1]);
			}
			catch (Exception e) {
				sender.sendMessage("/ch " + args[0] + " [seconds]");
				sender.sendMessage("Sets the interval before replacing a " + msg);
				return current;
			}
			sender.sendMessage(ChatColor.GREEN+ "New interval set to : "+interval + "seconds");

			return interval;
		}
		else {
			sender.sendMessage("/ch " + args[0] + " [seconds]");
			sender.sendMessage("Sets the interval before replacing a " + msg);
			return current;
		}
	}

	public void forceCmd(String[] args, String msg, CommandSender sender, WorldConfig current_world) 
	{
		String cmd = args[0];

		if(sender instanceof Player) 
		{
			if(!checkPermissions("heal", (Player)sender) && !checkPermissions("admin", (Player)sender)) 
			{
				sender.sendMessage(ChatColor.RED + "You don’t have the permission");
				return;
			}
		}   

		long since = 0;               
		if(args.length > 1){
			try{
				since = Long.parseLong(args[1]);
			}
			catch (Exception e) {
				sender.sendMessage("/ch " + cmd + " (seconds) (world_name | all)");
				sender.sendMessage("If a time is specified, heals all " + msg + " since x seconds ago. Otherwise, heals all.");
				return;
			}
		}
		boolean burnt = cmd.equalsIgnoreCase("healBurnt");
		if(args.length >2) {
			if(args[2].equalsIgnoreCase("all")) {
				for(WorldConfig w : getConfig().world_config.values()) {
					if(burnt)
						plugin.force_replace_burnt(since, w);
					else
						plugin.force_replace(since, w);
				}
			}
			else {
				if(burnt)
					plugin.force_replace_burnt(since, current_world);
				else
					plugin.force_replace(since, current_world);
			}
		}
		else {
			if(burnt)
				plugin.force_replace_burnt(since, current_world);
			else
				plugin.force_replace(since, current_world);
		}

		sender.sendMessage(ChatColor.GREEN + "Explosions healed");
	}

	public boolean checkPermissions(String node, Player player) {       //check permission for a given node for a given player
		boolean tmp_bool;
		node = "CreeperHeal." + node;
		if (permissions != null) {
			tmp_bool =  permissions.has(player, node) || permissions.has(player, "CreeperHeal.*");

			if(!tmp_bool && getConfig().opEnforce)

				return player.isOp();

			return tmp_bool;
		} else {
			tmp_bool = (player.hasPermission(node) || player.hasPermission("CreeperHeal.*"));
			if(!tmp_bool && getConfig().opEnforce)
				return player.isOp();
			return tmp_bool;
		}
	}

	public void setup_permissions() {        //permissions stuff

		Plugin test = plugin.getServer().getPluginManager().getPlugin("Permissions");

		if(permissions == null) {
			if(test != null) {
				permissions = ((Permissions)test).getHandler();
			}
		}
	}

	private CreeperConfig getConfig()
	{
		return plugin.config;
	}

	private void createTrap(Player p)
	{
		if(checkPermissions("trap.create", p) || checkPermissions("trap.*", p)) {
			Block block = p.getTargetBlock(CreeperHeal.transparent_blocks, 10);
			if(block.getType() == Material.TNT) {
				String owner = plugin.getTrapOwner(block);
				if(owner == null) {
					plugin.createTrap(block.getLocation(), p.getName());
					p.sendMessage(ChatColor.GREEN + "Trap registered");
				}
				else if(owner.equalsIgnoreCase(p.getName()))
					p.sendMessage(ChatColor.RED + "You already registered this trap");
				else    
					p.sendMessage(ChatColor.RED + "Trap belongs to "+ owner);
			}
			else
				p.sendMessage(ChatColor.RED + "You must point to a block of TNT");
		}
		else
			p.sendMessage(ChatColor.RED + "You do not have permission");
	}

	public boolean deleteTrap(Player p)
	{
		boolean delete_own, delete_all = checkPermissions("trap.remove.all", p) || checkPermissions("trap.*", p);
		delete_own = delete_all;
		if(!delete_own)
			delete_own = checkPermissions("trap.remove.own", p);
		if(delete_own) {

			Block block = p.getTargetBlock(CreeperHeal.transparent_blocks, 10);

			if(block.getType() == Material.TNT) {

				String owner = plugin.getTrapOwner(block.getLocation());

				if(owner == null) {

					p.sendMessage(ChatColor.RED + "Target is not a trap");
					return false;
				}

				else if(owner.equalsIgnoreCase(p.getName())){

					plugin.deleteTrap(block.getLocation());

					p.sendMessage(ChatColor.GREEN + "Trap removed");
					return true;

				}

				else {

					if(delete_all) {

						plugin.deleteTrap(block.getLocation());

						p.sendMessage(ChatColor.GREEN + "Trap removed");
						return true;
					}

					else {
						p.sendMessage(ChatColor.RED + "Trap belongs to " + owner + ", you cannot remove it.");
						return false;
					}

				}

			}

			else {
				p.sendMessage(ChatColor.RED + "You must target a TNT block.");
				return false;
			}

		}

		else {
			p.sendMessage(ChatColor.RED + "This TNT block is protected");
			return false;

		}
	}

}