package net.dashmc.map;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftChunk;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;

import net.dashmc.DashMC;
import net.dashmc.util.MathUtils;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.ChunkSection;
import net.minecraft.server.v1_8_R3.Entity;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import net.minecraft.server.v1_8_R3.NBTTagCompound;
import net.minecraft.server.v1_8_R3.NibbleArray;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutMapChunk;
import net.minecraft.server.v1_8_R3.PacketPlayOutTileEntityData;
import net.minecraft.server.v1_8_R3.PlayerChunkMap;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.WorldServer;

public class SchematicChunk {
	public static int HEIGHT = 256;

	static private Field chunkMapField;
	static private Field fieldTickingBlockCount;
	static private Method getOrCreatePlayerChunk;
	static private Field fieldNonEmptyBlockCount;
	static private final ChunkSection emptySection = new ChunkSection(0, true);
	static private byte[] fullSkyLight = new byte[2048];

	private Chunk chunk;
	public final char[][] ids;
	public final short[] count;
	public final short[] air;
	public final byte[] heightMap;

	private int x, z;

	private HashMap<Short, NBTTagCompound> tiles = new HashMap<>();

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

	// https://github.com/IntellectualSites/FastAsyncWorldedit-Legacy/blob/e3a89ad9d4a0437af48be84af0088901a3d6c822/core/src/main/java/com/boydti/fawe/example/CharFaweChunk.java#L195
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

	public void setTile(int x, int y, int z, NBTTagCompound nbtTagCompound) {
		if (nbtTagCompound == null || nbtTagCompound.isEmpty())
			return;
		short pair = (short) ((x & 15) << 12 | (z & 15) << 8 | y);
		tiles.put(pair, nbtTagCompound);
	}

	public Chunk getChunk() {
		if (chunk == null) {
			WorldServer bukkitWorld = ((CraftWorld) DashMC.getConf().getMapOrigin().getWorld()).getHandle();
			if (ChunkProvider.isInjected()) {
				ChunkProvider chunkProvider = ((ChunkProvider) bukkitWorld.chunkProviderServer);
				chunk = chunkProvider.getChunkAt(x, z, null, false).bukkitChunk;
			} else {
				chunk = bukkitWorld.getChunkAt(x, z).bukkitChunk;
			}
		}
		return chunk;
	}

	public void update() {
		CraftChunk chunk = (CraftChunk) getChunk();
		net.minecraft.server.v1_8_R3.Chunk nmsChunk = chunk.getHandle();

		int bx = x << 4;
		int bz = z << 4;

		// Chunk has been modified
		nmsChunk.f(true);
		nmsChunk.mustSave = true;

		net.minecraft.server.v1_8_R3.World nmsWorld = nmsChunk.getWorld();
		try {
			Collection<Entity>[] entities = nmsChunk.getEntitySlices();
			ChunkSection[] sections = nmsChunk.getSections();
			Map<BlockPosition, TileEntity> tileEntities = nmsChunk.getTileEntities();

			updateHeightMap();

			// Removes entities
			for (int i = 0; i < 16; i++) {
				int count = this.count[i];
				if (count == 0)
					continue;

				Collection<Entity> ents = entities[i];
				if (ents.isEmpty())
					continue;

				if (count >= 4096) {
					Iterator<Entity> entIter = ents.iterator();
					while (entIter.hasNext()) {
						Entity entity = entIter.next();
						if (entity instanceof EntityPlayer)
							continue;
						entIter.remove();
						nmsWorld.removeEntity(entity);
					}
					continue;
				}

				char[] array = getIdArray(i);
				if (array == null)
					continue;

				Entity[] entsArr = ents.toArray(new Entity[ents.size()]);
				for (Entity entity : entsArr) {
					if (entity instanceof EntityPlayer)
						continue;

					int x = (MathUtils.roundInt(entity.locX) & 15);
					int z = (MathUtils.roundInt(entity.locZ) & 15);
					int y = MathUtils.roundInt(entity.locY);
					if (y < 0 || y > 255)
						continue;
					if (array[DashMC.CACHE_J[y][z][x]] != 0) {
						nmsWorld.removeEntity(entity);
					}
				}
			}

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
				if (section == null && count == countAir) {
					continue;
				}

				if (count >= 4096 && section != null) {
					if (count == countAir) {
						sections[j] = null;
						continue;
					}
				}

				sections[j] = section = new ChunkSection(j << 4, true, newArray);
				section.a(new NibbleArray(fullSkyLight));
			}

			// Remove old tile entities
			for (Entry<BlockPosition, TileEntity> entry : tileEntities.entrySet()) {
				BlockPosition blockPosition = entry.getKey();
				TileEntity tileEntity = entry.getValue();

				nmsChunk.tileEntities.remove(blockPosition);
				nmsWorld.t(blockPosition);
				tileEntity.y();
				tileEntity.E();
			}

			// Set tiles/blocks with NBT
			for (Map.Entry<Short, NBTTagCompound> entry : tiles.entrySet()) {
				short coordPair = entry.getKey();
				int x = (coordPair >> 12 & 0xF) + bx;
				int y = coordPair & 0xFF;
				int z = (coordPair >> 8 & 0xF) + bz;

				BlockPosition blockPosition = new BlockPosition(x, y, z);
				TileEntity tileEntity = nmsWorld.getTileEntity(blockPosition);
				if (tileEntity == null)
					continue;

				NBTTagCompound tag = entry.getValue();
				tag.setInt("x", x);
				tag.setInt("y", y);
				tag.setInt("z", z);
				tileEntity.a(tag);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public net.minecraft.server.v1_8_R3.Chunk getCachedChunk(World world) {
		return ((CraftWorld) world).getHandle().chunkProviderServer.getChunkIfLoaded(x, z);
	}

	public void sendChunk() {
		net.minecraft.server.v1_8_R3.Chunk nmsChunk = ((CraftChunk) chunk).getHandle();
		if (nmsChunk == null)
			return;

		try {
			WorldServer worldServer = (WorldServer) nmsChunk.getWorld();
			PlayerChunkMap chunkMap = worldServer.getPlayerChunkMap();

			int x = nmsChunk.locX;
			int z = nmsChunk.locZ;
			if (!chunkMap.isChunkInUse(x, z))
				return;

			Object playerChunk = getOrCreatePlayerChunk.invoke(chunkMap, x, z, true);

			// https://github.com/Attano/Spigot-1.8/blob/master/net/minecraft/server/v1_8_R3/PlayerChunkMap.java#L440
			Method sendPacketToPlayers = playerChunk.getClass().getMethod("a", Packet.class);
			sendPacketToPlayers.setAccessible(true);

			boolean empty = false;
			ChunkSection[] sections = nmsChunk.getSections();

			// Make sure there are no empty sections, and make every section
			// fully lit. Every empty section should be reflected in the bit mask.

			char bitMask = 0xffff;
			for (int i = 0; i < sections.length; ++i) {
				if (sections[i] == null) {
					sections[i] = emptySection;
					// Make bit mask reflect that this section is empty
					bitMask ^= (1 << i);
					empty = true;
				} else {
					sections[i].a(new NibbleArray(fullSkyLight));
				}
			}

			// 0xffff = 65535 = all 16 bits are set
			if (bitMask == 0 || bitMask == 0xFFFF) {
				// First 8 bits
				PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, false, 0xFF00);
				sendPacketToPlayers.invoke(playerChunk, packet);

				// Last 8 bits
				bitMask = 0x00FF;
			}

			// Send packet with "normal" blocks
			PacketPlayOutMapChunk packet = new PacketPlayOutMapChunk(nmsChunk, false, bitMask);
			sendPacketToPlayers.invoke(playerChunk, packet);

			// Send packets with Tile Entities
			nmsChunk.getTileEntities().entrySet().forEach((entry) -> {
				TileEntity tileEntity = entry.getValue();
				PacketPlayOutTileEntityData tileUpdatePacket = (PacketPlayOutTileEntityData) tileEntity
						.getUpdatePacket();

				try {
					sendPacketToPlayers.invoke(playerChunk, tileUpdatePacket);
				} catch (Exception e) {
					e.printStackTrace();
				}

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
		// Executed on startup so it doesnt matter that
		// it's a bit inefficient.
		NibbleArray nibbleArray = new NibbleArray();
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					nibbleArray.a(x, y, z, 15);
				}
			}
		}
		fullSkyLight = nibbleArray.a().clone();

		try {
			chunkMapField = PlayerChunkMap.class.getDeclaredField("d");
			chunkMapField.setAccessible(true);

			fieldNonEmptyBlockCount = ChunkSection.class.getDeclaredField("nonEmptyBlockCount");
			fieldNonEmptyBlockCount.setAccessible(true);

			fieldTickingBlockCount = ChunkSection.class.getDeclaredField("tickingBlockCount");
			fieldTickingBlockCount.setAccessible(true);

			// https://github.com/Attano/Spigot-1.8/blob/master/net/minecraft/server/v1_8_R3/PlayerChunkMap.java#L88
			getOrCreatePlayerChunk = PlayerChunkMap.class.getDeclaredMethod("a", int.class, int.class, boolean.class);
			getOrCreatePlayerChunk.setAccessible(true);
		} catch (Exception e) {
		}
	}
}
