package com.buffspotions.mixin;

import com.buffspotions.effect.TamingLuck;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Wolf.class)
abstract class WolfTameMixin {
    @Redirect(method = "tryToTame", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int buffsPotions$tame(RandomSource random, int bound, @Local(argsOnly = true) Player player) {
        return TamingLuck.itemRoll(random, bound, player);
    }
}
