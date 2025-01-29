package dk.tandhjulet.map;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import dk.tandhjulet.SchematicPaster;

public class MapManager {
	private HashMap<String, File> schematics = new HashMap<>();

	private static MapManager instance;
	public static final File schematicDirectory = new File(SchematicPaster.getPlugin().getDataFolder(), "schematics");

	private MapManager() {
		instance = this;

		for (File schematicFile : schematicDirectory.listFiles()) {
			schematics.put(schematicFile.getName(), schematicFile);
		}
	}

	public Schematic readSchematic(String fileName) throws IOException {
		File schematicFile = schematics.get(fileName);
		return new SchematicReader(schematicFile).getSchematic();
	}

	public void pasteMap(String fileName, Location loc) throws IOException {
		Schematic map = readSchematic(fileName);
		map.load(loc);
		Bukkit.getLogger().info("[SchematicPaster] Pasting...");
		map.paste();
	}

	public boolean isMapAvailable(String fileName) {
		return schematics.containsKey(fileName);
	}

	public int size() {
		return schematics.size();
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
