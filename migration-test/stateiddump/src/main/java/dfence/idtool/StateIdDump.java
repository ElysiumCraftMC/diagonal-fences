package dfence.idtool;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/** Test helper: dumps the global numeric block state id table (Block.BLOCK_STATE_REGISTRY) and block raw ids. */
public class StateIdDump implements ModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("StateIdDump");

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of("stateids.txt")))) {
                int i = 0;
                for (BlockState state : Block.BLOCK_STATE_REGISTRY) {
                    out.println(Block.getId(state) + "\t" + Registry.BLOCK.getId(state.getBlock()) + "\t" + state);
                    i++;
                }
                LOGGER.info("StateIdDump: {} block states, {} blocks", i, Registry.BLOCK.size());
            } catch (Exception e) {
                LOGGER.error("dump failed", e);
            }
            LOGGER.info("StateIdDump: Block.getId(minecraft:oak_fence default) = {}", Block.getId(Blocks.OAK_FENCE.defaultBlockState()));
            LOGGER.info("StateIdDump: Block.getId(minecraft:water[level=0]) = {}", Block.getId(Blocks.WATER.defaultBlockState()));
            LOGGER.info("StateIdDump: Block.getId(minecraft:stone_slab[type=bottom,waterlogged=false]) = {}", Block.getId(Blocks.STONE_SLAB.defaultBlockState()));
            LOGGER.info("StateIdDump: Block.getId(minecraft:oak_slab default) = {}", Block.getId(Blocks.OAK_SLAB.defaultBlockState()));
            LOGGER.info("StateIdDump: Block.getId(minecraft:oak_stairs default) = {}", Block.getId(Blocks.OAK_STAIRS.defaultBlockState()));
            LOGGER.info("StateIdDump: Block.getId(minecraft:stone) = {}", Block.getId(Blocks.STONE.defaultBlockState()));
        });
    }
}
