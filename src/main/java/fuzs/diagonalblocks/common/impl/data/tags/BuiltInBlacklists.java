package fuzs.diagonalblocks.common.impl.data.tags;

import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockType;
import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockTypes;

import java.util.List;
import java.util.Map;

/**
 * Built-in blacklists from upstream's {@code ModBlockTagsProvider}, limited to fences.
 * <p>1.19.2 backport: data generation via Puzzles Lib is not available, the tag-based blacklists are shipped as static
 * JSON files in {@code data/diagonalfences/tags/blocks/non_diagonal_fences.json} and
 * {@code data/diagonalblocks/tags/blocks/never_blocks_diagonal_connections.json} instead.
 */
public final class BuiltInBlacklists {
    private static final List<String> BUILT_IN_BLACKLISTED_FENCES = List.of("betternether:nether_reed_fence",
            "betterend:pythadendron_fence",
            "betternether:rubeus_fence",
            "betterend:dragon_tree_fence",
            "betterend:lucernia_fence",
            "betternether:mushroom_fir_fence",
            "betternether:stalagnate_fence",
            "betterend:tenanea_fence",
            "betternether:nether_sakura_fence",
            "betterend:umbrella_tree_fence",
            "betterend:mossy_glowshroom_fence",
            "betterend:helix_tree_fence",
            "betterend:lacugrove_fence",
            "betterend:jellyshroom_fence",
            "betternether:nether_mushroom_fence",
            "betternether:wart_fence",
            "betterend:end_lotus_fence",
            "betternether:anchor_tree_fence",
            "betternether:willow_fence",
            "domum_ornamentum:vanilla_fence_compat");
    private static final List<String> TAG_BLACKLISTED_FENCES = List.of("immersiveengineering:treated_fence",
            "immersiveengineering:steel_fence");
    public static final Map<DiagonalBlockType, List<String>> BUILT_IN_BLACKLISTED_TYPES = Map.of(DiagonalBlockTypes.FENCE,
            BUILT_IN_BLACKLISTED_FENCES);
    public static final Map<DiagonalBlockType, List<String>> TAG_BLACKLISTED_TYPES = Map.of(DiagonalBlockTypes.FENCE,
            TAG_BLACKLISTED_FENCES);

    private BuiltInBlacklists() {
        // NO-OP
    }
}
