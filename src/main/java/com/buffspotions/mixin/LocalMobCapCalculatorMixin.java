package com.buffspotions.mixin;

import com.buffspotions.effect.CrowdTuning;
import com.buffspotions.mixin.MobCountsAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;

@Mixin(LocalMobCapCalculator.class)
abstract class LocalMobCapCalculatorMixin {
    @Shadow
    private Map playerMobCounts;

    @Shadow
    private List<ServerPlayer> getPlayersNear(ChunkPos pos) {
        throw new AssertionError();
    }

    @Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$tunedCap(MobCategory category, ChunkPos pos, CallbackInfoReturnable<Boolean> callback) {
        if (category == MobCategory.MISC) {
            return;
        }
        for (ServerPlayer player : this.getPlayersNear(pos)) {
            Object counts = this.playerMobCounts.get(player);
            int count = counts == null ? 0 : ((MobCountsAccess) counts).buffsPotions$counts().getOrDefault(category, 0);
            if (count < CrowdTuning.localCap(player, category)) {
                callback.setReturnValue(true);
                return;
            }
        }
        callback.setReturnValue(false);
    }
}
