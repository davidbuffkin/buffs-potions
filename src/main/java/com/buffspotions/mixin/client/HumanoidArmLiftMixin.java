package com.buffspotions.mixin.client;

import com.buffspotions.client.DisguiseStandMark;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An unnamed disguised armor stand holds its arms one degree forward, off the chestplate. */
@Mixin(HumanoidModel.class)
abstract class HumanoidArmLiftMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("RETURN"))
    private void buffsPotions$liftStillArms(HumanoidRenderState state, CallbackInfo callback) {
        if (!(state instanceof DisguiseStandMark mark) || !mark.buffsPotions$stillArms()) {
            return;
        }
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        float lift = (float) Math.toRadians(1.0);
        model.rightArm.xRot -= lift;
        model.leftArm.xRot -= lift;
    }
}
