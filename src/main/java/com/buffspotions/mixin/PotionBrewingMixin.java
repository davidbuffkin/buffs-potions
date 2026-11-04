package com.buffspotions.mixin;

import com.buffspotions.effect.Formation;
import com.buffspotions.effect.Incarnation;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.potion.Remedies;
import com.buffspotions.potion.SuspiciousBrew;
import com.buffspotions.registry.ModPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionBrewing.class)
abstract class PotionBrewingMixin {
    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$noSplash(ItemStack source, ItemStack ingredient, CallbackInfoReturnable<Boolean> callback) {
        if (blocksWeatherCalling(source, ingredient)
                || blocksUnlingerable(source, ingredient)
                || blocksLatentThrowable(source, ingredient)) {
            callback.setReturnValue(false);
            return;
        }
        if (MundaneBrewing.probing()) {
            return;
        }
        if (stillOnly(source)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$noSplashMix(ItemStack ingredient, ItemStack source, CallbackInfoReturnable<ItemStack> callback) {
        if (blocksWeatherCalling(source, ingredient)
                || blocksUnlingerable(source, ingredient)
                || blocksLatentThrowable(source, ingredient)) {
            callback.setReturnValue(source);
            return;
        }
        if (MundaneBrewing.probing()) {
            return;
        }
        if (stillOnly(source)) {
            callback.setReturnValue(source);
        }
    }

    private static boolean stillOnly(ItemStack source) {
        return MundaneBrewing.isMundane(source) || Remedies.isRemedy(source) || SuspiciousBrew.is(source);
    }

    private static boolean blocksLatentThrowable(ItemStack source, ItemStack ingredient) {
        if (!ingredient.is(Items.GUNPOWDER) && !ingredient.is(Items.DRAGON_BREATH)) {
            return false;
        }
        return source.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.LATENT_EYEDENTIFICATION);
    }

    private static boolean blocksUnlingerable(ItemStack source, ItemStack ingredient) {
        return ingredient.is(Items.DRAGON_BREATH)
                && (Incarnation.isIncarnation(source) || Formation.isFormation(source));
    }

    private static boolean blocksWeatherCalling(ItemStack source, ItemStack ingredient) {
        if (!ingredient.is(Items.GUNPOWDER) && !ingredient.is(Items.DRAGON_BREATH)) {
            return false;
        }
        return source.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(potion -> potion.unwrapKey().equals(ModPotions.RAIN_CALLING.unwrapKey())
                        || potion.unwrapKey().equals(ModPotions.SUN_CALLING.unwrapKey()))
                .orElse(false);
    }
}
