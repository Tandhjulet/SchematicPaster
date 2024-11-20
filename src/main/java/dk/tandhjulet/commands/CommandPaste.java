package dk.tandhjulet.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import dk.tandhjulet.SchematicPaster;
import dk.tandhjulet.map.MapManager;

public class CommandPaste implements CommandExecutor {
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.isOp()) {
			sender.sendMessage("Not permitted.");
			return false;
		}
		if (args.length == 0) {
			sender.sendMessage("/paste <map index>");
			return false;
		}

		int mapNum = new Integer(args[0]);
		if (mapNum < 1 || mapNum > MapManager.get().size()) {
			sender.sendMessage("Invalid index");
			return false;
		}

		MapManager.get().pasteMap(mapNum - 1);

		return true;
	}

	public static void register() {
		SchematicPaster.getPlugin().getCommand("paste").setExecutor(new CommandPaste());
	}
}
