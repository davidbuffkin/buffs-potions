package com.buffspotions.mixin.client;

import com.buffspotions.client.GolemHeadSpin;
import com.buffspotions.client.GolemHeadSpinMark;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.animal.golem.CopperGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CopperGolemModel.class)
abstract class CopperGolemModelMixin {
    @Unique
    private KeyframeAnimation buffsPotions$headSpin;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void buffsPotions$bakeHeadSpin(ModelPart root, CallbackInfo callback) {
        this.buffsPotions$headSpin = GolemHeadSpin.ANIMATION.bake(((CopperGolemModel) (Object) this).root());
    }

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/CopperGolemRenderState;)V", at = @At("RETURN"))
    private void buffsPotions$applyHeadSpin(CopperGolemRenderState state, CallbackInfo callback) {
        this.buffsPotions$headSpin.apply(((GolemHeadSpinMark) state).buffsPotions$headSpin(), state.ageInTicks);
    }
}
