package com.buffspotions.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerPlayer.class)
public interface ServerPlayerTravelAccess {
    @Accessor("enteredNetherPosition")
    @Nullable Vec3 buffsPotions$enteredNetherPosition();

    @Accessor("enteredNetherPosition")
    void buffsPotions$enteredNetherPosition(@Nullable Vec3 position);
}
