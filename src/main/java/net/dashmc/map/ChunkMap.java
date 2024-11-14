package net.dashmc.map;

import java.util.Map;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.dashmc.object.RunnableWithVal;
import net.dashmc.util.MathUtils;

public class ChunkMap {
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

	public void forEach(RunnableWithVal<SchematicChunk> chunk) {
		synchronized (chunks) {
			for (Map.Entry<Long, SchematicChunk> entry : chunks.long2ObjectEntrySet()) {
				chunk.run(entry.getValue());
			}
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
