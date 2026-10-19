package fuzs.diagonalblocks.common.impl.config;

import fuzs.diagonalblocks.common.impl.DiagonalBlocks;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 1.19.2 backport: minimal replacement for upstream's Puzzles Lib / Forge Config API Port based
 * {@code CommonConfig} and {@code ServerConfig}, stored in {@code config/diagonalblocks.properties}.
 */
public final class DiagonalBlocksConfig {
    private static final String DIAGONAL_BLOCKS_BY_DEFAULT = "diagonalBlocksByDefault";
    private static final String SWAP_BLOCKS_BY_INTERACTING = "swapBlocksByInteracting";

    /**
     * When placing blocks use the diagonal variants instead of the non-diagonal blocks.
     */
    public static boolean diagonalBlocksByDefault = true;
    /**
     * Swap between diagonal and non-diagonal block types via sneak+right-clicking with an empty hand.
     */
    public static boolean swapBlocksByInteracting = true;

    private DiagonalBlocksConfig() {
        // NO-OP
    }

    public static void load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve(DiagonalBlocks.MOD_ID + ".properties");
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            } catch (IOException exception) {
                DiagonalBlocks.LOGGER.warn("Failed to read config file {}", path, exception);
            }
        }
        diagonalBlocksByDefault = Boolean.parseBoolean(properties.getProperty(DIAGONAL_BLOCKS_BY_DEFAULT,
                Boolean.toString(diagonalBlocksByDefault)));
        swapBlocksByInteracting = Boolean.parseBoolean(properties.getProperty(SWAP_BLOCKS_BY_INTERACTING,
                Boolean.toString(swapBlocksByInteracting)));
        properties.setProperty(DIAGONAL_BLOCKS_BY_DEFAULT, Boolean.toString(diagonalBlocksByDefault));
        properties.setProperty(SWAP_BLOCKS_BY_INTERACTING, Boolean.toString(swapBlocksByInteracting));
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path)) {
                properties.store(writer,
                        " Diagonal Blocks (Diagonal Fences 1.19.2 backport)\n"
                                + " diagonalBlocksByDefault: When placing blocks use the diagonal variants instead of the non-diagonal blocks. [requires game restart]\n"
                                + " swapBlocksByInteracting: Swap between diagonal and non-diagonal block types via sneak+right-clicking with an empty hand.");
            }
        } catch (IOException exception) {
            DiagonalBlocks.LOGGER.warn("Failed to write config file {}", path, exception);
        }
    }
}
