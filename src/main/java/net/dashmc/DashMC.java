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
	// Precalculates and stores indexes to id array
	public final static short[][][] CACHE_I = new short[256][16][16];
	public final static short[][][] CACHE_J = new short[256][16][16];

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

		CommandDash.register();
		MapManager.get();
	}

	public FileConfiguration getConfig() {
		return null;
	}

	static {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 256; y++) {
					final short i = (short) (y >> 4);
					final short j = (short) (((y & 0xF) << 8) | (z << 4) | x);
					CACHE_I[y][z][x] = i;
					CACHE_J[y][z][x] = j;
				}
			}
		}
	}
}