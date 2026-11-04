package com.buffspotions.mixin.client;

import com.buffspotions.client.GolemHeadSpinMark;
import net.minecraft.client.renderer.entity.CopperGolemRenderer;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperGolemRenderer.class)
abstract class CopperGolemRendererMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/animal/golem/CopperGolem;Lnet/minecraft/client/renderer/entity/state/CopperGolemRenderState;F)V",
            at = @At("RETURN")
    )
    private void buffsPotions$copyHeadSpin(CopperGolem entity, CopperGolemRenderState state, float partialTick, CallbackInfo callback) {
        ((GolemHeadSpinMark) state).buffsPotions$headSpin().copyFrom(((GolemHeadSpinMark) entity).buffsPotions$headSpin());
    }
}
