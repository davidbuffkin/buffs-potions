package com.buffspotions.potion;

import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Brews named by the brewing-tab advancements. */
public final class PotionGrades {
    private PotionGrades() {
    }

    /** Potion of Speed II, the 1:30 glowstone brew. */
    public static boolean speedTwo(ItemStack stack) {
        return is(stack, Potions.STRONG_SWIFTNESS);
    }

    /** Potion of Water Breathing, the 6:00 redstone brew. */
    public static boolean longWaterBreathing(ItemStack stack) {
        return is(stack, Potions.LONG_WATER_BREATHING);
    }

    /** Insight III at a quarter of its base, the second torchflower thicken. */
    public static boolean insightThree(ItemStack stack) {
        return stackGrade(stack, "insight", ModEffects.INSIGHTFUL, 2, quarter(BuffsPotionsConfig.baseFor("insight")));
    }

    /** Night Vision stretched to four times its base, the second pitcher extend. */
    public static boolean nightVisionTwice(ItemStack stack) {
        return stackGrade(stack, "night_vision", MobEffects.NIGHT_VISION, 0, times(BuffsPotionsConfig.baseFor("night_vision"), 4));
    }

    /** Strength II at one and a half times its base, the clarified brew. */
    public static boolean strengthClarified(ItemStack stack) {
        return stackGrade(stack, "strength", MobEffects.STRENGTH, 1, clarified(BuffsPotionsConfig.baseFor("strength")));
    }

    private static boolean is(ItemStack stack, Holder<Potion> potion) {
        if (rejected(stack)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(potion);
    }

    private static boolean stackGrade(ItemStack stack, String name, Holder<MobEffect> effect, int amplifier, int duration) {
        if (rejected(stack)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || contents.customName().filter(name::equals).isEmpty()) {
            return false;
        }
        for (MobEffectInstance instance : contents.getAllEffects()) {
            if (instance.is(effect) && instance.getAmplifier() == amplifier && near(instance.getDuration(), duration)) {
                return true;
            }
        }
        return false;
    }

    private static boolean rejected(ItemStack stack) {
        return stack == null || stack.isEmpty() || Remedies.isRemedy(stack) || MundaneBrewing.isMundane(stack);
    }

    private static int quarter(int base) {
        return Math.max(1, base / 4);
    }

    private static int times(int base, int factor) {
        return Math.max(1, base * factor);
    }

    private static int clarified(int base) {
        return Math.max(1, (int) Math.round(base * 1.5));
    }

    private static boolean near(int actual, int expected) {
        return Math.abs(actual - expected) <= 1;
    }
}
