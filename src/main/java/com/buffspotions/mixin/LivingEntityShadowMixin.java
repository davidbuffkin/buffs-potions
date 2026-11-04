package com.buffspotions.mixin;

import com.buffspotions.registry.ModAttachments;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntityShadowMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$shadowStays(CallbackInfoReturnable<Boolean> callback) {
        if (((LivingEntity) (Object) this).hasData(ModAttachments.ENDER_SHADOW)) {
            callback.setReturnValue(false);
        }
    }
}
