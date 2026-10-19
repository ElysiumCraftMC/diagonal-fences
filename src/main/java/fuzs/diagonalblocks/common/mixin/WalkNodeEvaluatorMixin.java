package fuzs.diagonalblocks.common.mixin;

import fuzs.diagonalblocks.common.api.v2.util.EightWayDirection;
import fuzs.diagonalblocks.common.api.v2.block.StarCollisionBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import org.jetbrains.annotations.Nullable;

@Mixin(WalkNodeEvaluator.class)
abstract class WalkNodeEvaluatorMixin extends NodeEvaluator {

    // 1.19.2: the stored boolean is the "small mob may squeeze between two fences diagonally" flag
    @ModifyVariable(method = "isDiagonalValid", at = @At("STORE"))
    protected boolean isDiagonalValid(boolean isDiagonalValid, Node root, @Nullable Node xNode, @Nullable Node zNode, @Nullable Node diagonalNode) {
        // small animals (width less than 0.5, like chicken and rabbits) can walk through unconnected fences diagonally, but we add connections in there
        // so check if a diagonal connection is currently present and if so prevent the animal from trying to walk through
        if (isDiagonalValid && xNode != null && zNode != null && xNode.y == zNode.y) {
            for (EightWayDirection direction : EightWayDirection.getIntercardinalDirections()) {
                if (direction.getX() == xNode.x - zNode.x && direction.getZ() == xNode.z - zNode.z) {
                    BlockState state = this.level.getBlockState(new BlockPos(zNode.x, zNode.y, zNode.z));
                    BooleanProperty property = StarCollisionBlock.PROPERTY_BY_DIRECTION.get(direction);
                    if (state.hasProperty(property) && state.getValue(property)) {
                        state = this.level.getBlockState(new BlockPos(xNode.x, xNode.y, xNode.z));
                        property = StarCollisionBlock.PROPERTY_BY_DIRECTION.get(direction.getOpposite());
                        if (state.hasProperty(property) && state.getValue(property)) {
                            return false;
                        }
                    }
                }
            }
        }

        return isDiagonalValid;
    }
}
