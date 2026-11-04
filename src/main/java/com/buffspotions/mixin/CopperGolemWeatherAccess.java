package com.buffspotions.mixin;

import net.minecraft.world.entity.animal.golem.CopperGolem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CopperGolem.class)
public interface CopperGolemWeatherAccess {
    @Accessor("nextWeatheringTick")
    long buffsPotions$getNextWeatheringTick();

    @Accessor("nextWeatheringTick")
    void buffsPotions$setNextWeatheringTick(long nextWeatheringTick);
}
