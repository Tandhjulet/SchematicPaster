package dk.tandhjulet.commands;

import java.io.IOException;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import dk.tandhjulet.SchematicPaster;
import dk.tandhjulet.map.MapManager;

public class CommandPaste implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof ConsoleCommandSender) {
			Bukkit.getLogger().info("Unsupported by console.");
			return false;
		} else if (!sender.isOp()) {
			sender.sendMessage("Not permitted.");
			return false;
		} else if (args.length == 0) {
			sender.sendMessage("/paste <map index>");
			return false;
		}

		int mapNum = new Integer(args[0]);
		if (mapNum < 1 || mapNum > MapManager.get().size()) {
			sender.sendMessage("Invalid index");
			return false;
		}
		try {
			MapManager.get().pasteMap(mapNum - 1, ((Player) sender).getLocation());
		} catch (IOException e) {
			e.printStackTrace();
			sender.sendMessage("An error occured. View it in the logs.");
		}

		return true;
	}

	public static void register() {
		SchematicPaster.getPlugin().getCommand("paste").setExecutor(new CommandPaste());
	}
}
