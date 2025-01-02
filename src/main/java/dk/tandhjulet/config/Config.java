package dk.tandhjulet.config;

import org.bukkit.configuration.file.FileConfiguration;

import dk.tandhjulet.SchematicPaster;
import lombok.Getter;

public class Config {

	final SchematicPaster instance;
	final FileConfiguration fileConf;

	@Getter
	int maxChunksPerSecond = -1;

	public Config(SchematicPaster plugin) {
		this.instance = plugin;
		this.fileConf = instance.getConfig();

		fileConf.addDefault("maxChunksPerSecond", -1);
		fileConf.options().copyDefaults(true);
		instance.saveConfig();

		reload();
	}

	public void reload() {
		maxChunksPerSecond = fileConf.getInt("maxChunksPerSecond");
	}
}
