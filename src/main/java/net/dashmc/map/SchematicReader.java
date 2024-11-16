package net.dashmc.map;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;

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

	public void readDimensions() {
		width = nbtTagCompound.getShort("Width");
		height = nbtTagCompound.getShort("Height");
		length = nbtTagCompound.getShort("Length");
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
		schematic.setDimensions(width, height, length);

		return schematic;
	}
}
