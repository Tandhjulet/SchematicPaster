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
			return true;
		} else if (!sender.isOp()) {
			sender.sendMessage("Not permitted.");
			return true;
		} else if (args.length == 0) {
			sender.sendMessage("/paste <map index>");
			return true;
		}

		MapManager manager = MapManager.get();
		if (!manager.isMapAvailable(args[0])) {
			sender.sendMessage("Could not find file " + args[0] + ".");
			return true;
		}

		try {
			manager.pasteMap(args[0], ((Player) sender).getLocation());
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
