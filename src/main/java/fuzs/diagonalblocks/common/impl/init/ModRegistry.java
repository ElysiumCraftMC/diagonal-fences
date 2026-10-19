package fuzs.diagonalblocks.common.impl.init;

import fuzs.diagonalblocks.common.impl.DiagonalBlocks;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModRegistry {
    public static final TagKey<Block> NEVER_BLOCKS_DIAGONAL_CONNECTIONS_BLOCK_TAG = TagKey.create(Registry.BLOCK_REGISTRY,
            DiagonalBlocks.id("never_blocks_diagonal_connections"));

    public static void bootstrap() {
        // NO-OP
    }
}
