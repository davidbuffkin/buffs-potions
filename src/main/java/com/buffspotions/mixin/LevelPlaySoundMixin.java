package com.buffspotions.mixin;

import com.buffspotions.effect.CrowdTuning;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Level.class)
abstract class LevelPlaySoundMixin {
    @Inject(
            method = "playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD")
    )
    private void buffsPotions$beginSound(
            Entity except, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, CallbackInfo callback
    ) {
        begin(except);
    }

    @Inject(
            method = "playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("RETURN")
    )
    private void buffsPotions$endSound(
            Entity except, double x, double y, double z, SoundEvent sound, SoundSource source, float volume, float pitch, CallbackInfo callback
    ) {
        end(except);
    }

    @Inject(
            method = "playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("HEAD")
    )
    private void buffsPotions$beginHolderSound(
            Entity except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, CallbackInfo callback
    ) {
        begin(except);
    }

    @Inject(
            method = "playSound(Lnet/minecraft/world/entity/Entity;DDDLnet/minecraft/core/Holder;Lnet/minecraft/sounds/SoundSource;FF)V",
            at = @At("RETURN")
    )
    private void buffsPotions$endHolderSound(
            Entity except, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, CallbackInfo callback
    ) {
        end(except);
    }

    @ModifyVariable(
            method = "playLocalSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFZ)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private float buffsPotions$scaleBreak(float volume) {
        Player player = CrowdTuning.soundSource();
        if (player == null) {
            return volume;
        }
        float scale = CrowdTuning.volumeScale(player);
        return scale == 1.0F ? volume : volume * scale;
    }

    private static void begin(Entity except) {
        if (except instanceof Player player) {
            CrowdTuning.beginPlayerSound(player);
        }
    }

    private static void end(Entity except) {
        if (except instanceof Player player) {
            CrowdTuning.endPlayerSound(player);
        }
    }
}
