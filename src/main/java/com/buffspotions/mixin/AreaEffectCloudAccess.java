package com.buffspotions.mixin;

import net.minecraft.world.entity.AreaEffectCloud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AreaEffectCloud.class)
public interface AreaEffectCloudAccess {
    @Accessor("potionDurationScale")
    float buffsPotions$potionDurationScale();
}
