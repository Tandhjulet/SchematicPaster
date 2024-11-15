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

	/**
	 * For reverse lookup
	 * [ i | j ] => x, y or z
	 */
	public final static byte[][] CACHE_X = new byte[16][4096];
	public final static short[][] CACHE_Y = new short[16][4096];
	public final static byte[][] CACHE_Z = new byte[16][4096];

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

	public static boolean hasLight(int id) {
		switch (id) {
			case 39:
			case 40:
			case 50:
			case 51:
			case 76:
			case 10:
			case 11:
			case 62:
			case 74:
			case 89:
			case 122:
			case 124:
			case 130:
			case 138:
			case 169:
			case 213:
				return true;
			default:
				return false;
		}
	}

	static {
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				for (int y = 0; y < 256; y++) {
					final short i = (short) (y >> 4);
					final short j = (short) (((y & 0xF) << 8) | (z << 4) | x);
					CACHE_I[y][z][x] = i;
					CACHE_J[y][z][x] = j;

					CACHE_X[i][j] = (byte) x;
					CACHE_Y[i][j] = (short) y;
					CACHE_Z[i][j] = (byte) z;
				}
			}
		}
	}
}