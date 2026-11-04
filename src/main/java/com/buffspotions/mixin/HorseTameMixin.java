package com.buffspotions.mixin;

import com.buffspotions.effect.TamingLuck;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.goal.RunAroundLikeCrazyGoal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RunAroundLikeCrazyGoal.class)
abstract class HorseTameMixin {
    @Unique
    private boolean buffsPotions$tameSuccess;

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;getTemper()I")
    )
    private int buffsPotions$temper(AbstractHorse horse) {
        int temper = horse.getTemper();
        Player rider = horse.getFirstPassenger() instanceof Player player ? player : null;
        this.buffsPotions$tameSuccess = TamingLuck.mountSucceeds(
                horse.getRandom(), temper, horse.getMaxTemper(), TamingLuck.amplifier(rider, true), TamingLuck.amplifier(rider, false)
        );
        return this.buffsPotions$tameSuccess && temper <= 0 ? 1 : temper;
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 1)
    )
    private int buffsPotions$roll(RandomSource random, int bound) {
        return this.buffsPotions$tameSuccess ? 0 : bound;
    }

    @Redirect(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;modifyTemper(I)I")
    )
    private int buffsPotions$buck(AbstractHorse horse, int amount) {
        Player rider = horse.getFirstPassenger() instanceof Player player ? player : null;
        int gain = TamingLuck.temperGain(horse.getRandom(), amount, TamingLuck.amplifier(rider, true), TamingLuck.amplifier(rider, false));
        return horse.modifyTemper(gain);
    }
}
