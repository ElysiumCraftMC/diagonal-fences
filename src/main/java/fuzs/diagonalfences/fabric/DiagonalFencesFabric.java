package fuzs.diagonalfences.fabric;

import fuzs.diagonalblocks.common.impl.DiagonalBlocks;
import fuzs.diagonalfences.common.DiagonalFences;
import fuzs.diagonalfences.common.migration.LegacyFenceMigration;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class DiagonalFencesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // 1.19.2 backport: Diagonal Blocks is bundled directly instead of being an embedded (jar-in-jar) Puzzles Lib mod
        DiagonalBlocks.onConstructMod();
        DiagonalFences.onConstructMod();
        // migration of worlds saved by Diagonal Fences 4.x, see LegacyFenceMigration
        ServerLifecycleEvents.SERVER_STARTED.register(server -> LegacyFenceMigration.logSummary("server started"));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> LegacyFenceMigration.logSummary("server stopping"));
    }
}
