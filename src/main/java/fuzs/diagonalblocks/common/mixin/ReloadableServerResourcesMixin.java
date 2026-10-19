package fuzs.diagonalblocks.common.mixin;

import fuzs.diagonalblocks.common.impl.handler.DiagonalBlockHandler;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.19.2 backport: replaces Puzzles Lib's {@code ServerResourcesLoadCallback}, runs after tags have been bound.
 */
@Mixin(ReloadableServerResources.class)
abstract class ReloadableServerResourcesMixin {

    @Inject(method = "updateRegistryTags(Lnet/minecraft/core/RegistryAccess;)V", at = @At("TAIL"))
    public void updateRegistryTags(RegistryAccess registryAccess, CallbackInfo callback) {
        DiagonalBlockHandler.onServerResourcesLoad(registryAccess);
    }
}
