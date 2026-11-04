package com.buffspotions.mixin.client;

import com.buffspotions.client.ParameterNames;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EffectsInInventory.class)
abstract class EffectDurationLabelMixin {
    @Redirect(
            method = "extractEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/effect/MobEffectUtil;formatDuration(Lnet/minecraft/world/effect/MobEffectInstance;FF)Lnet/minecraft/network/chat/Component;"
            )
    )
    private Component buffsPotions$oneClock(MobEffectInstance effect, float scale, float tickrate) {
        if (ParameterNames.several(effect.getEffect())) {
            return Component.empty();
        }
        return MobEffectUtil.formatDuration(effect, scale, tickrate);
    }
}
