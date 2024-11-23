package dk.tandhjulet.map;

import java.io.File;
import java.util.ArrayList;

import org.bukkit.Bukkit;

import dk.tandhjulet.SchematicPaster;

public class MapManager {
	private ArrayList<Schematic> maps = new ArrayList<>();

	private static MapManager instance;
	private static File schematicDirectory = new File(SchematicPaster.getPlugin().getDataFolder(), "schematics");

	private MapManager() {
		instance = this;

		for (File schematicFile : schematicDirectory.listFiles()) {
			try {
				Schematic registered = new SchematicReader(schematicFile).getSchematic();
				maps.add(registered);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void pasteMap(int index) {
		Schematic map = maps.get(index);
		map.load();
		Bukkit.getLogger().info("[SchematicPaster] Pasting...");
		map.paste();
	}

	public int size() {
		return maps.size();
	}

	public static MapManager get() {
		if (instance == null)
			return new MapManager();

		return instance;
	}

	static {
		schematicDirectory.mkdir();
	}
}
