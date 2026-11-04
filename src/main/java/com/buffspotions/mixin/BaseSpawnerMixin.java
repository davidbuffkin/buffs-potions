package com.buffspotions.mixin;

import com.buffspotions.effect.CrowdTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BaseSpawner.class)
abstract class BaseSpawnerMixin {
    private static final ThreadLocal<Float> CROWD = new ThreadLocal<>();

    @Shadow
    private int spawnDelay;

    @Shadow
    private int maxNearbyEntities;

    @Shadow
    private int requiredPlayerRange;

    @Shadow
    private boolean isNearPlayer(Level level, BlockPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "serverTick", at = @At("HEAD"))
    private void buffsPotions$tuneDelay(ServerLevel level, BlockPos pos, CallbackInfo callback) {
        CROWD.set(null);
        if (this.spawnDelay <= 0 || !this.isNearPlayer(level, pos)) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        this.spawnDelay = CrowdTuning.adjustSpawnerDelay(
                level, center, this.requiredPlayerRange, this.spawnDelay, level.getRandom()
        );
        float scale = CrowdTuning.spawnerMultiplier(level, center, this.requiredPlayerRange);
        if (scale != 1.0F) {
            CROWD.set(scale);
        }
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void buffsPotions$clearCrowd(ServerLevel level, BlockPos pos, CallbackInfo callback) {
        CROWD.remove();
    }

    @Redirect(
            method = "serverTick",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/world/level/BaseSpawner;maxNearbyEntities:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int buffsPotions$nearbyCap(BaseSpawner spawner) {
        Float scale = CROWD.get();
        if (scale == null) {
            return this.maxNearbyEntities;
        }
        return Math.max(1, Math.round(this.maxNearbyEntities * scale));
    }
}
