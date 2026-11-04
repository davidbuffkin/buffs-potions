package com.buffspotions.mixin.client;

import com.buffspotions.client.DisguiseStandMark;
import com.buffspotions.client.StandPose;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.Rotations;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A disguised armor stand keeps the stand's pose, including when its wooden arms are hidden. */
@Mixin(PlayerModel.class)
abstract class PlayerModelStandPoseMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("RETURN"))
    private void buffsPotions$standPose(AvatarRenderState state, CallbackInfo callback) {
        if (!(state instanceof DisguiseStandMark mark)) {
            return;
        }
        Rotations[] pose = mark.buffsPotions$standPose();
        if (pose == null) {
            return;
        }
        PlayerModel model = (PlayerModel) (Object) this;
        StandPose.apply(model, pose[0], pose[1], pose[2], pose[3], pose[4], pose[5]);
        if (!mark.buffsPotions$stillArms()) {
            StandPose.bobArms(model, state.ageInTicks);
        }
    }
}
