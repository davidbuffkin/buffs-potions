package com.buffspotions.mixin;

import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuSeedAccess {
    @Accessor("enchantmentSeed")
    DataSlot buffsPotions$enchantmentSeed();
}
