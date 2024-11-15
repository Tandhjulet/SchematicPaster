package net.dashmc.map;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import lombok.Data;
import net.dashmc.DashMC;
import net.minecraft.server.v1_8_R3.NBTTagList;

@Data
public class Schematic {
	private ChunkMap map = new ChunkMap();

	private AtomicBoolean loading = new AtomicBoolean(true);

	private int blockCount;

	/**
	 * @return X
	 */
	private int width;

	/**
	 * @return Y
	 */
	private int height;

	/**
	 * @return Z
	 */
	private int length;

	private int area;
	private int volume;

	private NBTTagList entities;
	private NBTTagList tileEntities;

	private byte[] ids;
	private byte[] datas;

	public Schematic(int width, int height, int length) throws IOException {
		this.width = width;
		this.height = height;
		this.length = length;
		this.area = width * length;
		this.volume = area * height;
		ids = new byte[volume];
		datas = new byte[volume];
	}

	public void paste() {
		if (loading.get()) {
			Bukkit.getLogger().warning("Not finished loading.");
			return;
		}

		// TODO: Make async
		Collection<SchematicChunk> chunks = map.getSchematicChunks();
		for (SchematicChunk chunk : chunks) {
			chunk.update();
		}
	}

	public void setId(int index, int value) {
		ids[index] = (byte) value;
	}

	public void setData(int index, int value) {
		datas[index] = (byte) value;
	}

	private int getId(int index) {
		return ids[index] & 0xFF;
	}

	public void setDimensions(int width, int height, int length) {
		this.width = width;
		this.height = height;
		this.length = length;
	}

	private int ylast;
	private int ylasti;
	private int zlast;
	private int zlasti;

	public int getIndex(int x, int y, int z) {
		return x + ((ylast == y) ? ylasti : (ylasti = (ylast = y) * area))
				+ ((zlast == z) ? zlasti : (zlasti = (zlast = z) * width));
	}

	public int getBlock(int index) {
		int id = getId(index);
		return id;
	}

	public void load() {
		Location placeAt = DashMC.getConf().getMapOrigin();

		Bukkit.getLogger().info("Loading...");

		for (int y = 0, index = 0; y < height; y++) {
			Bukkit.getLogger().info("y: " + y);
			for (int z = 0; z < length; z++) {
				Bukkit.getLogger().info("z: " + z);
				for (int x = 0; x < width; x++, index++) {
					int cx = (x + placeAt.getBlockX()) >> 4;
					int cz = (z + placeAt.getBlockZ()) >> 4;
					Bukkit.getLogger().info("cx: " + cx + " cz: " + cz);

					SchematicChunk schemChunk = map.getSchematicChunk(cx, cz);
					schemChunk.setBlock(x & 15, y, z & 15, getBlock(index), 0);
				}
			}
		}
		loading.set(false);
	}

	public void setBlock(int x, int y, int z, short id, short data) {
		setBlock(getIndex(x, y, z), id, data);
	}

	public void setBlock(int index, short id, short data) {
		setId(index, id);
		setData(index, data);
	}
}
