package com.buffspotions.mixin;

import com.buffspotions.effect.Visitation;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A partial-visitation teleport is not walking, flying, or riding. */
@Mixin(ServerPlayer.class)
abstract class VisitationTravelMixin {
    @Inject(method = "checkMovementStatistics", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$skipVisitationDistance(double dx, double dy, double dz, CallbackInfo callback) {
        if (Visitation.ignoresTravel()) {
            callback.cancel();
        }
    }

    @Inject(method = "checkRidingStatistics", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$skipVisitationRiding(double dx, double dy, double dz, CallbackInfo callback) {
        if (Visitation.ignoresTravel()) {
            callback.cancel();
        }
    }
}
