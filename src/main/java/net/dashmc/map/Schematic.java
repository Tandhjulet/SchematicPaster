package net.dashmc.map;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.Data;
import net.minecraft.server.v1_8_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NBTTagList;

@Data
public class Schematic {
	private static final byte COMPOUND_ID = 10;

	private int blockCount;
	private short width; // X
	private short height; // Y
	private short length; // Z

	private byte[] blockIds; // one byte per block.
	private byte[] blockData; // Array of the block data. Only lower 4 bits used in each byte.

	private NBTTagList entities;
	private NBTTagList tileEntities;

	public Schematic(File file) throws IOException {
		final InputStream in = new FileInputStream(file);
		final NBTTagCompound tagCompound = NBTCompressedStreamTools.a(in);

		width = tagCompound.getShort("Width");
		height = tagCompound.getShort("Height");
		length = tagCompound.getShort("Length");

		blockIds = tagCompound.getByteArray("Blocks");
		blockData = tagCompound.getByteArray("Data");

		entities = tagCompound.getList("Entities", COMPOUND_ID);
		tileEntities = tagCompound.getList("TileEntities", COMPOUND_ID);

		in.close();
	}
}
