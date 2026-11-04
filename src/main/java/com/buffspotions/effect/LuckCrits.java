package com.buffspotions.effect;

import net.minecraft.util.RandomSource;

/**
 * Narrows an arrow crit's bonus. Luck I keeps the upper 75% of the damage span,
 * Luck II the upper 50%. Bad Luck does the same from the bottom.
 * A stronger dose stays at the level II window.
 */
public final class LuckCrits {
    private LuckCrits() {
    }

    /**
     * @param baseDamage the hit before the crit bonus
     * @param luckAmplifier Luck's amplifier, or {@code -1} when it is absent
     * @param badLuckAmplifier Bad Luck's amplifier, or {@code -1} when it is absent
     * @return the bonus added to {@code baseDamage}, matching vanilla's {@code 0 .. base/2 + 1} when neither effect is present
     */
    public static int bonus(int baseDamage, int luckAmplifier, int badLuckAmplifier, RandomSource random) {
        int maxBonus = Math.max(0, baseDamage / 2 + 1);
        int minBonus = luckAmplifier < 0 ? 0 : cut(maxBonus, luckAmplifier);
        int topBonus = badLuckAmplifier < 0 ? maxBonus : maxBonus - cut(maxBonus, badLuckAmplifier);
        if (minBonus > topBonus) {
            minBonus = topBonus = maxBonus / 2;
        }
        if (minBonus >= topBonus) {
            return minBonus;
        }
        return minBonus + random.nextInt(topBonus - minBonus + 1);
    }

    /** Level I drops a quarter of the span. Level II and above drop half. */
    private static int cut(int span, int amplifier) {
        if (amplifier <= 0) {
            return (span + 3) / 4;
        }
        return (span + 1) / 2;
    }
}
