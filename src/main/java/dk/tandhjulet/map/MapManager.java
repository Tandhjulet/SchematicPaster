package dk.tandhjulet.map;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import dk.tandhjulet.SchematicPaster;

public class MapManager {
	private ArrayList<Schematic> maps = new ArrayList<>();
	private ArrayList<File> schematics = new ArrayList<>();

	private static MapManager instance;
	private static File schematicDirectory = new File(SchematicPaster.getPlugin().getDataFolder(), "schematics");

	private MapManager() {
		instance = this;

		for (File schematicFile : schematicDirectory.listFiles()) {
			schematics.add(schematicFile);
		}
	}

	public Schematic getSchematic(int index) throws IOException {
		File schematicFile = schematics.get(index);
		return new SchematicReader(schematicFile).getSchematic();
	}

	public void pasteMap(int index, Location loc) throws IOException {
		Schematic map = getSchematic(index);
		map.load(loc);
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
