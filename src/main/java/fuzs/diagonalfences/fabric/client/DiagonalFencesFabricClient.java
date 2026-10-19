package fuzs.diagonalfences.fabric.client;

import fuzs.diagonalblocks.common.impl.client.DiagonalBlocksClient;
import net.fabricmc.api.ClientModInitializer;

public class DiagonalFencesFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // upstream only registers a Puzzles Lib config screen here, which is not available in this backport
        DiagonalBlocksClient.onConstructMod();
    }
}
