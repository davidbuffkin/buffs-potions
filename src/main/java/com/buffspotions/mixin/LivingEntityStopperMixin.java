package com.buffspotions.mixin;

import com.buffspotions.effect.Stopper;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityStopperMixin {
    @Inject(method = "isAffectedByPotions", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$stoppered(CallbackInfoReturnable<Boolean> callback) {
        if (callback.getReturnValue() && Stopper.blocks((LivingEntity) (Object) this)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(
            method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void buffsPotions$stopperedAdd(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> callback) {
        if (Stopper.blocks((LivingEntity) (Object) this)) {
            callback.setReturnValue(false);
        }
    }
}
