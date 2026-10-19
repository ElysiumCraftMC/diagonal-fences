package fuzs.diagonalblocks.common.impl;

import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockType;
import fuzs.diagonalblocks.common.api.v2.block.type.DiagonalBlockTypes;
import fuzs.diagonalblocks.common.impl.config.DiagonalBlocksConfig;
import fuzs.diagonalblocks.common.impl.handler.DiagonalBlockHandler;
import fuzs.diagonalblocks.common.impl.init.ModRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DiagonalBlocks {
    public static final String MOD_ID = "diagonalblocks";
    public static final String MOD_NAME = "Diagonal Blocks";
    public static final Logger LOGGER = LogManager.getLogger(DiagonalBlocks.MOD_NAME);

    private static boolean constructed;

    public static void onConstructMod() {
        if (constructed) return;
        constructed = true;
        DiagonalBlocksConfig.load();
        ModRegistry.bootstrap();
        if (FabricLoader.getInstance().isDevelopmentEnvironment() && Boolean.getBoolean("diagonalblocks.devTypes")) {
            // upstream registers all types in its own dev environment, only fences are available in this backport
            DiagonalBlockType.register(DiagonalBlockTypes.FENCE);
        }
        registerEventHandlers();
    }

    private static void registerEventHandlers() {
        // server resources (tags) loading is hooked via ReloadableServerResourcesMixin
        UseBlockCallback.EVENT.register(DiagonalBlockHandler::onUseBlock);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
