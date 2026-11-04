package com.buffspotions.mixin;

import com.buffspotions.potion.PotionDurations;
import java.util.List;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Potion.class)
abstract class PotionDurationMixin {
    @Inject(method = "getEffects", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$durations(CallbackInfoReturnable<List<MobEffectInstance>> callback) {
        List<MobEffectInstance> rewritten = PotionDurations.rewrite((Potion) (Object) this, callback.getReturnValue());
        if (rewritten != callback.getReturnValue()) {
            callback.setReturnValue(rewritten);
        }
    }
}
