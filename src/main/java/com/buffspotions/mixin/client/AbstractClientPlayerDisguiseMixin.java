package com.buffspotions.mixin.client;

import com.buffspotions.client.DisguiseSkins;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
abstract class AbstractClientPlayerDisguiseMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$disguise(CallbackInfoReturnable<PlayerSkin> callback) {
        PlayerSkin disguised = DisguiseSkins.apply((AbstractClientPlayer) (Object) this, callback.getReturnValue());
        if (disguised != callback.getReturnValue()) {
            callback.setReturnValue(disguised);
        }
    }
}
