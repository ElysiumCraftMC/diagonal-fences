# Diagonal Fences 26.3.0 -> Minecraft 1.19.2 (Fabric) backport

Unofficial backport of Diagonal Fences 26.3.0 (`v26.3.0-mc26.3.x`, Minecraft 26.3.x) to Minecraft 1.19.2 on Fabric.
Upstream Diagonal Fences 26.x is a thin wrapper around the Diagonal Blocks library (embedded jar-in-jar) and
Puzzles Lib. Neither library exists in this form for 1.19.2, so the relevant parts of Diagonal Blocks 26.3.0 are
bundled directly in this mod and all Puzzles Lib / Forge Config API Port usages are replaced with plain Fabric API,
access wideners and mixins.

Only Fabric API is required at runtime (no Puzzles Lib, no Forge Config API Port).

Build: `./gradlew build` (JDK 17). Mappings: official Mojang mappings (the upstream code uses Mojang names).

## What is kept from 26.3.0
* Separate diagonal fence blocks (`diagonalfences:<namespace>/<path>`) generated for every fence block, including
  modded fences, with the upstream built-in blacklist and the `diagonalfences:non_diagonal_fences` block tag.
* Fence block items place the diagonal variant by default; tags are copied from the base fence to the diagonal fence
  after every tag (re)load on server and client.
* Sneak + right-click with an empty hand swaps between diagonal and vanilla fences.
* `diagonalblocks:never_blocks_diagonal_connections` block tag (contains snow).
* Star shaped outline / collision / particle shapes, diagonal connection rules (wooden vs. nether brick fences,
  blocked by solid neighbours), mob pathfinding fix for small mobs.
* Models are generated at runtime from the base fence's multipart block state model with rotated quads, so resource
  packs and modded fences work without dedicated assets. A dedicated block state file for the diagonal block is still
  respected.
* Config options `diagonalBlocksByDefault` and `swapBlocksByInteracting` (now in `config/diagonalblocks.properties`).

## Dropped / changed
* Diagonal windows and walls types from Diagonal Blocks (not used by Diagonal Fences) are not included.
* No in-game config screen (upstream uses Puzzles Lib's config screen); config is a properties file.
* No data generation; tags are shipped as static JSON.
* The swap interaction uses `LevelEvent.PARTICLES_AND_SOUND_WAX_ON` (1.19.2 has no particles-only wax-on event),
  so a wax sound plays as well.
* Destroy particles are hooked via `ParticleEngine#destroy` (upstream: `ClientLevel#addDestroyBlockEffect`).

## Note: worlds and servers from the official 1.19.2 releases (4.x)
Diagonal Fences 4.x (e.g. 4.2.6) did not register new blocks; it added `north_east`/`south_east`/`south_west`/`north_west`
properties to every vanilla and modded fence. That grows the global numeric block state id table (21448 -> 26248 states
in vanilla 1.19.2) and changes the id of every state registered after `minecraft:oak_fence`. Chunks are sent to clients
as these ids, so when only the server or only the client runs 4.x, the client shows wrong blocks (slabs as waterlogged
stairs, kelp/glowstone as fences, see [diagonal-blocks#32](https://github.com/Fuzss/diagonal-blocks/issues/32),
[#48](https://github.com/Fuzss/diagonal-blocks/issues/48)); the save itself stores block states by name and is not
affected. This backport does not change any vanilla state id (its diagonal fences are separate blocks appended to the
table), but it does not convert fences saved by 4.x: they load as plain fences without diagonal connections. Server and
clients must always run the same mod set.
