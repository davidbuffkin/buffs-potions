package com.buffspotions.potion;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

/**
 * A milk bottle brewed into an awkward potion makes Potion of Warding.
 * The milk bottle sits in the ingredient slot, so the brewing stand consumes it.
 */
public final class WardingRecipe implements IBrewingRecipe {
    @Override
    public boolean isInput(ItemStack input) {
        return input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(potion -> potion.unwrapKey().equals(Potions.AWKWARD.unwrapKey()))
                .orElse(false);
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return FlagonContents.isMilkDose(ingredient);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) {
            return ItemStack.EMPTY;
        }
        return PotionContents.createItemStack(input.getItem(), ModPotions.WARDING);
    }
}
