package com.buffspotions.mixin;

import com.buffspotions.config.BuffsPotionsConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
abstract class HealHarmMixin {
    @Shadow
    private boolean isHarm;

    @Inject(method = "applyEffectTick", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$tick(ServerLevel level, LivingEntity entity, int amplifier, CallbackInfoReturnable<Boolean> callback) {
        affect(level, null, null, entity, amplifier, 1.0);
        callback.setReturnValue(true);
    }

    @Inject(method = "applyInstantaneousEffect", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$instant(
            ServerLevel level,
            @Nullable Entity source,
            @Nullable Entity indirect,
            LivingEntity target,
            int amplifier,
            double scale,
            CallbackInfo callback
    ) {
        affect(level, source, indirect, target, amplifier, scale);
        callback.cancel();
    }

    private void affect(
            ServerLevel level,
            @Nullable Entity source,
            @Nullable Entity indirect,
            LivingEntity target,
            int amplifier,
            double scale
    ) {
        int potionLevel = Math.max(0, amplifier) + 1;
        boolean heal = this.isHarm == target.isInvertedHealAndHarm();
        int amount = heal ? BuffsPotionsConfig.healing(potionLevel) : BuffsPotionsConfig.harming(potionLevel);
        int scaled = (int) (scale * amount + 0.5);
        if (heal) {
            target.heal(scaled);
            return;
        }
        float damage = scaled;
        if (source == null) {
            target.hurtServer(level, target.damageSources().magic(), damage);
        } else {
            target.hurtServer(level, target.damageSources().indirectMagic(source, indirect), damage);
        }
    }
}
