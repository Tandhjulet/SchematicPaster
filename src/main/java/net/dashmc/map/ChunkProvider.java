package net.dashmc.map;

import java.lang.reflect.Field;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.chunkio.ChunkIOExecutor;
import org.bukkit.craftbukkit.v1_8_R3.util.LongHash;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.v1_8_R3.BlockSand;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkProviderServer;
import net.minecraft.server.v1_8_R3.ChunkRegionLoader;
import net.minecraft.server.v1_8_R3.CrashReport;
import net.minecraft.server.v1_8_R3.CrashReportSystemDetails;
import net.minecraft.server.v1_8_R3.IChunkLoader;
import net.minecraft.server.v1_8_R3.IChunkProvider;
import net.minecraft.server.v1_8_R3.ReportedException;
import net.minecraft.server.v1_8_R3.World;
import net.minecraft.server.v1_8_R3.WorldServer;

// Large portions of this file are from
// https://github.com/Attano/Spigot-1.8/blob/master/net/minecraft/server/v1_8_R3/ChunkProviderServer.java#L128
public class ChunkProvider extends ChunkProviderServer {
	private static Field chunkLoaderField;
	@Getter
	@Setter
	private static boolean isInjected;

	static {
		try {
			chunkLoaderField = ChunkProviderServer.class.getDeclaredField("chunkLoader");
			chunkLoaderField.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void inject(World world)
			throws IllegalArgumentException, IllegalAccessException, CloneNotSupportedException {
		WorldServer worldServer = (WorldServer) world;
		ChunkProviderServer instance = worldServer.chunkProviderServer;
		ChunkProvider newChunkProvider = ChunkProvider.from(instance);
		worldServer.chunkProviderServer = newChunkProvider;

		setInjected(true);
	}

	public static ChunkProvider from(ChunkProviderServer cps)
			throws IllegalArgumentException, IllegalAccessException, CloneNotSupportedException {
		IChunkLoader chunkLoader = (IChunkLoader) chunkLoaderField.get(cps);
		ChunkProvider newChunkProvider = new ChunkProvider(cps.world, chunkLoader, cps.chunkProvider);
		newChunkProvider.chunks = cps.chunks;
		newChunkProvider.unloadQueue = cps.unloadQueue;
		newChunkProvider.forceChunkLoad = cps.forceChunkLoad;

		return newChunkProvider;
	}

	private ChunkProvider(WorldServer worldserver, IChunkLoader ichunkloader, IChunkProvider ichunkprovider) {
		super(worldserver, ichunkloader, ichunkprovider);
	}

	private IChunkLoader getChunkLoader() {
		try {
			return (IChunkLoader) chunkLoaderField.get(this);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			Bukkit.getLogger().severe("Could not get chunk loader from chunk provider");
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Chunk getChunkAt(int i, int j) {
		return getChunkAt(i, j, null, true);
	}

	@Override
	public Chunk getChunkAt(int i, int j, Runnable runnable) {
		return getChunkAt(i, j, runnable, true);
	}

	public Chunk getChunkAt(int i, int j, Runnable runnable, boolean relight) {
		unloadQueue.remove(i, j);
		Chunk chunk = chunks.get(LongHash.toLong(i, j));
		ChunkRegionLoader loader = null;

		if (getChunkLoader() instanceof ChunkRegionLoader) {
			loader = (ChunkRegionLoader) getChunkLoader();
		}

		// We can only use the queue for already generated chunks
		if (chunk == null && loader != null && loader.chunkExists(world, i, j)) {
			if (runnable != null) {
				ChunkIOExecutor.queueChunkLoad(world, loader, this, i, j, runnable);
				return null;
			} else {
				chunk = ChunkIOExecutor.syncChunkLoad(world, loader, this, i, j);
			}
		} else if (chunk == null) {
			chunk = originalGetChunkAt(i, j, relight);
		}

		// If we didn't load the chunk async and have a callback run it now
		if (runnable != null) {
			runnable.run();
		}

		return chunk;
	}

	@Override
	public Chunk originalGetChunkAt(int i, int j) {
		return originalGetChunkAt(i, j, true);
	}

	public Chunk originalGetChunkAt(int i, int j, boolean relight) {
		this.unloadQueue.remove(i, j);
		Chunk chunk = (Chunk) this.chunks.get(LongHash.toLong(i, j));
		boolean newChunk = false;

		if (chunk == null) {
			chunk = this.loadChunk(i, j);
			if (chunk == null) {
				if (this.chunkProvider == null) {
					chunk = this.emptyChunk;
				} else {
					try {
						chunk = this.chunkProvider.getOrCreateChunk(i, j);
					} catch (Throwable throwable) {
						CrashReport crashreport = CrashReport.a(throwable, "Exception generating new chunk");
						CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Chunk to be generated");

						crashreportsystemdetails.a("Location", (Object) String.format("%d,%d",
								new Object[] { Integer.valueOf(i), Integer.valueOf(j) }));
						crashreportsystemdetails.a("Position hash", (Object) Long.valueOf(LongHash.toLong(i, j))); // CraftBukkit
																													// -
																													// Use
																													// LongHash
						crashreportsystemdetails.a("Generator", (Object) this.chunkProvider.getName());
						throw new ReportedException(crashreport);
					}
				}
				newChunk = true; // CraftBukkit
			}

			this.chunks.put(LongHash.toLong(i, j), chunk);

			chunk.addEntities();

			// CraftBukkit start
			CraftServer server = world.getServer();
			if (server != null) {
				/*
				 * If it's a new world, the first few chunks are generated inside
				 * the World constructor. We can't reliably alter that, so we have
				 * no way of creating a CraftWorld/CraftServer at that point.
				 */
				server.getPluginManager()
						.callEvent(new org.bukkit.event.world.ChunkLoadEvent(chunk.bukkitChunk, newChunk));
			}

			// Update neighbor counts
			for (int x = -2; x < 3; x++) {
				for (int z = -2; z < 3; z++) {
					if (x == 0 && z == 0) {
						continue;
					}

					Chunk neighbor = this.getChunkIfLoaded(chunk.locX + x, chunk.locZ + z);
					if (neighbor != null) {
						neighbor.setNeighborLoaded(-x, -z);
						chunk.setNeighborLoaded(x, z);
					}
				}
			}
			// CraftBukkit end

			// https://github.com/Attano/Spigot-1.8/blob/master/net/minecraft/server/v1_8_R3/Chunk.java#L960
			boolean flag = isChunkLoaded(i, j - 1);
			boolean flag1 = isChunkLoaded(i + 1, j);
			boolean flag2 = isChunkLoaded(i, j + 1);
			boolean flag3 = isChunkLoaded(i - 1, j);
			boolean flag4 = isChunkLoaded(i - 1, j - 1);
			boolean flag5 = isChunkLoaded(i + 1, j + 1);
			boolean flag6 = isChunkLoaded(i - 1, j + 1);
			boolean flag7 = isChunkLoaded(i + 1, j - 1);

			if (flag1 && flag2 && flag5) {
				if (!chunk.isDone()) {
					getChunkAt(this, i, j, relight);
				} else {
					this.a(this, chunk, i, j);
				}
			}

			Chunk chunkToCreate;

			if (flag3 && flag2 && flag6) {
				chunkToCreate = getOrCreateChunk(i - 1, j);
				if (!chunkToCreate.isDone()) {
					getChunkAt(this, i - 1, j, relight);
				} else {
					this.a(this, chunkToCreate, i - 1, j);
				}
			}

			if (flag && flag1 && flag7) {
				chunkToCreate = getOrCreateChunk(i, j - 1);
				if (!chunkToCreate.isDone()) {
					getChunkAt(this, i, j - 1, relight);
				} else {
					this.a(this, chunkToCreate, i, j - 1);
				}
			}

			if (flag4 && flag && flag3) {
				chunkToCreate = getOrCreateChunk(i - 1, j - 1);
				if (!chunkToCreate.isDone()) {
					getChunkAt(this, i - 1, j - 1, relight);
				} else {
					this.a(this, chunkToCreate, i - 1, j - 1);
				}
			}
		}

		return chunk;
	}

	@Override
	public void getChunkAt(IChunkProvider ichunkprovider, int i, int j) {
		getChunkAt(ichunkprovider, i, j, true);
	}

	public void getChunkAt(IChunkProvider ichunkprovider, int i, int j, boolean relight) {
		Chunk chunk = this.getOrCreateChunk(i, j);

		if (!chunk.isDone()) {
			if (relight)
				chunk.n();
			if (this.chunkProvider != null) {
				this.chunkProvider.getChunkAt(ichunkprovider, i, j);

				BlockSand.instaFall = true;
				Random random = new Random();
				random.setSeed(world.getSeed());
				long xRand = random.nextLong() / 2L * 2L + 1L;
				long zRand = random.nextLong() / 2L * 2L + 1L;
				random.setSeed((long) i * xRand + (long) j * zRand ^ world.getSeed());

				org.bukkit.World world = this.world.getWorld();
				if (world != null) {
					this.world.populating = true;
					try {
						for (org.bukkit.generator.BlockPopulator populator : world.getPopulators()) {
							populator.populate(world, random, chunk.bukkitChunk);
						}
					} finally {
						this.world.populating = false;
					}
				}
				BlockSand.instaFall = false;
				this.world.getServer().getPluginManager()
						.callEvent(new org.bukkit.event.world.ChunkPopulateEvent(chunk.bukkitChunk));

				chunk.e();
			}
		}
	}
}
