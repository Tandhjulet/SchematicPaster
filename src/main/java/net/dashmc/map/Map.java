package net.dashmc.map;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import net.dashmc.DashMC;
import net.minecraft.server.v1_8_R3.Chunk;

import java.lang.System;

public class Map {
	private static Location loc = DashMC.getConf().getMapOrigin();

	private Schematic schematic;
	private MapLoader loader;

	public Map(File schem) throws IOException {
		schematic = new Schematic(schem);
		this.loader = new MapLoader(schematic);
	}

	public void run() {
		if (schematic.getBlockIds().length == 0) {
			return;
		}
		long time = System.currentTimeMillis();

		Set<Chunk> affectedChunks = loader.getAndLoadChunksAtSchematic(loc);
		loader.paste(loc);

		affectedChunks.forEach(chunk -> {
			chunk.initLighting();
		});

		int count = schematic.getWidth() * schematic.getHeight() * schematic.getLength();
		Bukkit.getLogger().info("Completed. Time taken: " + (System.currentTimeMillis() - time)
				+ " (ms) for " + count + " blocks!");
	}
}