package com.buffspotions.effect;

import com.buffspotions.BuffsPotions;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.EffectParticleModificationEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class InvisibilityVeil {
    private InvisibilityVeil() {
    }

    public static int amplifier(LivingEntity entity) {
        MobEffectInstance effect = entity.getEffect(MobEffects.INVISIBILITY);
        return effect == null ? -1 : effect.getAmplifier();
    }

    public static boolean hidesArmor(LivingEntity entity) {
        return amplifier(entity) >= 1;
    }

    public static boolean hidesHeldItem(LivingEntity entity) {
        return amplifier(entity) >= 2;
    }

    public static boolean hidesParticles(LivingEntity entity) {
        return amplifier(entity) >= 3;
    }

    @SubscribeEvent
    public static void hideParticles(EffectParticleModificationEvent event) {
        if (hidesParticles(event.getEntity()) && event.getEffect().is(MobEffects.INVISIBILITY)) {
            event.setVisible(false);
        }
    }
}
