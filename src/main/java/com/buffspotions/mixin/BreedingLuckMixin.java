package com.buffspotions.mixin;

import com.buffspotions.effect.BreedingLuck;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
abstract class BreedingLuckMixin {
    @Unique
    private int buffsPotions$extraBabies;

    @Inject(method = "spawnChildFromBreeding", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$litter(ServerLevel level, Animal partner, CallbackInfo ci) {
        this.buffsPotions$extraBabies = 0;
        Animal self = (Animal) (Object) this;
        BreedingLuck.Litter litter = BreedingLuck.roll(self.getRandom(), self, partner);
        if (litter.replacesBaby()) {
            BreedingLuck.deny(self, partner);
            if (litter == BreedingLuck.Litter.VEX) {
                BreedingLuck.spawnVex(level, self);
            }
            ci.cancel();
            return;
        }
        this.buffsPotions$extraBabies = litter.extraBabies();
    }

    @Inject(
            method = "spawnChildFromBreeding",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void buffsPotions$extras(ServerLevel level, Animal partner, CallbackInfo ci) {
        int extra = this.buffsPotions$extraBabies;
        this.buffsPotions$extraBabies = 0;
        Animal self = (Animal) (Object) this;
        for (int i = 0; i < extra; i++) {
            BreedingLuck.spawnExtra(level, self, partner);
        }
    }
}
