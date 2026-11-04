package com.buffspotions.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BreedGoal.class)
public interface BreedGoalAccess {
    @Accessor("animal")
    Animal buffsPotions$animal();

    @Accessor("partner")
    Animal buffsPotions$partner();

    @Accessor("level")
    ServerLevel buffsPotions$level();
}
