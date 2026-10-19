package fuzs.diagonalfences.common.mixin;

import fuzs.diagonalfences.common.migration.LegacyFenceMigration;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rewrites legacy Diagonal Fences 4.x palette entries on the raw chunk NBT, before vanilla decodes the palettes (which
 * would silently drop the unknown diagonal properties).
 */
@Mixin(ChunkSerializer.class)
abstract class ChunkSerializerMixin {
    @Unique
    private static final ThreadLocal<Boolean> diagonalfences$migrated = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "read", at = @At("HEAD"))
    private static void diagonalfences$migrateLegacyFences(ServerLevel serverLevel, PoiManager poiManager, ChunkPos chunkPos, CompoundTag compoundTag, CallbackInfoReturnable<ProtoChunk> callback) {
        diagonalfences$migrated.set(LegacyFenceMigration.migrateChunk(chunkPos, compoundTag));
    }

    @Inject(method = "read", at = @At("RETURN"))
    private static void diagonalfences$markMigratedChunk(ServerLevel serverLevel, PoiManager poiManager, ChunkPos chunkPos, CompoundTag compoundTag, CallbackInfoReturnable<ProtoChunk> callback) {
        if (diagonalfences$migrated.get()) {
            diagonalfences$migrated.set(Boolean.FALSE);
            // for full chunks this is an ImposterProtoChunk which forwards to the wrapped LevelChunk
            callback.getReturnValue().setUnsaved(true);
        }
    }
}
