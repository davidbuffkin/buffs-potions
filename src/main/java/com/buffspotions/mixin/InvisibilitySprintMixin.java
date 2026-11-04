package com.buffspotions.mixin;

import com.buffspotions.effect.InvisibilityVeil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class InvisibilitySprintMixin {
    @Inject(method = "canSpawnSprintParticle", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$hideFootsteps(CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof LivingEntity living && InvisibilityVeil.hidesParticles(living)) {
            callback.setReturnValue(false);
        }
    }
}
