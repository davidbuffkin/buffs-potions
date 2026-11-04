package com.buffspotions.mixin;

import com.buffspotions.effect.Eyedentification;
import com.buffspotions.effect.Stopper;
import com.buffspotions.potion.Remedies;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Arrow.class)
public abstract class ArrowMixin {
    @Inject(method = "doPostHurtEffects", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$beginArrow(LivingEntity target, CallbackInfo callback) {
        ItemStack stack = ((Arrow) (Object) this).getPickupItemStackOrigin();
        Stopper.begin();
        Eyedentification.beginApply(stack);
        if (Remedies.isRemedy(stack)) {
            Remedies.apply(target, stack);
            callback.cancel();
        }
    }

    @Inject(method = "doPostHurtEffects", at = @At("RETURN"))
    private void buffsPotions$endArrow(LivingEntity target, CallbackInfo callback) {
        Eyedentification.endApply();
        Stopper.end();
    }
}
