package dk.tandhjulet.map;

import java.util.Collection;
import java.util.Map;

import org.bukkit.World;

import dk.tandhjulet.object.RunnableWithVal;
import dk.tandhjulet.util.MathUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

public class ChunkMap {
	private World world;

	public ChunkMap(World world) {
		this.world = world;
	}

	private volatile SchematicChunk lastWrappedChunk;
	public final Long2ObjectOpenHashMap<SchematicChunk> chunks = new Long2ObjectOpenHashMap<SchematicChunk>() {
		@Override
		public SchematicChunk put(Long key, SchematicChunk value) {
			return put((long) key, value);
		}

		@Override
		public SchematicChunk put(long key, SchematicChunk value) {
			synchronized (this) {
				return super.put(key, value);
			}
		}
	};

	public void forEachAsync(RunnableWithVal<SchematicChunk> chunk) {
		synchronized (chunks) {
			for (Map.Entry<Long, SchematicChunk> entry : chunks.long2ObjectEntrySet()) {
				chunk.run(entry.getValue());
			}
		}
	}

	public Collection<SchematicChunk> getSchematicChunks() {
		synchronized (chunks) {
			return chunks.values();
		}
	}

	public SchematicChunk getSchematicChunk(int cx, int cz) {
		if (cx == Integer.MIN_VALUE && cz == Integer.MIN_VALUE) {
			return lastWrappedChunk;
		}
		long pair = MathUtils.pairInt(cx, cz);
		SchematicChunk chunk = this.chunks.get(pair);

		if (chunk == null) {
			chunk = new SchematicChunk(cx, cz);
			chunk.setWorld(this.world);

			SchematicChunk previous = this.chunks.put(pair, chunk);
			if (previous != null) {
				chunks.put(pair, previous);
				return previous;
			}
			this.chunks.put(pair, chunk);
		}
		return chunk;
	}
}
