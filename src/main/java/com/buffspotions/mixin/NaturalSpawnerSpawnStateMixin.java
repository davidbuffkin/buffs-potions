package com.buffspotions.mixin;

import com.buffspotions.effect.CrowdTuning;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LocalMobCapCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.level.NaturalSpawner$SpawnState")
abstract class NaturalSpawnerSpawnStateMixin {
    private static final int SPAWN_CHUNK_AREA = 17 * 17;

    @Shadow
    private int spawnableChunkCount;

    @Shadow
    private Object2IntOpenHashMap<MobCategory> mobCategoryCounts;

    @Shadow
    private LocalMobCapCalculator localMobCapCalculator;

    @Inject(method = "canSpawnForCategoryGlobal", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$globalCap(MobCategory category, CallbackInfoReturnable<Boolean> callback) {
        ChunkMap chunks = ((LocalMobCapChunkAccess) (Object) this.localMobCapCalculator).buffsPotions$chunkMap();
        ServerLevel level = ((ChunkMapLevelAccess) (Object) chunks).buffsPotions$level();
        float scale = CrowdTuning.globalCapScale(level.players(), category);
        if (scale == 1.0F || category.getMaxInstancesPerChunk() <= 0) {
            return;
        }
        int vanilla = category.getMaxInstancesPerChunk() * this.spawnableChunkCount / SPAWN_CHUNK_AREA;
        int cap = Math.max(0, Math.round(vanilla * scale));
        callback.setReturnValue(this.mobCategoryCounts.getInt(category) < cap);
    }
}
