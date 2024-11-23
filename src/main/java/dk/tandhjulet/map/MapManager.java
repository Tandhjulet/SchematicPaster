package dk.tandhjulet.map;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;

import dk.tandhjulet.SchematicPaster;
import dk.tandhjulet.config.Config.MapConfig;

public class MapManager {
	private HashMap<String, Schematic> maps = new HashMap<>();
	private ArrayList<String> mapNames = new ArrayList<>();

	private static MapManager instance;
	private static File schematicDirectory = new File(SchematicPaster.getPlugin().getDataFolder(), "schematics");

	private MapManager() {
		instance = this;

		List<MapConfig> registeredMaps = SchematicPaster.getConf().getMaps();

		if (registeredMaps == null)
			return;

		registeredMaps.forEach((map) -> {
			String filePath = map.get("schemFile", String.class);

			try {
				String mapName = map.get("name", String.class);

				File schemFile = new File(schematicDirectory, filePath);
				if (!schemFile.exists()) {
					Bukkit.getLogger().severe("File " + filePath + " doesn't exist.");
					return;
				}

				Schematic registered = new SchematicReader(schemFile).getSchematic();
				maps.put(mapName, registered);
				mapNames.add(mapName);
			} catch (Exception e) {
				e.printStackTrace();
			}

		});
	}

	public void pasteMap(int index) {
		String mapName = mapNames.get(index);
		pasteMap(mapName);
	}

	public void pasteMap(String name) {
		Schematic map = maps.get(name);
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
