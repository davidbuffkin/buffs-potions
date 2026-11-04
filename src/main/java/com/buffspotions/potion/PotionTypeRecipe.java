package com.buffspotions.potion;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

/**
 * Brews one potion type into another and keeps the bottle (drinkable, splash, or lingering).
 * Custom recipes are checked before vanilla mixes, so this can replace a vanilla mix.
 */
public final class PotionTypeRecipe implements IBrewingRecipe {
    private final Holder<Potion> from;
    private final Item ingredient;
    private final Holder<Potion> to;

    public PotionTypeRecipe(Holder<Potion> from, Item ingredient, Holder<Potion> to) {
        this.from = from;
        this.ingredient = ingredient;
        this.to = to;
    }

    @Override
    public boolean isInput(ItemStack input) {
        return matches(input);
    }

    @Override
    public boolean isIngredient(ItemStack stack) {
        return stack.is(this.ingredient);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) {
            return ItemStack.EMPTY;
        }
        return PotionContents.createItemStack(input.getItem(), this.to);
    }

    private boolean matches(ItemStack input) {
        return input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(potion -> potion.unwrapKey().equals(this.from.unwrapKey()))
                .orElse(false);
    }
}
