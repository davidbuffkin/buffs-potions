package com.buffspotions.mixin;

import com.buffspotions.config.BuffsPotionsConfig;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MobEffectInstance.class)
abstract class VanillaRateMixin {
    @Redirect(
            method = "tickServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffect;shouldApplyEffectTickThisTick(II)Z"
            )
    )
    private boolean buffsPotions$rate(MobEffect effect, int tickCount, int amplification) {
        if (effect == MobEffects.WITHER.value()) {
            int step = BuffsPotionsConfig.witherInterval() >> amplification;
            return step > 0 ? tickCount % step == 0 : true;
        }
        int[] steps = steps(effect);
        if (steps == null) {
            return effect.shouldApplyEffectTickThisTick(tickCount, amplification);
        }
        int index = Math.max(0, amplification);
        int step = steps[Math.min(index, steps.length - 1)];
        return step > 0 ? tickCount % step == 0 : true;
    }

    private static int[] steps(MobEffect effect) {
        if (effect == MobEffects.REGENERATION.value()) {
            return BuffsPotionsConfig.regenerationIntervals();
        }
        if (effect == MobEffects.POISON.value()) {
            return BuffsPotionsConfig.poisonIntervals();
        }
        return null;
    }
}
