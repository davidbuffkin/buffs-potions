package com.buffspotions.effect;

import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/** How many times Air Step can hop before the next landing. */
public final class AirStep {
    private AirStep() {
    }

    public static int limit(int amplifier) {
        int level = Math.max(0, Math.min(amplifier, 3));
        return BuffsPotionsConfig.airStepJumps() << level;
    }

    public static int used(LivingEntity entity) {
        return entity.getData(ModAttachments.AIR_JUMPS);
    }

    public static boolean allow(LivingEntity entity) {
        if (entity.onGround()) {
            return true;
        }
        if (entity.getDeltaMovement().y >= 0.0) {
            return false;
        }
        MobEffectInstance effect = entity.getEffect(ModEffects.WIND_JUMPING);
        return effect != null && used(entity) < limit(effect.getAmplifier());
    }

    public static void tick(LivingEntity entity) {
        if ((entity.onGround() || !entity.hasEffect(ModEffects.WIND_JUMPING)) && used(entity) != 0) {
            entity.setData(ModAttachments.AIR_JUMPS, 0);
        }
    }

    public static void onJump(LivingEntity entity) {
        if (entity.onGround() || !entity.hasEffect(ModEffects.WIND_JUMPING)) {
            return;
        }
        entity.setData(ModAttachments.AIR_JUMPS, used(entity) + 1);
    }
}
