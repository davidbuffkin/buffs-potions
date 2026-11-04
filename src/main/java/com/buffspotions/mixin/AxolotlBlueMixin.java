package com.buffspotions.mixin;

import com.buffspotions.effect.AxolotlBlue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Axolotl.class)
abstract class AxolotlBlueMixin {
    @Redirect(
            method = "getBreedOffspring",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/animal/axolotl/Axolotl;useRareVariant(Lnet/minecraft/util/RandomSource;)Z"
            )
    )
    private boolean buffsPotions$luckBlue(RandomSource random, @Local(argsOnly = true) AgeableMob partner) {
        Animal self = (Animal) (Object) this;
        Animal other = partner instanceof Animal animal ? animal : null;
        return AxolotlBlue.rare(random, self, other);
    }
}
