package fuzs.diagonalblocks.common.mixin.accessor;

import it.unimi.dsi.fastutil.doubles.DoubleList;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * 1.19.2 backport: {@link VoxelShape#getCoords(Direction.Axis)} is still protected.
 */
@Mixin(VoxelShape.class)
public interface VoxelShapeAccessor {

    @Invoker("getCoords")
    DoubleList diagonalblocks$callGetCoords(Direction.Axis axis);
}
