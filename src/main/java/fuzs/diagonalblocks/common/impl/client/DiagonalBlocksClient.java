package fuzs.diagonalblocks.common.impl.client;

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
    }
}
