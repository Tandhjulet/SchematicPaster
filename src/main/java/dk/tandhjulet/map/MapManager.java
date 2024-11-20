package dk.tandhjulet.map;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;

import dk.tandhjulet.SchematicPaster;
import dk.tandhjulet.config.Config.MapConfig;

public class MapManager {
	private ArrayList<Schematic> maps = new ArrayList<>();

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
				File schemFile = new File(schematicDirectory, filePath);
				if (!schemFile.exists()) {
					Bukkit.getLogger().severe("File " + filePath + " doesn't exist.");
					return;
				}

				Schematic registered = new SchematicReader(schemFile).getSchematic();
				maps.add(registered);
			} catch (Exception e) {
				e.printStackTrace();
			}

		});
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
