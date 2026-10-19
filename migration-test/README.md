# Migration test (Diagonal Fences 4.2.6 -> this backport)

End-to-end test of `LegacyFenceMigration` on a real world written by the official mod. It was run on
2026-10-19 with these dedicated 1.19.2 Fabric servers (Fabric Loader 0.16.14, flat world, `server.properties`
with `level-type=flat`):

| step | mods |
|------|------|
| old  | `DiagonalFences-v4.2.6-1.19.2-Fabric.jar`, `PuzzlesLib-v4.4.3-1.19.2-Fabric.jar`, `ForgeConfigAPIPort-v4.2.11-1.19.2-Fabric.jar` (all from Modrinth), `fabric-api-0.77.0+1.19.2.jar` |
| new  | `diagonalfences-fabric-26.3.0+1.19.2-backport.jar`, `fabric-api-0.77.0+1.19.2.jar` |
| removed | `fabric-api-0.77.0+1.19.2.jar` only |

Tools (python 3, no dependencies):
* `srv.sh <server dir> <log> <commands>` starts a server, waits for `Done`, sends the console commands, then `save-all flush` and `stop`.
* `palettes.py <world> [regex]` dumps matching chunk palette entries (and container contents) from the region files.
* `count.py <world> [regex]` counts blocks per palette name by decoding the packed section data, and counts vanilla fence blocks still in the legacy format.
* `mkstruct.py <file>` writes a structure template with legacy palette entries (4.2.6 has no easy way to write one from the console, structure blocks powered by redstone don't save to disk).

Steps and the command files used:
1. old server, fresh world: `commands/1-old-place.txt` (oak/spruce/birch(waterlogged)/nether brick/dark oak/crimson fences, straight and diagonal, in chunks 0,0 1,1 2,0 6,6 19,-19, plus a chest with fence items), `commands/1-old-verify.txt` (all 11 `execute if block` tests passed with the old `minecraft:*_fence[...north_east=...]` states). World copied as the pristine 4.2.6 world, palette dump in `results/4.2.6-palette-dump.txt`.
2. new server on a copy: `commands/2-new-first-load.txt`. The log shows the migration notice and
   `converted 63 block(s) (26 palette entries) in 6 chunk(s)` (20 test fences plus 43 fences of a village that the flat world generated next to spawn while 4.2.6 was installed); all 14 migrated-state tests passed with `diagonalfences:minecraft/*_fence[...]`, the chest still holds `minecraft:oak_fence`/`minecraft:birch_fence` items. (The two `Test failed` lines at the end of this run in the log excerpt are two extra assertions of the original run that wrongly expected `/setblock ... minecraft:oak_fence` to create a diagonal fence; commands place exactly the given block, as upstream. The command file has the corrected assertions.)
3. second load: `commands/3-new-second-load.txt`, no chunk conversions (idempotent), states unchanged, `place template dftest:legacy` with the template from `mkstruct.py` (in `world/generated/dftest/structures/legacy.nbt`) placed two converted acacia diagonal fences (`plus 2 other stored block state(s)`).
4. third load: `commands/4-new-third-load.txt`, no migration log at all, all 16 tests passed, diagonal palette entries byte-for-byte identical to before the run.
5. mod removed (Fabric API only), on a copy of the pristine 4.2.6 world and on a copy of the converted world: `commands/5-mod-removed.txt` (tests are alternatives, one of each pair is expected to fail).

`results/block-counts.txt` and `results/server-logs-excerpt.txt` have the measured output.

## "Slabs turned into water / wrong blocks" investigation (numeric block state ids)

Extra tools:
* `stateiddump/` a tiny test-only Fabric mod (build with the root project's Gradle wrapper) that writes the global
  numeric block state id table (`Block.BLOCK_STATE_REGISTRY`) to `stateids.txt` on server start and logs a few ids.
* `client-probe/probe.js` (`npm install` then `node probe.js out.json`) joins `127.0.0.1:25565` as a **vanilla 1.19.2
  client** (mineflayer, block state table from minecraft-data, offline mode) and records the block states it decodes for
  the 512-position test section at x 96..111, z 96..111, y 150 and 152. `probe-compare.py <world> <out.json>` compares
  them with what the server saved.
* `worlddiff.py <worldA> <worldB> [--strip-legacy] [--fence-rename]` compares every block position (all sections,
  all palette sizes), block entities, entities with a `BlockState` (falling blocks), POI and heightmaps of the chunks
  present in both saves.

Setup: default (normal) world, seed `dfence-1192`, 4.2.6 + Puzzles Lib + Forge Config API Port + Fabric API; a plains
village at -400, 864 (7x7 chunks force-loaded) and the spawn area; `commands/6-normal-world-4.2.6-place.txt` places a
section with 512 different block states (a palette of 331 entries, above the 256-entry limit for indirect palettes),
slabs, stairs, a water pool, diagonal fences, a chest and two `NoGravity` falling blocks (a slab and a legacy diagonal
oak fence). 1618 legacy fence blocks in 85 chunks.

Results (`results/`):
* `state-id-tables.txt`: with 4.2.6 the table has 26248 instead of 21448 states, 16853 non-fence vanilla states get a
  different numeric id (everything after `minecraft:oak_fence`). With the backport all 21448 vanilla ids are unchanged,
  the 5120 diagonal fence states are appended at 21448..26567.
* `client-probe.txt`: 4.2.6 server + vanilla client: 356 of 512 positions decoded as wrong blocks (e.g. stone slab ->
  purpur stairs, glass pane -> nether brick fence). Vanilla server + vanilla client: 512/512 correct. Backport server +
  vanilla client: 503/512 correct, the 9 wrong ones are the diagonal fences themselves (ids unknown to vanilla).
* `world-diffs.txt`: removing 4.2.6 (A), keeping it (control C), migrating (B): the only differences are the fence
  diagonal properties and normal game simulation (water flowing out of the test pool, crops, grass, villagers closing
  doors). A compared directly with C differs in 20 positions, all random ticks / villagers. No block, block entity,
  entity, POI or heightmap was changed into something else. R/R2/RM: uninstall mode reverts all 1618 diagonal fences
  (0 differences apart from the rename), a second load changes nothing (3 door positions from villagers), and loading
  the reverted world without the mod changes nothing (0 differences), all fences including the 6 waterlogged ones stay.
