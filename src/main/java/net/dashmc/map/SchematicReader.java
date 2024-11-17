package net.dashmc.map;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bukkit.util.Vector;

import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;

public class SchematicReader {
	private Schematic schematic;

	public NBTTagCompound nbtTagCompound;

	public SchematicReader(File file) throws IOException {
		final InputStream in = new FileInputStream(file);
		nbtTagCompound = NBTCompressedStreamTools.a(in);
	}

	int height;
	int width;
	int length;

	private int originX;
	private int originY;
	private int originZ;

	private int offsetX;
	private int offsetY;
	private int offsetZ;

	public void readDimensions() {
		width = nbtTagCompound.getShort("Width");
		height = nbtTagCompound.getShort("Height");
		length = nbtTagCompound.getShort("Length");

		originX = nbtTagCompound.getInt("WEOriginX");
		originY = nbtTagCompound.getInt("WEOriginY");
		originZ = nbtTagCompound.getInt("WEOriginZ");

		offsetX = nbtTagCompound.getInt("WEOffsetX");
		offsetY = nbtTagCompound.getInt("WEOffsetY");
		offsetZ = nbtTagCompound.getInt("WEOffsetZ");
	}

	public void readBlocks() throws IOException {
		byte[] blocks = nbtTagCompound.getByteArray("Blocks");
		setupSchematic(blocks.length);

		for (int i = 0; i < blocks.length; i++) {
			if (blocks[i] == 0)
				continue;

			schematic.setId(i, blocks[i]);
		}

		byte[] data = nbtTagCompound.getByteArray("Data");
		setupSchematic(data.length);
		for (int i = 0; i < data.length; i++) {
			if (data[i] == 0)
				continue;

			schematic.setData(i, data[i]);
		}

		NBTTagList compoundList = nbtTagCompound.getList("TileEntities", 10);
		for (int i = 0; i < compoundList.size(); i++) {
			NBTTagCompound compound = compoundList.get(i);
			int x = compound.getInt("x");
			int y = compound.getInt("y");
			int z = compound.getInt("z");
			schematic.setTile(x, y, z, compound);
		}
	}

	private Schematic setupSchematic(int size) throws IOException {
		if (schematic != null) {
			if (schematic.getWidth() == 0) {
				schematic.setDimensions(size, 1, 1);
			}
			return schematic;
		}

		return schematic = new Schematic(size, 1, 1);
	}

	public Schematic getSchematic() throws IOException {
		readDimensions();
		readBlocks();

		Vector min = new Vector(originX, originY, originZ);
		Vector offset = new Vector(offsetX, offsetY, offsetZ);

		schematic.setMinPoint(min.clone());
		schematic.setOrigin(min.subtract(offset));

		schematic.setDimensions(width, height, length);

		return schematic;
	}
}
