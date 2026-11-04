package com.buffspotions.mixin.client;

import com.buffspotions.client.DisguiseStandMark;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.equipment.ElytraModel;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.Rotations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Elytra on a disguised armor stand turn with the stand's body. */
@Mixin(ElytraModel.class)
abstract class ElytraStandPoseMixin {
    @Shadow
    private ModelPart leftWing;
    @Shadow
    private ModelPart rightWing;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/HumanoidRenderState;)V", at = @At("RETURN"))
    private void buffsPotions$standBody(HumanoidRenderState state, CallbackInfo callback) {
        if (!(state instanceof DisguiseStandMark mark)) {
            return;
        }
        Rotations[] pose = mark.buffsPotions$standPose();
        if (pose == null) {
            return;
        }
        Rotations body = pose[1];
        float x = (float) Math.toRadians(body.x());
        float y = (float) Math.toRadians(body.y());
        float z = (float) Math.toRadians(body.z());
        this.leftWing.xRot += x;
        this.leftWing.yRot += y;
        this.leftWing.zRot += z;
        this.rightWing.xRot += x;
        this.rightWing.yRot += y;
        this.rightWing.zRot -= z;
    }
}
