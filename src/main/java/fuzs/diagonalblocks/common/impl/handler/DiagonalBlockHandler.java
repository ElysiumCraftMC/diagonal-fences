package fuzs.diagonalblocks.common.impl.handler;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableList;
import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockType;
import fuzs.diagonalblocks.common.impl.DiagonalBlocks;
import fuzs.diagonalblocks.common.impl.config.DiagonalBlocksConfig;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Map;

public class DiagonalBlockHandler {

    /**
     * 1.19.2 backport: replaces Puzzles Lib's {@code RegistryEntryAddedCallback}, which also replays already registered
     * entries.
     */
    public static void registerBlockAddedListener(DiagonalBlockType type) {
        // copy, as we are going to register new entries while iterating
        List<Map.Entry<ResourceKey<Block>, Block>> entries = ImmutableList.copyOf(Registry.BLOCK.entrySet());
        for (Map.Entry<ResourceKey<Block>, Block> entry : entries) {
            onBlockAdded(type, entry.getKey().location(), entry.getValue());
        }
        RegistryEntryAddedCallback.event(Registry.BLOCK).register((int rawId, ResourceLocation id, Block block) -> {
            onBlockAdded(type, id, block);
        });
    }

    private static void onBlockAdded(DiagonalBlockType type, ResourceLocation id, Block entry) {
        if (type.isTarget(id, entry)) {
            ResourceLocation identifier = type.id(id.getNamespace() + "/" + id.getPath());
            if (!Registry.BLOCK.containsKey(identifier)) {
                Registry.register(Registry.BLOCK, identifier, type.makeDiagonalBlock(id, entry));
            }
        }
    }

    public static InteractionResult onUseBlock(Player player, Level level, InteractionHand interactionHand, BlockHitResult hitResult) {
        if (!DiagonalBlocksConfig.swapBlocksByInteracting || player.isSpectator()) {
            return InteractionResult.PASS;
        }

        // allow for toggling between diagonal and non-diagonal block variants via shift+right-clicking with empty hand
        if (player.isSecondaryUseActive() && player.getItemInHand(interactionHand).isEmpty()) {
            BlockPos blockPos = hitResult.getBlockPos();
            BlockState blockState = level.getBlockState(blockPos);
            for (DiagonalBlockType type : DiagonalBlockType.TYPES) {
                if (!type.supportsOriginalBlockState() && !blockState.is(type.getBlacklistTagKey())) {
                    Block newBlock;
                    Block block = blockState.getBlock();
                    if (type.getBlockConversions().containsKey(block)) {
                        newBlock = type.getBlockConversions().get(block);
                    } else if (type.getBlockConversions().containsValue(block)) {
                        newBlock = type.getBlockConversions().inverse().get(block);
                    } else {
                        newBlock = null;
                    }

                    if (newBlock != null) {
                        BlockState newBlockState = newBlock.withPropertiesOf(blockState);
                        newBlockState = Block.updateFromNeighbourShapes(newBlockState, level, blockPos);
                        level.setBlock(blockPos, newBlockState, 3);
                        level.neighborChanged(blockPos, newBlock, blockPos);
                        level.gameEvent(GameEvent.BLOCK_CHANGE, blockPos, GameEvent.Context.of(player, newBlockState));
                        level.levelEvent(player, LevelEvent.PARTICLES_AND_SOUND_WAX_ON, blockPos, 0);
                        return InteractionResult.sidedSuccess(level.isClientSide);
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }

    public static void onServerResourcesLoad(RegistryAccess registries) {
        onTagsUpdated(registries);
    }

    public static void onTagsUpdated(RegistryAccess registries) {
        if (DiagonalBlocksConfig.diagonalBlocksByDefault && !DiagonalBlocksConfig.revertToVanillaOnLoad) {
            for (Map.Entry<ResourceKey<Item>, Item> entry : Registry.ITEM.entrySet()) {
                if (entry.getValue() instanceof BlockItem blockItem) {
                    Block block = blockItem.getBlock();
                    setItemForBlock(entry.getKey().location(), blockItem, block);
                    setBlockForItem(blockItem, block);
                }
            }
        }

        for (DiagonalBlockType type : DiagonalBlockType.TYPES) {
            type.getBlockConversions().forEach(BlockConversionHelper::copyBoundTags);
        }
    }

    private static void setItemForBlock(ResourceLocation identifier, BlockItem blockItem, Block block) {
        for (DiagonalBlockType type : DiagonalBlockType.TYPES) {
            // item id should be fine to use for block items
            Block diagonalBlock = type.getBlockConversions().get(block);
            if (diagonalBlock != null && type.isTarget(identifier, block)) {
                BlockConversionHelper.setItemForBlock(diagonalBlock, blockItem);
                break;
            }
        }
    }

    private static void setBlockForItem(BlockItem blockItem, Block block) {
        for (DiagonalBlockType type : DiagonalBlockType.TYPES) {
            BiMap<Block, Block> conversions = type.getBlockConversions();
            Block baseBlock;
            Block diagonalBlock = conversions.get(block);
            if (diagonalBlock != null) {
                baseBlock = block;
            } else {
                baseBlock = conversions.inverse().get(block);
                if (baseBlock != null) {
                    diagonalBlock = block;
                } else {
                    continue;
                }
            }

            if (baseBlock.builtInRegistryHolder().is(type.getBlacklistTagKey())) {
                BlockConversionHelper.setBlockForItem(blockItem, baseBlock);
            } else {
                BlockConversionHelper.setBlockForItem(blockItem, diagonalBlock);
            }

            break;
        }
    }
}
