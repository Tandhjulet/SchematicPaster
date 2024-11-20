package dk.tandhjulet.packet;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk.ChunkMap;

public class ChunkUpdatePacketBuilder {
	private static Field chunkXField;
	private static Field chunkZField;
	private static Field continousField;

	private static Field chunkMapField;

	public static PacketPlayOutMapChunk build(Chunk chunk, int bitMask) {
		PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk();

		try {
			chunkXField.set(packet, chunk.locX);
			chunkZField.set(packet, chunk.locZ);

			continousField.set(packet, false);

			// ChunkMap map = PacketPlayOutMapChunk.a(chunk, false,
			// !chunk.getWorld().worldProvider.o(), bitMask);
			ChunkMap map = ChunkUpdatePacketBuilder.createChunkMap(chunk, !chunk.getWorld().worldProvider.o(), bitMask);
			chunkMapField.set(packet, map);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return packet;
	}

	public static ChunkMap createChunkMap(Chunk chunk, boolean isOverworld, int bitMask) {
		ChunkSection[] chunkSections = chunk.getSections();
		PacketPlayOutMapChunk.ChunkMap chunkMap = new PacketPlayOutMapChunk.ChunkMap();
		List<ChunkSection> arraylist = Lists.newArrayList();

		int j;

		// Make the bitmask in the chunkmap reflect if the sections are empty or not
		for (j = 0; j < chunkSections.length; ++j) {
			ChunkSection chunksection = chunkSections[j];

			// We can check if the section is empty in the bitmask by
			// creating a new mask and rightshifting by the index of the chunksection (j).
			// if the result is 0 the chunk section is empty.
			if (chunksection != null && (bitMask & 1 << j) != 0) {
				chunkMap.b |= 1 << j;
				arraylist.add(chunksection);
			}
		}

		// We use j as our pointer into the data array kept within chunkMap now.
		j = 0;

		// Calculate the bytes needed to hold the chunk data
		int nonEmptyChunks = Integer.bitCount(chunkMap.b);
		chunkMap.a = new byte[claculateNeededBytes(nonEmptyChunks, isOverworld)];
		Iterator<ChunkSection> iterator = arraylist.iterator();

		ChunkSection section;

		while (iterator.hasNext()) {
			section = iterator.next();
			char[] achar = section.getIdArray();
			char[] achar1 = achar;
			int k = achar.length;

			for (int l = 0; l < k; ++l) {
				char c0 = achar1[l];

				chunkMap.a[j++] = (byte) (c0 & 255);
				chunkMap.a[j++] = (byte) (c0 >> 8 & 255);
			}
		}

		// Maybe this should be precomputed?
		int lightArrayLength = nonEmptyChunks * 16 * 16 * 8;
		byte[] newLightArray = new byte[lightArrayLength];
		Arrays.fill(newLightArray, (byte) 255);

		System.arraycopy(newLightArray, 0, chunkMap.a, j, newLightArray.length);

		return chunkMap;
	}

	private static int claculateNeededBytes(int i, boolean flag) {
		int j = i * 2 * 16 * 16 * 16;
		int k = i * 16 * 16 * 8;
		int l = flag ? i * 16 * 16 * 8 : 0;

		return j + k + l;
	}

	static {
		try {
			chunkXField = PacketPlayOutMapChunk.class.getDeclaredField("a");
			chunkXField.setAccessible(true);

			chunkZField = PacketPlayOutMapChunk.class.getDeclaredField("b");
			chunkZField.setAccessible(true);

			chunkMapField = PacketPlayOutMapChunk.class.getDeclaredField("c");
			chunkMapField.setAccessible(true);

			continousField = PacketPlayOutMapChunk.class.getDeclaredField("d");
			continousField.setAccessible(true);
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
	}
}
