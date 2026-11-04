package com.buffspotions.mixin;

import com.buffspotions.effect.EggHatch;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ThrownEgg.class)
abstract class ThrownEggMixin {
    @Redirect(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int buffsPotions$eggLuck(RandomSource random, int bound) {
        if (bound != 8) {
            return random.nextInt(bound);
        }
        ThrownEgg egg = (ThrownEgg) (Object) this;
        int luck = -1;
        int badLuck = -1;
        Entity owner = egg.getOwner();
        if (owner instanceof LivingEntity living) {
            luck = amplifier(living, true);
            badLuck = amplifier(living, false);
        }
        EggHatch.Kind kind = EggHatch.select(luck, badLuck);
        if (kind == EggHatch.Kind.VANILLA) {
            return random.nextInt(8);
        }
        EggHatch.spawn(egg, EggHatch.roll(random, kind));
        return 1;
    }

    private static int amplifier(LivingEntity living, boolean luck) {
        MobEffectInstance effect = living.getEffect(luck ? MobEffects.LUCK : MobEffects.UNLUCK);
        return effect == null ? -1 : effect.getAmplifier();
    }
}
