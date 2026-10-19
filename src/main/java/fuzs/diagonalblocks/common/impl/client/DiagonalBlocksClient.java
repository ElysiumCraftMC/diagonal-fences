package fuzs.diagonalblocks.common.impl.client;

import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockType;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;

import fuzs.diagonalblocks.common.impl.client.handler.DiagonalModelHandler;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;

public class DiagonalBlocksClient {
    private static boolean constructed;

    public static void onConstructMod() {
        if (constructed) return;
        constructed = true;
        // client tags updates are hooked via ClientPacketListenerFabricMixin
        // 1.19.2 backport: diagonal block models are supplied via Fabric API's model loading api,
        // translators for windows and walls are not included as this backport only ships fences
        ModelLoadingRegistry.INSTANCE.registerVariantProvider(DiagonalModelHandler::new);
        // run once all mods have finished client initialization, so render types they set for their own blocks are known
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> copyRenderTypes());
    }

    /**
     * 1.19.2 backport: chunk render types are not part of the block model yet, they are set per block in
     * {@link ItemBlockRenderTypes} (Fabric: {@link BlockRenderLayerMap}). Without this the diagonal blocks always use
     * the solid render type, so cutout textures of the base block (wire meshes, leaves, pickets with holes, ...) show
     * their transparent pixels. Upstream did the same for 1.20.1 and older.
     */
    private static void copyRenderTypes() {
        for (DiagonalBlockType type : DiagonalBlockType.TYPES) {
            type.getBlockConversions().forEach((oldBlock, newBlock) -> {
                RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(oldBlock.defaultBlockState());
                if (renderType != RenderType.solid()) {
                    BlockRenderLayerMap.INSTANCE.putBlock(newBlock, renderType);
                }
            });
        }
    }
}
