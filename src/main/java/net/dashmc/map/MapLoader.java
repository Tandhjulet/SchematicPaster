package net.dashmc.map;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.World;

public class MapLoader {
	private Schematic schematic;
	private World nmsWorld;
	private org.bukkit.World bukkitWorld;

	public MapLoader(Schematic schematic) {
		this.schematic = schematic;
	}

	public void paste(Location loc) {
		bukkitWorld = loc.getWorld();
		nmsWorld = ((CraftWorld) bukkitWorld).getHandle();

		final int pasteLocX = loc.getBlockX();
		final int pasteLocY = loc.getBlockY();
		final int pasteLocZ = loc.getBlockZ();

		Bukkit.getLogger().info("x: " + pasteLocX + " y: " + pasteLocY + " z: " + pasteLocZ);

		final int length = schematic.getLength();
		final int width = schematic.getWidth();

		final byte[] blockIds = schematic.getBlockIds();
		final byte[] blockData = schematic.getBlockData();

		for (int x = 0; x < schematic.getWidth(); x++) {
			for (int z = 0; z < schematic.getLength(); z++) {
				for (int y = 0; y < schematic.getHeight(); y++) {
					final int index = (y * length + z) * width + x;

					IBlockData ibd = Block.getByCombinedId(blockIds[index] + (blockData[index] << 12));

					final int worldLocX = pasteLocX + x;
					final int worldLocY = pasteLocY + y;
					final int worldLocZ = pasteLocZ + z;
					Chunk chunk = ensureLoaded(worldLocX >> 4, worldLocZ >> 4);

					ChunkSection chunkSection = chunk.getSections()[worldLocY >> 4];
					if (chunkSection == null) {
						chunkSection = new ChunkSection(worldLocY >> 4 << 4, !chunk.world.worldProvider.o());
						chunk.getSections()[worldLocY >> 4] = chunkSection;
					}

					chunkSection.setType(worldLocX & 15, worldLocY & 15, worldLocZ & 15, ibd);
					// We can recalculate this ourselves (i think ?)
					chunkSection.recalcBlockCounts();

					final BlockPosition blockPosition = new BlockPosition(worldLocX, worldLocY, worldLocZ);
					nmsWorld.notify(blockPosition);
				}
			}
		}
	}

	public Chunk ensureLoaded(int x, int z) {
		Chunk chunk = nmsWorld.getChunkAt(x, z);
		if (bukkitWorld.isChunkLoaded(x, z)) {
			bukkitWorld.loadChunk(x, z);
		}
		return chunk;
	}

	public Set<Chunk> getAndLoadChunksAtSchematic(Location loc) {
		World nmsWorld = ((CraftWorld) loc.getWorld()).getHandle();
		org.bukkit.World bukkitWorld = loc.getWorld();

		final Set<Chunk> chunks = new HashSet<>();

		// 16 = 1, 32 = 2, ...
		final int chunkZ = loc.getBlockZ() >> 4;
		final int chunkX = loc.getBlockX() >> 4;

		final int schemLength = (schematic.getLength() >> 4) + chunkZ;
		final int schemWidth = (schematic.getWidth() >> 4) + chunkX;

		for (int x = chunkX; x < schemWidth; x++) {
			for (int z = chunkZ; z < schemLength; z++) {
				// This loads the chunk if it isn't loaded.
				chunks.add(nmsWorld.getChunkAt(x, z));

				if (!bukkitWorld.isChunkLoaded(x, z))
					loc.getWorld().loadChunk(x, z);
			}
		}

		return chunks;
	}
}
