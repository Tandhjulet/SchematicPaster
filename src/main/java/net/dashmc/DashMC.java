package net.dashmc;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import eu.okaeri.configs.ConfigManager;
import eu.okaeri.configs.yaml.bukkit.YamlBukkitConfigurer;
import eu.okaeri.configs.yaml.bukkit.serdes.SerdesBukkit;
import lombok.Getter;
import net.dashmc.commands.CommandDash;
import net.dashmc.config.Config;
import net.dashmc.map.MapManager;

public class DashMC extends JavaPlugin {

	@Getter
	private static DashMC plugin;

	@Getter
	private static Config conf;

	@Override
	public void onEnable() {
		plugin = this;

		conf = ConfigManager.create(Config.class, (conf) -> {
			conf.withConfigurer(new YamlBukkitConfigurer(), new SerdesBukkit());
			conf.withBindFile(new File(this.getDataFolder(), "config.yml"));
			conf.saveDefaults();
			conf.load(true);
		});

		MapManager.get();
		CommandDash.register();
	}

	public FileConfiguration getConfig() {
		return null;
	}

	public static MapManager getMapManager() {
		return MapManager.get();
	}
}