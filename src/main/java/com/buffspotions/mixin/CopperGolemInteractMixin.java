package com.buffspotions.mixin;

import com.buffspotions.bellows.CopperGolemBellows;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CopperGolem.class)
abstract class CopperGolemInteractMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$keepBellows(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback) {
        InteractionResult result = CopperGolemBellows.keepBellows((CopperGolem) (Object) this, player, hand);
        if (result != null) {
            callback.setReturnValue(result);
        }
    }
}
