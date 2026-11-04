package com.buffspotions.mixin.client;

import com.buffspotions.effect.CrowdTuning;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LocalPlayer.class)
abstract class LocalPlayerSoundMixin {
    @ModifyVariable(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float buffsPotions$scaleVolume(float volume) {
        float scale = CrowdTuning.volumeScale((Player) (Object) this);
        return scale == 1.0F ? volume : volume * scale;
    }
}
