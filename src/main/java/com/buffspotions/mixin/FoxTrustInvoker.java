package com.buffspotions.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Fox.class)
public interface FoxTrustInvoker {
    @Invoker("addTrustedEntity")
    void buffsPotions$trust(LivingEntity entity);
}
