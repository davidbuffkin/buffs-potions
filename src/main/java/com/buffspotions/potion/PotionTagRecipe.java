package com.buffspotions.potion;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

/**
 * Brews an awkward potion with any item in a tag, keeping the bottle type.
 */
public final class PotionTagRecipe implements IBrewingRecipe {
    private final Holder<Potion> from;
    private final TagKey<Item> ingredient;
    private final Holder<Potion> to;

    public PotionTagRecipe(Holder<Potion> from, TagKey<Item> ingredient, Holder<Potion> to) {
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
