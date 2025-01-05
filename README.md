## Schematic Pasting
This 1.8-compatible lightweight plugin (inspired by FAWE & WE) provides the fastest schematic pasting available. In testing, 6.5 million blocks were reliably placed in **<100ms** (30ms-100ms). This is approx. **5350% times faster than FAWE** (400% for operations inside ungenerated chunks), but this plugin is, compared to the contrary, also thread safe! This means no more weird bugs, mirroring, etc.

So, to summarize:
  - **~5350% faster than FAWE**
  - **Up to 220,000,000 blocks per second**

However, all this speed also comes with a few drawbacks:

|						| FAWE			| This plugin		|
| --------------------- | ------------- | ----------------- |
| Updates DataPallette	| YES			| YES				|
| Thread-safe			| NO			| YES				|
| Has multithreading	| YES			| ON THE WAY		|
| Lighting support		| YES			| FULL BRIGHT		|
| Lightweight			| NO (~5,600kB)	| YES (~0.5KB)		|
| Custom ChunkProvider	| NO			| YES				|
| Updates whole chunks	| SEMI			| ALWAYS			|

... so with that out of the way, here are the key optimizations implemented by this plugin:  
1. This plugin modifies the DataPalette (the data structure that minecraft uses to save chunks) directly. Since the DataPalette operates near file-level, this gives crazy speeds.

2. In other plugins, every block in the chunk section is often looped on the main thread and then placed. This plugin will *always* paste the entire chunk section. This also adds the opportunity for entire chunks to be able to load from different threads, causing big increases to loading & pasting speeds (however multithreaded support is yet to be implemented as of writing).

3. A custom ChunkProvider gets injected into the WorldServer by this plugin. By doing this, we gain more fine grained control over lighting updates. Thus, we can ensure that chunks loaded because of this plugin doesn't execute any expensive lightning calculations - they're going to be overriden when the schematic gets pasted anyway.