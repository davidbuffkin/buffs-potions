package com.buffspotions.mixin.client;

import com.buffspotions.client.InvisibilityRender;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidMobRenderer.class)
abstract class HumanoidInvisibilityMixin {
    @Inject(method = "extractHumanoidRenderState", at = @At("RETURN"))
    private static void buffsPotions$hideGear(
            LivingEntity entity, HumanoidRenderState state, float partialTicks, ItemModelResolver resolver, CallbackInfo callback
    ) {
        InvisibilityRender.hide(entity, state);
    }
}
