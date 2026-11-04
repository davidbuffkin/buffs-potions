package com.buffspotions.mixin;

import com.buffspotions.effect.Eyedentification;
import com.buffspotions.effect.Stopper;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModAttachments;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpectralArrow.class)
public abstract class SpectralArrowMixin {
    @Inject(method = "doPostHurtEffects", at = @At("RETURN"))
    private void buffsPotions$tipSpectral(LivingEntity target, CallbackInfo callback) {
        SpectralArrow arrow = (SpectralArrow) (Object) this;
        ItemStack tip = arrow.getData(ModAttachments.ARROW_TIP.get());
        if (tip.isEmpty()) {
            return;
        }
        Stopper.begin();
        Eyedentification.beginApply(tip);
        try {
            if (Remedies.isRemedy(tip)) {
                Remedies.apply(target, tip);
                return;
            }
            PotionContents contents = tip.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            float scale = tip.getOrDefault(DataComponents.POTION_DURATION_SCALE, 0.125F);
            Entity source = arrow.getEffectSource();
            contents.forEachEffect(effect -> target.addEffect(effect, source), scale);
        } finally {
            Eyedentification.endApply();
            Stopper.end();
        }
    }
}
