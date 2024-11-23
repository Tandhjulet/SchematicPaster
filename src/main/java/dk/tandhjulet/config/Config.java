package dk.tandhjulet.config;

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
	@Comment("Max chunks to place pr. second. Set to -1 to disable.")
	private Integer maxChunksPerSecond = -1;
}
