package com.buffspotions.mixin;

import com.buffspotions.effect.TamingLuck;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractHorse.class)
abstract class AbstractHorseTemperMixin {
    @Redirect(
            method = "handleEating",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/AbstractHorse;modifyTemper(I)I")
    )
    private int buffsPotions$foodTemper(AbstractHorse horse, int amount, @Local(argsOnly = true) Player player) {
        int gain = TamingLuck.temperGain(horse.getRandom(), amount, TamingLuck.amplifier(player, true), TamingLuck.amplifier(player, false));
        return horse.modifyTemper(gain);
    }
}
