package com.buffspotions.mixin;

import com.buffspotions.effect.LuckCrits;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractArrow.class)
abstract class ArrowLuckMixin {
    @Redirect(
            method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I")
    )
    private int buffsPotions$luckCrit(RandomSource random, int bound, @Local(ordinal = 0) int damage) {
        if (bound != damage / 2 + 2) {
            return random.nextInt(bound);
        }
        Entity owner = ((AbstractArrow) (Object) this).getOwner();
        if (!(owner instanceof LivingEntity living)) {
            return random.nextInt(bound);
        }
        int luck = amplifier(living, true);
        int badLuck = amplifier(living, false);
        if (luck < 0 && badLuck < 0) {
            return random.nextInt(bound);
        }
        return LuckCrits.bonus(damage, luck, badLuck, random);
    }

    private static int amplifier(LivingEntity living, boolean luck) {
        MobEffectInstance effect = living.getEffect(luck ? MobEffects.LUCK : MobEffects.UNLUCK);
        return effect == null ? -1 : effect.getAmplifier();
    }
}
