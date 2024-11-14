package net.dashmc.map;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

import lombok.Data;
import net.dashmc.DashMC;
import net.dashmc.object.RunnableWithVal;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;

@Data
public class Schematic {
	private static final byte COMPOUND_ID = 10;
	private ChunkMap map = new ChunkMap();

	private AtomicBoolean loading = new AtomicBoolean(true);

	private int blockCount;
	private short width; // X
	private short height; // Y
	private short length; // Z
	private int floorArea;

	private char[][] ids;

	private NBTTagList entities;
	private NBTTagList tileEntities;

	public Schematic(File file) throws IOException {
		final InputStream in = new FileInputStream(file);
		final NBTTagCompound tagCompound = NBTCompressedStreamTools.a(in);

		width = tagCompound.getShort("Width");
		height = tagCompound.getShort("Height");
		length = tagCompound.getShort("Length");

		floorArea = width * length;
		ids = new char[floorArea * ((height + 15) >> 4)][];

		entities = tagCompound.getList("Entities", COMPOUND_ID);
		tileEntities = tagCompound.getList("TileEntities", COMPOUND_ID);

		in.close();

		loadBlockIDs(tagCompound.getByteArray("Blocks"), tagCompound.getByteArray("Data"));
	}

	public char[] getIdArray(int i) {
		return this.ids[i];
	}

	public void paste() {
		map.forEach(new RunnableWithVal<SchematicChunk>() {
			@Override
			public void run(SchematicChunk chunk) {
				chunk.update();
			}
		});
	}

	private void loadBlockIDs(byte[] blockIds, byte[] blockData) {
		Location placeAt = DashMC.getConf().getMapOrigin();

		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				for (int y = 0, index = 0; y < height; y++) {
					for (int z = 0; z < length; z++) {
						for (int x = 0; x < width; z++, index++) {
							int cx = (x + placeAt.getBlockX()) >> 4;
							int cz = (z + placeAt.getBlockZ()) >> 4;

							SchematicChunk schemChunk = map.getSchematicChunk(cx, cz);
							schemChunk.setBlock(x & 15, y, z & 15, blockIds[index], blockData[index]);
						}
					}
				}

				loading.set(false);
			}
		};

		runnable.runTaskAsynchronously(DashMC.getPlugin());
	}
}
