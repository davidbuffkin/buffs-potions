package com.buffspotions.mixin;

import com.buffspotions.effect.CrowdTuning;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AvoidEntityGoal.class)
abstract class AvoidEntityGoalMixin {
    @Shadow
    protected LivingEntity toAvoid;

    @Inject(method = "canUse", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$ignoreQuiet(CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() && this.toAvoid instanceof Player player && CrowdTuning.calms(player)) {
            this.toAvoid = null;
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$stopFleeingQuiet(CallbackInfoReturnable<Boolean> callback) {
        if (this.toAvoid instanceof Player player && CrowdTuning.calms(player)) {
            callback.setReturnValue(false);
        }
    }
}
