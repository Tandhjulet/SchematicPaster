package dk.tandhjulet.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import eu.okaeri.configs.OkaeriConfig;
import eu.okaeri.configs.annotation.Comment;
import lombok.Getter;

public class Config extends OkaeriConfig {
	private transient static org.bukkit.World defaultWorld = Bukkit.getWorlds().get(0);

	@Getter
	@Comment({ "Origin to place the map at. If the schematic origin is at",
			"spawn, this location should be at spawn aswell." })
	private Location mapOrigin = new Location(defaultWorld, 0, 0, 0);

	@Getter
	@Comment("Define what maps that should be in rotation here.")
	private List<MapConfig> maps = new ArrayList<>(Collections.singleton(new MapConfig()));

	public class MapConfig extends OkaeriConfig {
		@Comment("Map name")
		private String name = "Test map";

		@Comment("Path to schematic file")
		private String schemFile = "test.schem";
	}

	@Getter
	@Comment("Max chunks to place pr. second. Set to -1 to disable.")
	private Integer maxChunksPerSecond = -1;
}
