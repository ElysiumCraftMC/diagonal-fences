package fuzs.diagonalblocks.common.api.v2.block.type;

import fuzs.diagonalblocks.common.api.v2.block.DiagonalFenceBlock;
import fuzs.diagonalblocks.common.impl.data.tags.BuiltInBlacklists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.FenceBlock;

import java.util.List;

public final class DiagonalBlockTypes {
    public static final DiagonalBlockType FENCE = new DiagonalBlockTypeImpl("fences",
            FenceBlock.class,
            (Block block) -> DiagonalFenceBlock::new,
            CrossCollisionBlock.NORTH,
            CrossCollisionBlock.EAST,
            CrossCollisionBlock.WEST,
            CrossCollisionBlock.SOUTH,
            CrossCollisionBlock.WATERLOGGED);

    static {
        BuiltInBlacklists.BUILT_IN_BLACKLISTED_TYPES.forEach((DiagonalBlockType diagonalBlockType, List<String> strings) -> {
            strings.stream().map(ResourceLocation::new).forEach(diagonalBlockType::disableBlockFactory);
        });
    }

    private DiagonalBlockTypes() {
        // NO-OP
    }
}
