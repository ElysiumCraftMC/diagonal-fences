package fuzs.diagonalblocks.common.impl.client.handler;

import com.mojang.datafixers.util.Pair;
import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockType;
import fuzs.diagonalblocks.common.api.v2.client.MultiPartTranslator;
import fuzs.diagonalblocks.common.impl.DiagonalBlocks;
import fuzs.diagonalblocks.common.impl.client.resources.model.RotatedVariant;
import fuzs.diagonalblocks.common.impl.data.tags.BuiltInBlacklists;
import net.fabricmc.fabric.api.client.model.ModelProviderContext;
import net.fabricmc.fabric.api.client.model.ModelVariantProvider;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;
import java.io.Reader;
import java.util.*;

/**
 * 1.19.2 backport: upstream uses Puzzles Lib's block state resolver, here Fabric API's (v0)
 * {@link ModelVariantProvider} is used instead to supply the generated multipart models for diagonal blocks.
 * <p>A new instance is created for every resource reload.
 */
public class DiagonalModelHandler implements ModelVariantProvider {
    static final Set<ResourceLocation> INVALID_MODEL_BLOCKS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final String ROTATED_MODEL_PREFIX = "rotated/";

    private final ResourceManager resourceManager;
    private final Map<Block, Optional<UnbakedModel>> blockModels = new IdentityHashMap<>();
    private final Map<Block, Map<ModelResourceLocation, BlockState>> blockStatesByLocation = new IdentityHashMap<>();
    private final Map<Pair<Variant, Direction>, ModelResourceLocation> rotatedModelLocations = new HashMap<>();
    private final Map<ModelResourceLocation, RotatedVariant> rotatedModels = new HashMap<>();

    public DiagonalModelHandler(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Nullable
    @Override
    public UnbakedModel loadModelVariant(ModelResourceLocation modelId, ModelProviderContext context) {
        if (modelId.getNamespace().equals(DiagonalBlocks.MOD_ID) && modelId.getPath()
                .startsWith(ROTATED_MODEL_PREFIX)) {
            return this.rotatedModels.get(modelId);
        }

        if (modelId.getVariant().equals("inventory")) return null;
        ResourceLocation blockId = new ResourceLocation(modelId.getNamespace(), modelId.getPath());
        Block block = Registry.BLOCK.getOptional(blockId).orElse(null);
        if (block == null) return null;
        for (DiagonalBlockType diagonalBlockType : DiagonalBlockType.TYPES) {
            Block baseBlock = diagonalBlockType.getBlockConversions().inverse().get(block);
            if (baseBlock != null) {
                return this.loadDiagonalBlockModel(modelId,
                        context,
                        diagonalBlockType,
                        blockId,
                        block,
                        baseBlock);
            }
        }

        return null;
    }

    @Nullable
    private UnbakedModel loadDiagonalBlockModel(ModelResourceLocation modelId, ModelProviderContext context, DiagonalBlockType diagonalBlockType, ResourceLocation blockId, Block block, Block baseBlock) {
        // allow resource packs to provide a dedicated block state file for the diagonal block, vanilla will load it
        if (this.resourceManager.getResource(getBlockStateFile(blockId)).isPresent()) {
            return null;
        }

        Optional<UnbakedModel> model = this.blockModels.computeIfAbsent(block, (Block block1) -> {
            return Optional.ofNullable(this.createDiagonalMultiPart(diagonalBlockType, block, baseBlock));
        });
        if (model.isPresent()) {
            return model.get();
        } else {
            // fall back to the original model for the corresponding non-diagonal block state
            BlockState blockState = this.blockStatesByLocation.computeIfAbsent(block, (Block block1) -> {
                Map<ModelResourceLocation, BlockState> map = new HashMap<>();
                for (BlockState possibleState : block.getStateDefinition().getPossibleStates()) {
                    map.put(BlockModelShaper.stateToModelLocation(possibleState), possibleState);
                }
                return map;
            }).get(modelId);
            if (blockState != null && MultiPartTranslator.get(diagonalBlockType).allowBaseModelAsFallback()) {
                return context.loadModel(BlockModelShaper.stateToModelLocation(baseBlock.withPropertiesOf(blockState)));
            } else {
                return null;
            }
        }
    }

    @Nullable
    private UnbakedModel createDiagonalMultiPart(DiagonalBlockType diagonalBlockType, Block block, Block baseBlock) {
        ResourceLocation baseBlockId = Registry.BLOCK.getKey(baseBlock);
        BlockModelDefinition.Context context = new BlockModelDefinition.Context();
        // the base block's model definition is parsed using the diagonal block's state definition,
        // this works as the diagonal block contains all properties of the base block
        context.setDefinition(block.getStateDefinition());
        MultiPart multiPart = null;
        for (Resource resource : this.resourceManager.getResourceStack(getBlockStateFile(baseBlockId))) {
            try (Reader reader = resource.openAsReader()) {
                BlockModelDefinition blockModelDefinition = BlockModelDefinition.fromStream(context, reader);
                if (blockModelDefinition.isMultiPart()) {
                    multiPart = blockModelDefinition.getMultiPart();
                }
            } catch (Exception exception) {
                DiagonalBlocks.LOGGER.warn("Failed to load block state definition for '{}' from pack '{}'",
                        baseBlockId,
                        resource.sourcePackId(),
                        exception);
            }
        }

        if (multiPart != null) {
            MultiPartTranslator multiPartTranslator = MultiPartTranslator.get(diagonalBlockType);
            List<Selector> selectors = multiPartTranslator.apply(multiPart.getSelectors(), this::registerRotatedModel);
            return new MultiPart(block.getStateDefinition(), selectors);
        } else {
            reportInvalidBlockModel(baseBlockId, diagonalBlockType);
            return null;
        }
    }

    private ResourceLocation registerRotatedModel(Variant variant, Direction direction) {
        return this.rotatedModelLocations.computeIfAbsent(Pair.of(variant, direction),
                (Pair<Variant, Direction> pair) -> {
                    ModelResourceLocation modelResourceLocation = new ModelResourceLocation(DiagonalBlocks.id(
                            ROTATED_MODEL_PREFIX + this.rotatedModelLocations.size()), direction.getName());
                    this.rotatedModels.put(modelResourceLocation, new RotatedVariant(variant, direction));
                    return modelResourceLocation;
                });
    }

    private static ResourceLocation getBlockStateFile(ResourceLocation blockId) {
        return new ResourceLocation(blockId.getNamespace(), "blockstates/" + blockId.getPath() + ".json");
    }

    public static void reportInvalidBlockModel(ResourceLocation identifier, DiagonalBlockType blockType) {
        if (INVALID_MODEL_BLOCKS.add(identifier)) {
            // don't report built-in blacklisted blocks
            if (!BuiltInBlacklists.TAG_BLACKLISTED_TYPES.getOrDefault(blockType, Collections.emptyList())
                    .contains(identifier.toString())) {
                DiagonalBlocks.LOGGER.warn(
                        "Block '{}' is not using multipart model and should be added to the '{}' block tag. The model will not appear correctly in-game!",
                        identifier,
                        blockType.getBlacklistTagKey().location());
            }
        }
    }
}
