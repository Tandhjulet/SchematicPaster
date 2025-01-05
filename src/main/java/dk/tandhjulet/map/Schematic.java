package dk.tandhjulet.map;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import dk.tandhjulet.SchematicPaster;
import dk.tandhjulet.object.SimpleLocation;
import lombok.Data;
import net.minecraft.server.v1_8_R3.NBTTagCompound;

@Data
public class Schematic {
	private ChunkMap map;

	private AtomicBoolean loading = new AtomicBoolean(true);

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

	private byte[] ids;
	private byte[] datas;

	private int mx;
	private int my;
	private int mz;

	private Vector minPoint;

	// this can probably be optimized...
	private final HashMap<SimpleLocation, NBTTagCompound> nbtMapLoc;
	private final HashMap<Integer, NBTTagCompound> nbtMapIndex;

	public Schematic(int width, int height, int length) throws IOException {
		this.width = width;
		this.height = height;
		this.length = length;
		this.area = width * length;
		this.volume = area * height;
		ids = new byte[volume];
		datas = new byte[volume];

		nbtMapLoc = new HashMap<>();
		nbtMapIndex = new HashMap<>();
	}

	public void paste() {
		if (loading.get()) {
			Bukkit.getLogger().warning("[SchematicPaster] Did not finish loading before paste was attempted.");
			return;
		}

		long pretick = System.currentTimeMillis();
		boolean debug = SchematicPaster.getConf().isDebug();

		BukkitRunnable runnable = new BukkitRunnable() {
			Iterator<SchematicChunk> chunkIterator = map.getSchematicChunks().iterator();
			int maxChunks = (SchematicPaster.getConf().getMaxChunksPerSecond() == -1) ? map.getSchematicChunks().size()
					: SchematicPaster.getConf().getMaxChunksPerSecond();
			long posttick = Long.MIN_VALUE;

			@Override
			public void run() {
				if (posttick == Long.MIN_VALUE)
					posttick = System.currentTimeMillis();

				for (int i = 0; i < maxChunks; i++) {
					long pasteTime = System.currentTimeMillis();

					SchematicChunk chunk = chunkIterator.next();
					chunk.update();
					chunk.sendChunk();

					if (debug) {
						long res = System.currentTimeMillis() - pasteTime;
						Bukkit.getLogger()
								.info("Pasted chunk at " + chunk.getX() + " z: " + chunk.getZ() + " (Took " + res
										+ "ms)");
					}

					if (!chunkIterator.hasNext()) {
						cancel();
						map.chunks.clear();

						long now = System.currentTimeMillis();
						long pretickDelta = now - pretick;
						long nowDelta = now - posttick;

						Bukkit.getLogger().info(String.format(
								"[SchematicPaster] Done pasting. Took %,d ms (%,d ms pretick)", nowDelta,
								pretickDelta));
						return;
					}
				}
			}
		};
		runnable.runTaskTimer(SchematicPaster.getPlugin(), 0L, 20L);
	}

	public void convertTilesToIndex() {
		if (nbtMapLoc.isEmpty()) {
			return;
		}

		for (Map.Entry<SimpleLocation, NBTTagCompound> entry : nbtMapLoc.entrySet()) {
			SimpleLocation key = entry.getKey();
			setTile(getIndex(key.getX(), key.getY(), key.getZ()), entry.getValue());
		}
		nbtMapLoc.clear();
	}

	public void setTile(int x, int y, int z, NBTTagCompound compoundTag) {
		if (compoundTag.isEmpty())
			return;
		nbtMapLoc.put(new SimpleLocation(x, y, z), compoundTag);
	}

	public void setTile(int index, NBTTagCompound compoundTag) {
		if (compoundTag.isEmpty())
			return;
		compoundTag.remove("x");
		compoundTag.remove("y");
		compoundTag.remove("z");
		nbtMapIndex.put(index, compoundTag);
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

	/**
	 * Load schematic to be pasted
	 * 
	 * @param to Location to load the schematic at. It will get pasted here once
	 *           #paste() is called.
	 */
	public void load(Location to) {
		map = new ChunkMap(to.getWorld());

		final int relx = to.getBlockX() + minPoint.getBlockX() - getMx();
		final int rely = to.getBlockY() + minPoint.getBlockY() - getMy();
		final int relz = to.getBlockZ() + minPoint.getBlockZ() - getMz();

		Bukkit.getLogger().info("[SchematicPaster] Loading " + length * height * width + " blocks...");

		for (int y = 0, index = 0; y < height; y++) {
			for (int z = 0; z < length; z++) {
				for (int x = 0; x < width; x++, index++) {
					int xx = x + relx;
					int zz = z + relz;
					int yy = y + rely;

					setChunkBlock(xx, yy, zz, index);
				}
			}
		}
		// deleteData();
		loading.set(false);
		Bukkit.getLogger().info("[SchematicPaster] Finished loading.");
	}

	public void deleteData() {
		ids = new byte[0];
		datas = new byte[0];

		nbtMapIndex.clear();
		nbtMapLoc.clear();
	}

	private NBTTagCompound getTag(int index) {
		convertTilesToIndex();
		return nbtMapIndex.get(index);
	}

	private void setChunkBlock(int x, int y, int z, int index) {
		int id = getId(index);
		int data = getData(index);

		int cx = x >> 4;
		int cz = z >> 4;

		SchematicChunk schemChunk = map.getSchematicChunk(cx, cz);
		schemChunk.setBlock(x & 15, y, z & 15, id, data);

		if (SchematicPaster.hasNBT(id)) {
			NBTTagCompound compoundTag = getTag(index);
			schemChunk.setTile(x & 15, y, z & 15, compoundTag);
		}

	}

	public void setBlock(int x, int y, int z, int id, int data) {
		setBlock(getIndex(x, y, z), id, data);
	}

	public void setBlock(int index, int id, int data) {
		setId(index, id);
		setData(index, data);
	}
}
