package fuzs.diagonalblocks.fabric.mixin.client;

import fuzs.diagonalblocks.common.impl.handler.DiagonalBlockHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.game.ClientboundUpdateTagsPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.19.2 backport: replaces Puzzles Lib's {@code ClientTagsUpdatedCallback}.
 */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerFabricMixin {
    @Shadow
    private RegistryAccess.Frozen registryAccess;

    @Inject(method = "handleUpdateTags", at = @At("TAIL"))
    public void handleUpdateTags(ClientboundUpdateTagsPacket packet, CallbackInfo callback) {
        DiagonalBlockHandler.onTagsUpdated(this.registryAccess);
    }
}
