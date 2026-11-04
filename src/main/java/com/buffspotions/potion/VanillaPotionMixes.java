package com.buffspotions.potion;

import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;

/** Vanilla mixes this mod replaces before the brewing-stand recipe book is built from them. */
public final class VanillaPotionMixes {
    private VanillaPotionMixes() {
    }

    /** @return true when the vanilla mix should not be recorded */
    public static boolean replace(PotionBrewing.Builder builder, Holder<Potion> from, Item ingredient, Holder<Potion> to) {
        if (is(from, ingredient, to, Potions.WATER, Items.GLOWSTONE_DUST, Potions.THICK)) {
            builder.addMix(Potions.WATER, Items.GLOWSTONE_DUST, Potions.MUNDANE);
            return true;
        }
        if (is(from, ingredient, to, Potions.AWKWARD, Items.SPIDER_EYE, Potions.POISON)) {
            builder.addMix(Potions.AWKWARD, Items.POISONOUS_POTATO, Potions.POISON);
            return true;
        }
        return is(from, ingredient, to, Potions.WATER, Items.FERMENTED_SPIDER_EYE, Potions.WEAKNESS)
                || is(from, ingredient, to, Potions.LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.SLOWNESS)
                || is(from, ingredient, to, Potions.LONG_LEAPING, Items.FERMENTED_SPIDER_EYE, Potions.LONG_SLOWNESS)
                || is(from, ingredient, to, Potions.POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING)
                || is(from, ingredient, to, Potions.LONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.HARMING)
                || is(from, ingredient, to, Potions.STRONG_POISON, Items.FERMENTED_SPIDER_EYE, Potions.STRONG_HARMING);
    }

    private static boolean is(
            Holder<Potion> from,
            Item ingredient,
            Holder<Potion> to,
            Holder<Potion> expectedFrom,
            Item expectedIngredient,
            Holder<Potion> expectedTo
    ) {
        return from.is(expectedFrom) && ingredient == expectedIngredient && to.is(expectedTo);
    }
}
