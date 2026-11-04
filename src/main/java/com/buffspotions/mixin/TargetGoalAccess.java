package com.buffspotions.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(TargetGoal.class)
public interface TargetGoalAccess {
    @Accessor("mob")
    Mob buffsPotions$mob();

    @Accessor("mustSee")
    boolean buffsPotions$mustSee();
}
