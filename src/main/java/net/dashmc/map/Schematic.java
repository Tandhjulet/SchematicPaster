package net.dashmc.map;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.util.Vector;

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

	private int mx;
	private int my;
	private int mz;

	private Vector minPoint;

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
			chunk.sendChunk();
		}
	}

	public void setId(int index, int value) {
		ids[index] = (byte) value;
	}

	public void setData(int index, int value) {
		datas[index] = (byte) value;
	}

	private int getData(int index) {
		return datas[index];
	}

	private int getId(int index) {
		return ids[index] & 0xFF;
	}

	public void setOrigin(Vector origin) {
		mx = origin.getBlockX();
		mz = origin.getBlockZ();
		my = origin.getBlockY();
	}

	public void setDimensions(int width, int height, int length) {
		this.width = width;
		this.height = height;
		this.length = length;

		this.area = width * length;
		int newVolume = area * height;
		if (newVolume != volume) {
			volume = newVolume;
			ids = new byte[volume];
			datas = new byte[volume];
		}
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
		Location to = DashMC.getConf().getMapOrigin();
		final int relx = to.getBlockX() + minPoint.getBlockX() - getMx();
		final int rely = to.getBlockY() + minPoint.getBlockY() - getMy();
		final int relz = to.getBlockZ() + minPoint.getBlockZ() - getMz();

		Bukkit.getLogger().info("Loading... (" + length * height * width + " blocks)");

		for (int y = 0, index = 0; y < height; y++) {
			for (int z = 0; z < length; z++) {
				for (int x = 0; x < width; x++, index++) {
					int xx = x + relx;
					int zz = z + relz;
					int yy = y + rely;

					setChunkBlock(xx, yy, zz, getId(index), getData(index));
				}
			}
		}
		loading.set(false);
	}

	private void setChunkBlock(int x, int y, int z, int id, int data) {
		int cx = x >> 4;
		int cz = z >> 4;

		SchematicChunk schemChunk = map.getSchematicChunk(cx, cz);
		schemChunk.setBlock(x & 15, y, z & 15, id, data);
	}

	public void setBlock(int x, int y, int z, int id, int data) {
		setBlock(getIndex(x, y, z), id, data);
	}

	public void setBlock(int index, int id, int data) {
		setId(index, id);
		setData(index, data);
	}
}
