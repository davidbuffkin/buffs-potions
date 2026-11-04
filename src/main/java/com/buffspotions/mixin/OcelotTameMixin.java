package com.buffspotions.mixin;

import com.buffspotions.effect.TamingLuck;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Ocelot.class)
abstract class OcelotTameMixin {
    @Redirect(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int buffsPotions$trust(RandomSource random, int bound, @Local(argsOnly = true) Player player) {
        return TamingLuck.itemRoll(random, bound, player);
    }
}
