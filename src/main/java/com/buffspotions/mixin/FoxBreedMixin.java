package com.buffspotions.mixin;

import com.buffspotions.effect.BreedingLuck;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.animal.fox.Fox$FoxBreedGoal")
abstract class FoxBreedMixin {
    @Unique
    private int buffsPotions$extraBabies;

    @Inject(method = "breed", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$litter(CallbackInfo ci) {
        this.buffsPotions$extraBabies = 0;
        BreedGoalAccess goal = (BreedGoalAccess) (Object) this;
        Animal parent = goal.buffsPotions$animal();
        Animal partner = goal.buffsPotions$partner();
        BreedingLuck.Litter litter = BreedingLuck.roll(parent.getRandom(), parent, partner);
        if (litter.replacesBaby()) {
            BreedingLuck.deny(parent, partner);
            if (litter == BreedingLuck.Litter.VEX) {
                BreedingLuck.spawnVex(goal.buffsPotions$level(), parent);
            }
            ci.cancel();
            return;
        }
        this.buffsPotions$extraBabies = litter.extraBabies();
    }

    @Inject(
            method = "breed",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void buffsPotions$extras(CallbackInfo ci) {
        int extra = this.buffsPotions$extraBabies;
        this.buffsPotions$extraBabies = 0;
        BreedGoalAccess goal = (BreedGoalAccess) (Object) this;
        ServerLevel level = goal.buffsPotions$level();
        Animal parent = goal.buffsPotions$animal();
        Animal partner = goal.buffsPotions$partner();
        for (int i = 0; i < extra; i++) {
            BreedingLuck.spawnExtra(level, parent, partner);
        }
    }
}
