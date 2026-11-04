package com.buffspotions.potion;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModEffects;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;

/**
 * Brews the effect an ingredient would put in an awkward potion into a milk bottle instead,
 * as a remedy that clears that effect. Instant effects have no remedy. Redstone and glowstone do nothing.
 * Fermented spider eye inverts a remedy. Gunpowder and dragon's breath make it splash or lingering.
 */
public final class RemedyRecipe implements IBrewingRecipe {
    private static final ThreadLocal<Boolean> SIMULATING = ThreadLocal.withInitial(() -> false);
    private static final Map<Item, Holder<MobEffect>> DIRECT = Map.of(
            Items.RESIN_CLUMP, ModEffects.BLOCK_SENSE,
            Items.COOKIE, ModEffects.ITEM_EATER,
            Items.LODESTONE, ModEffects.SEEKING,
            Items.CARVED_PUMPKIN, ModEffects.DISGUISE
    );

    @Override
    public boolean isInput(ItemStack input) {
        return FlagonContents.isMilkDose(input) || Remedies.isRemedy(input);
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return !ingredient.isEmpty();
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (SIMULATING.get() || !isInput(input) || ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (Remedies.isRemedy(input)) {
            return modify(input, ingredient);
        }
        if (!FlagonContents.isMilkDose(input)) {
            return ItemStack.EMPTY;
        }
        if (ingredient.is(Items.REDSTONE)
                || ingredient.is(Items.GLOWSTONE_DUST)
                || ingredient.is(Items.GUNPOWDER)
                || ingredient.is(Items.DRAGON_BREATH)
                || ingredient.is(Items.TORCHFLOWER)
                || ingredient.is(Items.PITCHER_PLANT)) {
            return ItemStack.EMPTY;
        }
        Holder<MobEffect> effect = brewed(ingredient);
        if (effect == null) {
            effect = DIRECT.get(ingredient.getItem());
        }
        if (effect == null || effect.value().isInstantaneous()) {
            return ItemStack.EMPTY;
        }
        return Remedies.create(effect, input.getItem());
    }

    private static ItemStack modify(ItemStack input, ItemStack ingredient) {
        if (ingredient.is(Items.REDSTONE) || ingredient.is(Items.GLOWSTONE_DUST)
                || ingredient.is(Items.TORCHFLOWER) || ingredient.is(Items.PITCHER_PLANT)) {
            return ItemStack.EMPTY;
        }
        if (input.is(Items.POTION) && ingredient.is(Items.GUNPOWDER)) {
            return copy(input, Items.SPLASH_POTION);
        }
        if (input.is(Items.SPLASH_POTION) && ingredient.is(Items.DRAGON_BREATH)) {
            return copy(input, Items.LINGERING_POTION);
        }
        if (ingredient.is(Items.FERMENTED_SPIDER_EYE)) {
            Holder<MobEffect> inverted = invert(Remedies.effectOf(input));
            if (inverted == null || inverted.value().isInstantaneous()) {
                return ItemStack.EMPTY;
            }
            return Remedies.create(inverted, input.getItem());
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack copy(ItemStack input, Item output) {
        ItemStack result = new ItemStack(output);
        PotionContents contents = input.get(DataComponents.POTION_CONTENTS);
        if (contents != null) {
            result.set(DataComponents.POTION_CONTENTS, contents);
        }
        Holder<MobEffect> effect = input.get(ModDataComponents.REMEDY.get());
        if (effect != null) {
            result.set(ModDataComponents.REMEDY.get(), effect);
        }
        return result;
    }

    private static Holder<MobEffect> brewed(ItemStack ingredient) {
        PotionBrewing brewing = ActiveBrewing.get();
        if (brewing == null) {
            return null;
        }
        ItemStack awkward = PotionContents.createItemStack(Items.POTION, Potions.AWKWARD);
        SIMULATING.set(true);
        try {
            return lastingEffect(brewing.mix(ingredient, awkward));
        } finally {
            SIMULATING.set(false);
        }
    }

    private static Holder<MobEffect> invert(Holder<MobEffect> effect) {
        if (effect == null) {
            return null;
        }
        PotionBrewing brewing = ActiveBrewing.get();
        Holder<Potion> base = basePotion(effect);
        if (brewing != null && base != null) {
            ItemStack inverted = brewing.mix(new ItemStack(Items.FERMENTED_SPIDER_EYE), PotionContents.createItemStack(Items.POTION, base));
            Holder<MobEffect> next = lastingEffect(inverted);
            if (next != null && !same(next, effect)) {
                return next;
            }
        }
        if (same(effect, MobEffects.STRENGTH)) {
            return MobEffects.WEAKNESS;
        }
        return null;
    }

    private static Holder<MobEffect> lastingEffect(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return null;
        }
        Holder<MobEffect> found = null;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().value().isInstantaneous()) {
                continue;
            }
            if (found != null && !same(found, effect.getEffect())) {
                return found;
            }
            found = effect.getEffect();
        }
        return found;
    }

    private static Holder<Potion> basePotion(Holder<MobEffect> effect) {
        Holder<Potion> match = null;
        for (Holder<Potion> potion : BuiltInRegistries.POTION.listElements().toList()) {
            String path = potion.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
            if (path.startsWith("long_") || path.startsWith("strong_")) {
                continue;
            }
            var effects = potion.value().getEffects();
            if (effects.isEmpty() || effects.getFirst().getAmplifier() != 0) {
                continue;
            }
            if (!same(effects.getFirst().getEffect(), effect)) {
                continue;
            }
            match = potion;
            if (!path.contains("_")) {
                return potion;
            }
        }
        return match;
    }

    private static boolean same(Holder<MobEffect> left, Holder<MobEffect> right) {
        return left.unwrapKey().equals(right.unwrapKey());
    }
}
