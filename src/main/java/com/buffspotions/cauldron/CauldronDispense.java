package com.buffspotions.cauldron;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/**
 * An empty bottle or empty flagon dispensed into a water cauldron comes back full of water.
 * The cauldron keeps its water, matching a player filling from the same block.
 */
public final class CauldronDispense {
    private CauldronDispense() {
    }

    public static ItemStack fill(ItemStack stack) {
        if (stack.is(Items.GLASS_BOTTLE)) {
            return PotionContents.createItemStack(Items.POTION, Potions.WATER);
        }
        if (stack.is(ModItems.FLAGON.get()) && FlagonContents.of(stack).isEmpty()) {
            ItemStack filled = stack.copyWithCount(1);
            filled.set(ModDataComponents.FLAGON_CONTENTS.get(), FlagonContents.EMPTY.fillWithWater());
            return filled;
        }
        return ItemStack.EMPTY;
    }
}
