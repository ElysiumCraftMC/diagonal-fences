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
   `converted 63 block(s) (26 palette entries) in 6 chunk(s)` (20 test fences plus 43 fences of a village that the flat world generated next to spawn while 4.2.6 was installed); all 14 state tests passed with `diagonalfences:minecraft/*_fence[...]`, the chest still holds `minecraft:oak_fence`/`minecraft:birch_fence` items.
3. second load: `commands/3-new-second-load.txt`, no chunk conversions (idempotent), states unchanged, `place template dftest:legacy` with the template from `mkstruct.py` (in `world/generated/dftest/structures/legacy.nbt`) placed two converted acacia diagonal fences (`plus 2 other stored block state(s)`).
4. third load: `commands/4-new-third-load.txt`, no migration log at all, all 16 tests passed, diagonal palette entries byte-for-byte identical to before the run.
5. mod removed (Fabric API only), on a copy of the pristine 4.2.6 world and on a copy of the converted world: `commands/5-mod-removed.txt` (tests are alternatives, one of each pair is expected to fail).

`results/block-counts.txt` and `results/server-logs-excerpt.txt` have the measured output.
