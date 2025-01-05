package dk.tandhjulet.config;

import org.bukkit.configuration.file.FileConfiguration;

import dk.tandhjulet.SchematicPaster;
import lombok.Getter;

public class Config {

	private final SchematicPaster instance;
	private FileConfiguration fileConf;

	@Getter
	private int maxChunksPerSecond = -1;
	@Getter
	private boolean debug = false;

	public Config(SchematicPaster plugin) {
		this.instance = plugin;
		this.fileConf = instance.getConfig();

		fileConf.addDefault("maxChunksPerSecond", -1);
		fileConf.addDefault("debug", false);
		fileConf.options().copyDefaults(true);
		instance.saveConfig();

		reload();
	}

	public void reload() {
		instance.reloadConfig();
		this.fileConf = instance.getConfig();

		maxChunksPerSecond = fileConf.getInt("maxChunksPerSecond");
		debug = fileConf.getBoolean("debug");
	}
}
