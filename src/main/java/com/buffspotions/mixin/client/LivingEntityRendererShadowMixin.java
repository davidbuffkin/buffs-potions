package com.buffspotions.mixin.client;

import com.buffspotions.client.ShadowMark;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererShadowMixin {
    @Unique
    private static final ThreadLocal<Boolean> buffsPotions$shadow = ThreadLocal.withInitial(() -> false);

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void buffsPotions$markShadow(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo callback) {
        ((ShadowMark) (Object) state).buffsPotions$setShadow(
                entity.hasData(ModAttachments.ENDER_SHADOW) || entity.hasEffect(ModEffects.DISLOCATED)
        );
    }

    @Inject(method = "isBodyVisible", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$translucentBody(LivingEntityRenderState state, CallbackInfoReturnable<Boolean> callback) {
        if (((ShadowMark) (Object) state).buffsPotions$isShadow()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void buffsPotions$beginShadow(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo callback) {
        buffsPotions$shadow.set(((ShadowMark) (Object) state).buffsPotions$isShadow());
    }

    @Inject(method = "submit", at = @At("RETURN"))
    private void buffsPotions$endShadow(LivingEntityRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo callback) {
        buffsPotions$shadow.remove();
    }

    @ModifyConstant(method = "submit", constant = @Constant(intValue = 654311423))
    private int buffsPotions$shadowAlpha(int color) {
        return Boolean.TRUE.equals(buffsPotions$shadow.get()) ? 0x99FFFFFF : color;
    }
}
