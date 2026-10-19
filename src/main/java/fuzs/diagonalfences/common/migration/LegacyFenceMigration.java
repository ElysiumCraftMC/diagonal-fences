package fuzs.diagonalfences.common.migration;

import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockType;
import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockTypes;
import fuzs.diagonalfences.common.DiagonalFences;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Migrates block states written by the official Diagonal Fences 4.x releases for Minecraft 1.19.2 (and older).
 * <p>
 * Those versions did not register separate blocks. Instead a mixin into {@link net.minecraft.world.level.block.FenceBlock}
 * appended four extra boolean properties ({@code north_east}, {@code south_east}, {@code south_west},
 * {@code north_west}) to the state definition of <em>every</em> vanilla and modded fence. A chunk palette entry therefore
 * looks like
 * <pre>{Name:"minecraft:oak_fence",Properties:{east:"false",north:"false",north_east:"true",north_west:"false",
 * south:"false",south_east:"false",south_west:"false",waterlogged:"false",west:"false"}}</pre>
 * Vanilla (and this backport, which leaves the vanilla fence state definition untouched like upstream Diagonal Fences
 * 26.x) silently ignores unknown properties when decoding such an entry, so all diagonal connections would be lost.
 * <p>
 * This class rewrites such entries <em>on the raw NBT, before it is decoded</em> to the diagonal block registered by
 * this mod for the same base fence, e.g. {@code diagonalfences:minecraft/oak_fence}, keeping all properties (the
 * diagonal block has exactly the same property names). Entries without the legacy diagonal properties, entries that
 * already name a diagonal block, and entries for unknown blocks or blocks without a diagonal variant are left alone, so
 * the migration is idempotent and a no-op on vanilla / already converted data.
 */
public final class LegacyFenceMigration {
    private static final String[] LEGACY_DIAGONAL_PROPERTIES = {"north_east", "south_east", "south_west", "north_west"};

    private static final AtomicLong CHUNKS = new AtomicLong();
    private static final AtomicLong BLOCKS = new AtomicLong();
    private static final AtomicLong PALETTE_ENTRIES = new AtomicLong();
    private static final AtomicLong OTHER_STATES = new AtomicLong();
    private static final AtomicBoolean ANNOUNCED = new AtomicBoolean();
    private static final AtomicLong REPORTED_CHUNKS = new AtomicLong();
    private static final AtomicLong REPORTED_OTHER = new AtomicLong();

    private LegacyFenceMigration() {
    }

    /**
     * Returns the id of the diagonal block that should replace a legacy palette / block state entry, or {@code null}
     * if the entry is not a legacy Diagonal Fences 4.x state.
     */
    @Nullable
    private static String getReplacementName(CompoundTag stateTag) {
        if (!stateTag.contains("Name", Tag.TAG_STRING) || !stateTag.contains("Properties", Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag properties = stateTag.getCompound("Properties");
        boolean legacy = false;
        for (String key : LEGACY_DIAGONAL_PROPERTIES) {
            if (properties.contains(key, Tag.TAG_STRING)) {
                legacy = true;
                break;
            }
        }
        if (!legacy) return null;
        ResourceLocation id = ResourceLocation.tryParse(stateTag.getString("Name"));
        if (id == null) return null;
        Block block = Registry.BLOCK.getOptional(id).orElse(null);
        if (block == null) return null;
        DiagonalBlockType type = DiagonalBlockTypes.FENCE;
        Block diagonalBlock = type.getBlockConversions().get(block);
        if (diagonalBlock == null) return null;
        // like upstream, blacklisted fences stay non-diagonal (vanilla then drops the unused legacy properties)
        if (block.builtInRegistryHolder().is(type.getBlacklistTagKey())) {
            return null;
        }
        return Registry.BLOCK.getKey(diagonalBlock).toString();
    }

    /**
     * Migrates a single block state compound as read by {@link net.minecraft.nbt.NbtUtils#readBlockState(CompoundTag)}
     * (moving pistons, structure templates, falling blocks, ...).
     *
     * @return a migrated copy, or the original tag if nothing needed to change
     */
    public static CompoundTag migrateBlockStateTag(CompoundTag stateTag) {
        String replacement = getReplacementName(stateTag);
        if (replacement == null) return stateTag;
        CompoundTag copy = stateTag.copy();
        copy.putString("Name", replacement);
        OTHER_STATES.incrementAndGet();
        return copy;
    }

    /**
     * Migrates all block state palettes of a serialized chunk in place.
     *
     * @return {@code true} if anything was changed, the chunk must then be marked as unsaved
     */
    public static boolean migrateChunk(ChunkPos chunkPos, CompoundTag chunkTag) {
        if (!chunkTag.contains("sections", Tag.TAG_LIST)) return false;
        ListTag sections = chunkTag.getList("sections", Tag.TAG_COMPOUND);
        long blocks = 0;
        int entries = 0;
        for (int i = 0; i < sections.size(); i++) {
            CompoundTag section = sections.getCompound(i);
            if (!section.contains("block_states", Tag.TAG_COMPOUND)) continue;
            CompoundTag blockStates = section.getCompound("block_states");
            ListTag palette = blockStates.getList("palette", Tag.TAG_COMPOUND);
            boolean[] converted = null;
            for (int j = 0; j < palette.size(); j++) {
                CompoundTag entry = palette.getCompound(j);
                String replacement = getReplacementName(entry);
                if (replacement != null) {
                    entry.putString("Name", replacement);
                    if (converted == null) converted = new boolean[palette.size()];
                    converted[j] = true;
                    entries++;
                }
            }
            if (converted != null) {
                blocks += countBlocks(converted, blockStates.contains("data", Tag.TAG_LONG_ARRAY) ? blockStates.getLongArray("data") : null);
            }
        }
        if (entries == 0) return false;
        long chunks = CHUNKS.incrementAndGet();
        BLOCKS.addAndGet(blocks);
        PALETTE_ENTRIES.addAndGet(entries);
        if (ANNOUNCED.compareAndSet(false, true)) {
            DiagonalFences.LOGGER.info("Found fences saved by Diagonal Fences 4.x (legacy vanilla-fence block states with diagonal properties), converting them to the new diagonal fence blocks (first chunk: {})", chunkPos);
        }
        DiagonalFences.LOGGER.debug("Converted {} legacy diagonal fence block(s) ({} palette entries) in chunk {}", blocks, entries, chunkPos);
        return true;
    }

    /**
     * Counts how many of the 4096 section positions reference a converted palette index, mirroring
     * {@link net.minecraft.world.level.chunk.PalettedContainer} storage (1.16+ layout, entries do not span longs).
     */
    private static long countBlocks(boolean[] converted, long @Nullable [] data) {
        int size = converted.length;
        if (size == 1 || data == null || data.length == 0) {
            return converted[0] ? 4096 : 0;
        }
        int bits = Math.max(4, 32 - Integer.numberOfLeadingZeros(size - 1));
        int perLong = 64 / bits;
        long mask = (1L << bits) - 1L;
        long count = 0;
        for (int index = 0; index < 4096; index++) {
            int longIndex = index / perLong;
            if (longIndex >= data.length) break;
            int value = (int) ((data[longIndex] >>> ((index - longIndex * perLong) * bits)) & mask);
            if (value < size && converted[value]) count++;
        }
        return count;
    }

    /**
     * Logs a summary of everything converted since the last summary, called once the server has started (covers the
     * spawn area) and when it stops (covers the whole session).
     */
    public static void logSummary(String when) {
        long chunks = CHUNKS.get();
        long other = OTHER_STATES.get();
        if (chunks == REPORTED_CHUNKS.get() && other == REPORTED_OTHER.get()) return;
        REPORTED_CHUNKS.set(chunks);
        REPORTED_OTHER.set(other);
        DiagonalFences.LOGGER.info("Diagonal Fences 4.x migration summary ({}): converted {} block(s) ({} palette entries) in {} chunk(s), plus {} other stored block state(s) (pistons, structures, ...); converted chunks are marked for saving and will be written in the new format",
                when, BLOCKS.get(), PALETTE_ENTRIES.get(), chunks, other);
    }
}
