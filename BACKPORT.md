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

## Migrating worlds from the official 1.19.2 releases (Diagonal Fences 4.x)

### How 4.2.6 stored diagonal fences
Diagonal Fences 4.2.6 (`v4.2.6-1.19.2-Fabric`, the last official 1.19.2 release) and earlier did **not** register any
blocks or items. `FenceBlockMixin` injects into `FenceBlock#createBlockStateDefinition` and appends four boolean
properties (`DiagonalBlock.NORTH_EAST/SOUTH_EAST/SOUTH_WEST/NORTH_WEST`) to the state definition of **every**
`FenceBlock` (vanilla and modded, including blacklisted ones, which just never set them). So chunk palettes contain
vanilla ids with extra properties, for example:

```
{Name:"minecraft:oak_fence",Properties:{east:"false",north:"false",north_east:"true",north_west:"false",
 south:"false",south_east:"false",south_west:"false",waterlogged:"false",west:"false"}}
```

The same applies to every other place a block state is stored (structure templates, moving pistons, ...). Item ids are
untouched (`minecraft:oak_fence`), Puzzles Lib does not store anything in the world for this mod, and
`IronBarsBlockMixin` is shipped in the 4.2.6 jar but not listed in the mixin config, so panes/bars are not affected.
`results/4.2.6-palette-dump.txt` in `migration-test/` is a dump of a real world written by 4.2.6.

Vanilla decodes palette entries with a `MapCodec` per known property and `NbtUtils#readBlockState` skips unknown
properties, so without migration all diagonal connections are silently dropped when the world is loaded by vanilla or by
this backport (which, like upstream 26.x, does not change the vanilla fence state definition).

### What the backport does
`fuzs.diagonalfences.common.migration.LegacyFenceMigration`, hooked by `diagonalfences.migration.mixins.json`:
* `ChunkSerializer#read` (HEAD, before vanilla decodes anything): every `sections[].block_states.palette` entry whose
  `Name` is a fence with a diagonal variant and whose `Properties` contain any of `north_east`, `south_east`,
  `south_west`, `north_west` is renamed to the diagonal block (`minecraft:oak_fence` -> `diagonalfences:minecraft/oak_fence`).
  Properties are kept as they are; the diagonal block has exactly the same property names, so straight connections,
  diagonal connections and `waterlogged` are preserved. Only the palette changes, the packed index data stays valid.
  The chunk is marked unsaved so it is written back in the new format on the next save.
* `NbtUtils#readBlockState` (HEAD): the same rename for single stored states (moving pistons, structure templates,
  falling blocks, ...).
* Left untouched: entries without the legacy properties (vanilla data), entries that already name a diagonal block
  (already converted), unknown blocks, fences without a diagonal variant and fences in `diagonalfences:non_diagonal_fences`
  (those stay vanilla, the unused legacy properties are dropped by vanilla as before). Running it again is a no-op.
* Logging: one notice when the first legacy chunk is found, then a summary when the server has started and when it
  stops, e.g. `Diagonal Fences 4.x migration summary (server started): converted 63 block(s) (26 palette entries) in 6
  chunk(s), plus 0 other stored block state(s) ...`. Per-chunk details are logged at debug level.
* Items: nothing to do (4.x used vanilla item ids; fence items place the diagonal block in this backport).

### Tested
See `migration-test/README.md`: a world created on a dedicated server with the official 4.2.6 jar + Puzzles Lib 4.4.3 +
Forge Config API Port 4.2.11 (straight, diagonal and waterlogged fences of six types in five chunks, a chest with fence
items, plus a village) was loaded with only this backport + Fabric API: all 63 legacy fence blocks were converted with
their connections and waterlogging intact, the second and third loads converted nothing and left the data unchanged,
and a structure template with legacy states was placed as diagonal fences.

### Limitations
* Chunks are converted when they are loaded, not all at once; chunks never loaded again keep the old format (they are
  converted whenever they are loaded later). `--forceUpgrade` / "Optimize world" do not go through `ChunkSerializer#read`
  and do not convert anything.
* Only data read on the logical server is migrated (that is where all world data lives); data read through other paths
  (e.g. external tools, other mods' custom storage) is not.
* Fences in the legacy format whose base fence is not present (mod removed) are not touched.
* Removing the mod afterwards: like upstream 26.x, the diagonal fences are separate registered blocks, so a world loaded
  without this mod replaces every `diagonalfences:*` block with **air** (waterlogged ones too, no water is left); vanilla
  fences and fence items stay. For comparison, removing 4.2.6 keeps the fences as vanilla fences and only loses the
  diagonal connections. To remove the mod, first turn diagonal fences back into vanilla ones (sneak + right-click with an
  empty hand, per block). Both cases were tested, see `migration-test/results/block-counts.txt`.
