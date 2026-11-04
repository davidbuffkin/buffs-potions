package com.buffspotions.mixin;

import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.AttackRange;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AttackRange.class)
abstract class AttackRangeMixin {
    @Inject(method = "effectiveMaxRange", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$reaching(Entity entity, CallbackInfoReturnable<Float> callback) {
        if (!(entity instanceof LivingEntity living)) {
            return;
        }
        MobEffectInstance reaching = living.getEffect(ModEffects.REACHING);
        if (reaching == null) {
            return;
        }
        float current = callback.getReturnValue();
        float interaction = (float) living.getAttributeValue(Attributes.ENTITY_INTERACTION_RANGE);
        if (Math.abs(current - interaction) <= 0.05F) {
            return;
        }
        callback.setReturnValue(current + (float) BuffsPotionsConfig.reachingBlocks() * (reaching.getAmplifier() + 1));
    }
}
