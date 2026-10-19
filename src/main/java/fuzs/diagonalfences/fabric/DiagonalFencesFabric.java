package fuzs.diagonalfences.fabric;

import fuzs.diagonalblocks.common.impl.DiagonalBlocks;
import fuzs.diagonalfences.common.DiagonalFences;
import net.fabricmc.api.ModInitializer;

public class DiagonalFencesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // 1.19.2 backport: Diagonal Blocks is bundled directly instead of being an embedded (jar-in-jar) Puzzles Lib mod
        DiagonalBlocks.onConstructMod();
        DiagonalFences.onConstructMod();
    }
}
