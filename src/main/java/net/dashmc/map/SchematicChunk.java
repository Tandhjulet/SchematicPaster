package net.dashmc.map;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;

import net.dashmc.DashMC;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.LongHashMap;
import net.minecraft.server.v1_8_R3.NibbleArray;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutTileEntityData;
import net.minecraft.server.v1_8_R3.PlayerChunkMap;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.WorldServer;

public class SchematicChunk {
	public static int HEIGHT = 256;

	static private Field chunkMapField;
	static private final ChunkSection emptySection = new ChunkSection(0, true);
	static private NibbleArray fullSkyLight;

	private Chunk chunk;
	public final char[][] ids;
	public final short[] count;
	public final short[] air;
	public final byte[] heightMap;

	private int x, z;

	public SchematicChunk(int x, int z) {
		this.x = x;
		this.z = z;

		this.ids = new char[HEIGHT >> 4][];
		this.count = new short[HEIGHT >> 4];
		this.air = new short[HEIGHT >> 4];
		this.heightMap = new byte[256];
	}

	public void setCount(final int i, final short value) {
		this.count[i] = value;
	}

	public char[] getIdArray(final int i) {
		return this.ids[i];
	}

	public void updateHeightMap() {
		CraftChunk craftChunk = (CraftChunk) getChunk();
		if (craftChunk != null) {
			int[] otherMap = craftChunk.getHandle().heightMap;
			for (int i = 0; i < heightMap.length; i++) {
				int newHeight = heightMap[i] & 0xFF;
				int currentHeight = otherMap[i];
				if (newHeight > currentHeight) {
					otherMap[i] = newHeight;
				}
			}
		}
	}

	// Directly copied from
	// https://github.com/IntellectualSites/FastAsyncWorldedit-Legacy/blob/e3a89ad9d4a0437af48be84af0088901a3d6c822/core/src/main/java/com/boydti/fawe/example/CharFaweChunk.java#L136
	public void setBlock(int x, int y, int z, int id, int data) {
		final int i = DashMC.CACHE_I[y][z][x];
		final int j = DashMC.CACHE_J[y][z][x];
		char[] vs = this.ids[i];
		if (vs == null) {
			vs = this.ids[i] = new char[4096];
			this.count[i]++;
		} else {
			switch (vs[j]) {
				case 0:
					this.count[i]++;
					break;
				case 1:
					this.air[i]--;
					break;
			}
		}
		switch (id) {
			case 0:
				this.air[i]++;
				vs[j] = (char) 1;
				return;
			case 39:
			case 40:
			case 51:
			case 74:
			case 89:
			case 122:
			case 124:
			case 138:
			case 169:
			case 213:
			case 2:
			case 4:
			case 13:
			case 14:
			case 15:
			case 20:
			case 21:
			case 22:
			case 30:
			case 32:
			case 37:
			case 41:
			case 42:
			case 45:
			case 46:
			case 47:
			case 48:
			case 49:
			case 56:
			case 57:
			case 58:
			case 7:
			case 73:
			case 79:
			case 80:
			case 81:
			case 82:
			case 83:
			case 85:
			case 87:
			case 88:
			case 101:
			case 102:
			case 103:
			case 110:
			case 112:
			case 113:
			case 121:
			case 129:
			case 133:
			case 165:
			case 166:
			case 172:
			case 173:
			case 174:
			case 190:
			case 191:
			case 192:
				vs[j] = (char) (id << 4);
				heightMap[z << 4 | x] = (byte) y;
				return;
			default:
				vs[j] = (char) ((id << 4) + data);
				heightMap[z << 4 | x] = (byte) y;
				return;
		}
	}

	public Chunk getChunk() {
		if (chunk == null) {
			World bukkitWorld = DashMC.getConf().getMapOrigin().getWorld();
			chunk = bukkitWorld.getChunkAt(x, z);
		}
		return chunk;
	}

	public void update() {
		CraftChunk chunk = (CraftChunk) getChunk();
		net.minecraft.server.v1_8_R3.Chunk nmsChunk = chunk.getHandle();

		// Chunk has been modified
		nmsChunk.f(true);
		nmsChunk.mustSave = true;

		// net.minecraft.server.v1_8_R3.World nmsWorld = nmsChunk.getWorld();
		try {
			ChunkSection[] sections = nmsChunk.getSections();
			// java.util.Map<BlockPosition, TileEntity> tileEntities =
			// nmsChunk.getTileEntities();
			// Collection<Entity>[] entities = nmsChunk.getEntitySlices();

			updateHeightMap();

			// Removes entities
			// Set entities

			// Set blocks
			for (int j = 0; j < sections.length; j++) {
				int count = this.count[j];
				if (count == 0)
					continue;

				char[] newArray = this.getIdArray(j);
				if (newArray == null)
					continue;

				int countAir = this.air[j];
				ChunkSection section = sections[j];
				if (section == null) {
					if (count == countAir)
						continue;

					sections[j] = section = new ChunkSection(j << 4, false, newArray);
					continue;
				}

				if (count >= 4096) {
					if (count == countAir) {
						sections[j] = null;
						continue;
					}
					sections[j] = section = new ChunkSection(j << 4, false, newArray);
					continue;
				}
			}

		} catch (Exception e) {

		}

	}

	public void sendChunk(int bitMask) {
		CraftChunk craftChunk = (CraftChunk) getChunk();
		net.minecraft.server.v1_8_R3.Chunk nmsChunk = craftChunk.getHandle();

		try {
			WorldServer worldServer = (WorldServer) nmsChunk.getWorld();
			PlayerChunkMap chunkMap = worldServer.getPlayerChunkMap();

			int x = nmsChunk.locX;
			int z = nmsChunk.locZ;
			if (chunkMap.isChunkInUse(x, z))
				return;

			@SuppressWarnings("unchecked")
			LongHashMap<Object> map = (LongHashMap<Object>) chunkMapField.get(chunkMap);

			// https://hub.spigotmc.org/stash/projects/CHEETAH/repos/craftbukkit/browse/src/main/java/net/minecraft/server/PlayerChunkMap.java?at=38fbe60d4689ff026f09767ac9a5656da0549c2d
			long pair = (long) x + 2147483647L | (long) z + 2147483647L << 32;

			// The class PlayerChunk and it's associated fields are private
			Object playerChunk = map.getEntry(pair);
			Field fieldPlayers = playerChunk.getClass().getDeclaredField("b");
			fieldPlayers.setAccessible(true);

			@SuppressWarnings("unchecked")
			Collection<EntityPlayer> players = (Collection<EntityPlayer>) fieldPlayers.get(playerChunk);
			if (players.isEmpty())
				return;

			boolean empty = false;
			ChunkSection[] sections = nmsChunk.getSections();

			// Make sure there are no empty sections, and make every section
			// fully lit.
			for (int i = 0; i < sections.length; ++i) {
				if (sections[i] == null) {
					sections[i] = emptySection;
					empty = true;
				}
				sections[i].b(fullSkyLight);
			}

			// 0xffff = 65535 = all 16 bits are set
			if (bitMask == 0 || bitMask == 0xffff) {
				// skip loading entities
				bitMask = 255;
			}

			// Send packet with "normal" blocks
			PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, false, bitMask);
			players.forEach((player) -> {
				player.playerConnection.sendPacket(packet);
			});

			// Send packets with Tile Entities
			nmsChunk.getTileEntities().entrySet().forEach((entry) -> {
				TileEntity tileEntity = entry.getValue();
				PacketPlayOutTileEntityData tileUpdatePacket = (PacketPlayOutTileEntityData) tileEntity
						.getUpdatePacket();

				players.forEach((player) -> {
					player.playerConnection.sendPacket(tileUpdatePacket);
				});
			});

			if (empty) {
				for (int i = 0; i < sections.length; ++i) {
					if (sections[i] == emptySection) {
						sections[i] = null;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static {
		byte[] byteArray = new byte[2048];
		Arrays.fill(byteArray, (byte) 127);
		fullSkyLight = new NibbleArray(byteArray);

		try {
			chunkMapField = PlayerChunkMap.class.getDeclaredField("d");
			chunkMapField.setAccessible(true);
		} catch (Exception e) {
		}
	}
}
