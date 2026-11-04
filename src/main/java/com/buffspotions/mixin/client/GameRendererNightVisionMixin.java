package com.buffspotions.mixin.client;

import com.buffspotions.belt.BeltEnchantments;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
abstract class GameRendererNightVisionMixin {
    @Inject(method = "nightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void buffsPotions$steadyGulpNightVision(
            LivingEntity camera, float partialTick, CallbackInfoReturnable<Float> callback
    ) {
        if (camera instanceof Player player
                && player.hasEffect(MobEffects.NIGHT_VISION)
                && BeltEnchantments.steadiesNightVision(player)) {
            callback.setReturnValue(1.0F);
        }
    }
}
