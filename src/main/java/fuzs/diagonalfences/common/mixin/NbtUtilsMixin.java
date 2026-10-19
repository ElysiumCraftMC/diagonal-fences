package fuzs.diagonalfences.common.mixin;

import fuzs.diagonalfences.common.migration.LegacyFenceMigration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Migrates legacy Diagonal Fences 4.x block states stored outside of chunk palettes (moving pistons, structure
 * templates, falling block entities, ...), which are all read through {@link NbtUtils#readBlockState(CompoundTag)}.
 */
@Mixin(NbtUtils.class)
abstract class NbtUtilsMixin {

    @ModifyVariable(method = "readBlockState", at = @At("HEAD"), argsOnly = true)
    private static CompoundTag diagonalfences$migrateLegacyFence(CompoundTag tag) {
        return LegacyFenceMigration.migrateBlockStateTag(tag);
    }
}
