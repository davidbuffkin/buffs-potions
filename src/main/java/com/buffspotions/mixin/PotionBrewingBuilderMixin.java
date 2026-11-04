package com.buffspotions.mixin;

import com.buffspotions.potion.VanillaPotionMixes;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PotionBrewing.Builder.class)
abstract class PotionBrewingBuilderMixin {
    @Inject(method = "addMix", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$replaceVanillaMixes(Holder<Potion> from, Item ingredient, Holder<Potion> to, CallbackInfo callback) {
        if (VanillaPotionMixes.replace((PotionBrewing.Builder) (Object) this, from, ingredient, to)) {
            callback.cancel();
        }
    }
}
