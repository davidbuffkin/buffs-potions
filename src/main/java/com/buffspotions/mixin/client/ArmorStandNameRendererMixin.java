package com.buffspotions.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A stand's own name shows only while looked at, the same way a mob's name does. */
@Mixin(ArmorStandRenderer.class)
abstract class ArmorStandNameRendererMixin {
    @Inject(method = "shouldShowName(Lnet/minecraft/world/entity/decoration/ArmorStand;D)Z", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$lookAtName(ArmorStand entity, double distance, CallbackInfoReturnable<Boolean> callback) {
        boolean lookedAt = entity.hasCustomName() && entity == Minecraft.getInstance().crosshairPickEntity;
        callback.setReturnValue(lookedAt);
    }
}
