package fuzs.diagonalblocks.common.impl.handler;

import net.minecraft.core.Holder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 1.19.2 backport: minimal replacement for Puzzles Lib's {@code BlockConversionHelper}, implemented via access
 * widener.
 */
public final class BlockConversionHelper {

    private BlockConversionHelper() {
        // NO-OP
    }

    /**
     * Make {@link Block#asItem()} for the block return the item.
     */
    public static void setItemForBlock(Block block, Item item) {
        Item.BY_BLOCK.put(block, item);
        block.item = item;
    }

    /**
     * Make the block item place the block.
     */
    public static void setBlockForItem(BlockItem blockItem, Block block) {
        blockItem.block = block;
    }

    /**
     * Copy all tags bound to the original block to the other block.
     */
    public static void copyBoundTags(Block from, Block to) {
        Holder.Reference<Block> fromHolder = from.builtInRegistryHolder();
        Holder.Reference<Block> toHolder = to.builtInRegistryHolder();
        List<TagKey<Block>> tags = fromHolder.tags().toList();
        toHolder.bindTags(tags);
    }
}
