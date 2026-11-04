package com.buffspotions.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractThrownPotion.class)
public interface ThrownPotionHitInvoker {
    @Invoker("onHitAsWater")
    void buffsPotions$onHitAsWater(ServerLevel level);

    @Invoker("dowseFire")
    void buffsPotions$dowseFire(BlockPos pos);
}
