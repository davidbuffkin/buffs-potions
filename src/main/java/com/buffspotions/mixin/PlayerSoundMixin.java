package com.buffspotions.mixin;

import com.buffspotions.effect.CrowdTuning;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerSoundMixin {
    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"))
    private void buffsPotions$beginSound(SoundEvent sound, float volume, float pitch, CallbackInfo callback) {
        CrowdTuning.beginPlayerSound((Player) (Object) this);
    }

    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("RETURN"))
    private void buffsPotions$endSound(SoundEvent sound, float volume, float pitch, CallbackInfo callback) {
        CrowdTuning.endPlayerSound((Player) (Object) this);
    }

    @Inject(method = "playServerSideSound", at = @At("HEAD"))
    private void buffsPotions$beginServerSound(SoundEvent sound, CallbackInfo callback) {
        CrowdTuning.beginPlayerSound((Player) (Object) this);
    }

    @Inject(method = "playServerSideSound", at = @At("RETURN"))
    private void buffsPotions$endServerSound(SoundEvent sound, CallbackInfo callback) {
        CrowdTuning.endPlayerSound((Player) (Object) this);
    }
}
