package com.buffspotions.potion;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModDataComponents;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

public final class MundaneBrewing {
    private static final Set<Item> EXCLUDED = Set.of(
            Items.REDSTONE,
            Items.GLOWSTONE_DUST,
            Items.GUNPOWDER,
            Items.DRAGON_BREATH,
            Items.NETHER_WART
    );
    private static final ThreadLocal<Boolean> PROBING = ThreadLocal.withInitial(() -> false);

    private MundaneBrewing() {
    }

    public static boolean probing() {
        return PROBING.get();
    }

    /** A mundane potion plus this ingredient, as the still distills it. Empty when the ingredient does not brew a potion. */
    public static ItemStack distilled(PotionBrewing brewing, Item ingredient) {
        ItemStack base = PotionContents.createItemStack(Items.POTION, Potions.MUNDANE);
        ItemStack result = mix(brewing, new ItemStack(ingredient), base);
        return result == null ? ItemStack.EMPTY : result;
    }

    public static boolean isMundane(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.MUNDANE.get(), false);
    }

    /** The plain mundane potion from redstone and a water bottle, including a flagon of that potion. */
    public static boolean isPlain(ItemStack stack) {
        if (isPlainPotion(stack)) {
            return true;
        }
        FlagonContents contents = stack.get(ModDataComponents.FLAGON_CONTENTS.get());
        return contents != null && isPlainPotion(contents.dose());
    }

    private static boolean isPlainPotion(ItemStack stack) {
        return stack.is(Items.POTION)
                && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.MUNDANE)
                && !isMundane(stack);
    }

    public static Component wrapName(ItemStack stack, Component name) {
        if (!isMundane(stack)) {
            return name;
        }
        return Component.translatable("item.buffs_potions.mundane", name);
    }

    /**
     * @return {@code null} to leave vanilla brewing alone, otherwise the forced result
     */
    public static Boolean hasMix(PotionBrewing brewing, ItemStack source, ItemStack ingredient) {
        if (isMundane(source)) {
            return (ingredient.is(Items.GUNPOWDER) && source.is(Items.POTION))
                    || (ingredient.is(Items.DRAGON_BREATH) && source.is(Items.SPLASH_POTION))
                    || ingredient.is(Items.FERMENTED_SPIDER_EYE);
        }
        return fromWater(brewing, ingredient, source) == null ? null : true;
    }

    /**
     * @return {@code null} to leave vanilla brewing alone, otherwise the forced result
     */
    public static ItemStack mix(PotionBrewing brewing, ItemStack ingredient, ItemStack source) {
        if (isMundane(source)) {
            if (ingredient.is(Items.GUNPOWDER) && source.is(Items.POTION)) {
                return rebottle(source, Items.SPLASH_POTION);
            }
            if (ingredient.is(Items.DRAGON_BREATH) && source.is(Items.SPLASH_POTION)) {
                return rebottle(source, Items.LINGERING_POTION);
            }
            if (ingredient.is(Items.FERMENTED_SPIDER_EYE)) {
                return invert(brewing, source);
            }
            return source;
        }
        ItemStack brewed = fromWater(brewing, ingredient, source);
        if (brewed == null) {
            return null;
        }
        brewed.set(ModDataComponents.MUNDANE.get(), true);
        return brewed;
    }

    private static ItemStack rebottle(ItemStack source, Item item) {
        ItemStack result = new ItemStack(item);
        PotionContents contents = source.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            result.set(DataComponents.POTION_CONTENTS, contents);
        }
        result.set(ModDataComponents.MUNDANE.get(), true);
        return result;
    }

    private static ItemStack invert(PotionBrewing brewing, ItemStack source) {
        ItemStack plain = source.copy();
        plain.remove(ModDataComponents.MUNDANE.get());
        PROBING.set(true);
        try {
            ItemStack inverted = brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), plain);
            PotionContents before = plain.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            PotionContents after = inverted.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
            if (before.potion().isEmpty() || after.potion().isEmpty() || before.potion().equals(after.potion())) {
                return source;
            }
            if (!inverted.is(Items.POTION) && !inverted.is(Items.SPLASH_POTION) && !inverted.is(Items.LINGERING_POTION)) {
                return source;
            }
            inverted.set(ModDataComponents.MUNDANE.get(), true);
            return inverted;
        } finally {
            PROBING.set(false);
        }
    }

    private static ItemStack fromWater(PotionBrewing brewing, ItemStack ingredient, ItemStack source) {
        if (!source.is(Items.POTION) || EXCLUDED.contains(ingredient.getItem())) {
            return null;
        }
        PotionContents contents = source.get(DataComponents.POTION_CONTENTS);
        if (contents == null || !contents.is(Potions.MUNDANE) || isMundane(source)) {
            return null;
        }
        PROBING.set(true);
        try {
            ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
            ItemStack result = brewing.mix(ingredient, awkward);
            if (result == awkward || !isEffectPotion(result)) {
                return null;
            }
            return result;
        } finally {
            PROBING.set(false);
        }
    }

    private static boolean isEffectPotion(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || !contents.hasEffects()) {
            return false;
        }
        return !contents.is(Potions.WATER)
                && !contents.is(Potions.AWKWARD)
                && !contents.is(Potions.MUNDANE)
                && !contents.is(Potions.THICK);
    }
}
