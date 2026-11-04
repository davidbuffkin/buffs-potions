package com.buffspotions.mixin;

import com.buffspotions.effect.CrowdTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
    @Inject(method = "isRightDistanceToPlayerAndSpawnPoint", at = @At("HEAD"), cancellable = true)
    private static void buffsPotions$safeZone(
            ServerLevel level,
            ChunkAccess chunk,
            BlockPos.MutableBlockPos pos,
            double nearestPlayerDistanceSqr,
            CallbackInfoReturnable<Boolean> callback
    ) {
        Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, -1.0, false);
        if (!(nearest instanceof ServerPlayer player) || !CrowdTuning.changesSafeZone(player)) {
            return;
        }
        double safe = CrowdTuning.safeDistance(player);
        if (player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= safe * safe) {
            callback.setReturnValue(false);
            return;
        }
        LevelData.RespawnData respawn = level.getRespawnData();
        if (respawn.dimension() == level.dimension()
                && respawn.pos().closerToCenterThan(new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5), 24.0)) {
            callback.setReturnValue(false);
            return;
        }
        ChunkPos chunkPos = ChunkPos.containing(pos);
        callback.setReturnValue(chunkPos.equals(chunk.getPos()) || level.canSpawnEntitiesInChunk(chunkPos));
    }
}
