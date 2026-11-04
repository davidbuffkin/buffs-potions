package com.buffspotions.mixin;

import com.buffspotions.effect.TamingLuck;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.animal.equine.Llama;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Llama.class)
abstract class LlamaTemperMixin {
    @Redirect(
            method = "handleEating",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/Llama;modifyTemper(I)I")
    )
    private int buffsPotions$foodTemper(Llama llama, int amount, @Local(argsOnly = true) Player player) {
        int gain = TamingLuck.temperGain(llama.getRandom(), amount, TamingLuck.amplifier(player, true), TamingLuck.amplifier(player, false));
        return llama.modifyTemper(gain);
    }
}
