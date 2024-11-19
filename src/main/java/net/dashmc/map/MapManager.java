package net.dashmc.map;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;

import net.dashmc.DashMC;
import net.dashmc.config.Config.MapConfig;

public class MapManager {
	private ArrayList<Schematic> maps = new ArrayList<>();

	private static MapManager instance;
	private static File schematicDirectory = new File(DashMC.getPlugin().getDataFolder(), "schematics");

	private MapManager() {
		instance = this;

		List<MapConfig> registeredMaps = DashMC.getConf().getMaps();

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
		Bukkit.getLogger().info("Loading...");
		map.load();
		Bukkit.getLogger().info("Pasting...");
		map.paste();
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
