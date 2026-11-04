package com.buffspotions.mixin.client;

import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Slot.class)
public interface SlotPositionAccessor {
    @Accessor("x")
    @Mutable
    void buffsPotions$setX(int x);

    @Accessor("y")
    @Mutable
    void buffsPotions$setY(int y);
}
