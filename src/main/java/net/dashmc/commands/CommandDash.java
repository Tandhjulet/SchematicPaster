package net.dashmc.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.dashmc.DashMC;
import net.dashmc.map.MapManager;

public class CommandDash implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.isOp()) {
			sender.sendMessage("Not permitted.");
			return false;
		}

		int mapNum = new Integer(args[0]);
		MapManager.get().pasteMap(mapNum);

		return true;
	}

	public static void register() {
		DashMC.getPlugin().getCommand("dash").setExecutor(new CommandDash());
	}
}
