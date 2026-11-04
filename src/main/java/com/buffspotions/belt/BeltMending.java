package com.buffspotions.belt;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public final class BeltMending {
    public static boolean hasMending(ItemStack stack) {
        return acceptsRepair(stack, EquipmentSlot.MAINHAND);
    }

    public static boolean acceptsRepair(ItemStack stack, EquipmentSlot slot) {
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            Enchantment enchantment = entry.getKey().value();
            if (enchantment.effects().has(EnchantmentEffectComponents.REPAIR_WITH_XP) && enchantment.matchingSlot(slot)) {
                return true;
            }
        }
        return false;
    }

    private BeltMending() {
    }
}
