package com.buffspotions.effect;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Luck and Bad Luck change how many tries a tame takes.
 * The higher amplifier wins. Equal amplifiers leave the tame unchanged.
 * A stronger dose stays at level II, and still beats a weaker dose of the other effect.
 */
public final class TamingLuck {
    public enum Kind {
        VANILLA,
        LUCK_I,
        LUCK_II,
        BAD_I,
        BAD_II
    }

    private TamingLuck() {
    }

    public static Kind select(int luckAmplifier, int badLuckAmplifier) {
        if (luckAmplifier < 0 && badLuckAmplifier < 0) {
            return Kind.VANILLA;
        }
        if (luckAmplifier >= 0 && badLuckAmplifier >= 0 && luckAmplifier == badLuckAmplifier) {
            return Kind.VANILLA;
        }
        if (luckAmplifier > badLuckAmplifier) {
            return luckAmplifier <= 0 ? Kind.LUCK_I : Kind.LUCK_II;
        }
        return badLuckAmplifier <= 0 ? Kind.BAD_I : Kind.BAD_II;
    }

    public static int amplifier(LivingEntity living, boolean luck) {
        if (living == null) {
            return -1;
        }
        MobEffectInstance effect = living.getEffect(luck ? MobEffects.LUCK : MobEffects.UNLUCK);
        return effect == null ? -1 : effect.getAmplifier();
    }

    /**
     * A result of {@code 0} is the success vanilla checks for. Luck I doubles that chance,
     * so a 1-in-3 tame averages 1.5 items and a 1-in-10 tame averages 5.
     * Bad Luck I and II make the average twice and four times as long. Luck II always succeeds.
     */
    public static int itemRoll(RandomSource random, int bound, LivingEntity actor) {
        return itemSucceeds(random, bound, amplifier(actor, true), amplifier(actor, false)) ? 0 : 1;
    }

    public static boolean itemSucceeds(RandomSource random, int bound, int luckAmplifier, int badLuckAmplifier) {
        return switch (select(luckAmplifier, badLuckAmplifier)) {
            case LUCK_II -> true;
            case LUCK_I -> random.nextInt(bound) < 2;
            case BAD_I -> random.nextInt(bound * 2) == 0;
            case BAD_II -> random.nextInt(bound * 4) == 0;
            case VANILLA -> random.nextInt(bound) == 0;
        };
    }

    /**
     * Food and a failed ride both add temper through here.
     * Luck I doubles it. Bad Luck I and II average half and a quarter, with the remainder rolled.
     * Luck II does not change the amount; the first ride already succeeds.
     */
    public static int temperGain(RandomSource random, int amount, int luckAmplifier, int badLuckAmplifier) {
        return switch (select(luckAmplifier, badLuckAmplifier)) {
            case LUCK_I -> amount * 2;
            case BAD_I -> scale(random, amount, 2);
            case BAD_II -> scale(random, amount, 4);
            case VANILLA, LUCK_II -> amount;
        };
    }

    /**
     * Whether this ride's check tames the horse or llama.
     * Luck I treats temper as twice as high. Bad Luck I and II trust a half and a quarter of it.
     * Luck II accepts the first check, including a temper of 0.
     */
    public static boolean mountSucceeds(RandomSource random, int temper, int maxTemper, int luckAmplifier, int badLuckAmplifier) {
        if (maxTemper <= 0) {
            return false;
        }
        return switch (select(luckAmplifier, badLuckAmplifier)) {
            case LUCK_II -> true;
            case LUCK_I -> random.nextInt(maxTemper) < Math.min(maxTemper, temper * 2);
            case BAD_I -> random.nextInt(maxTemper * 2) < temper;
            case BAD_II -> random.nextInt(maxTemper * 4) < temper;
            case VANILLA -> random.nextInt(maxTemper) < temper;
        };
    }

    /** {@code amount / divisor}, plus one extra when a roll of the remainder hits. */
    private static int scale(RandomSource random, int amount, int divisor) {
        int gain = amount / divisor;
        int remainder = amount % divisor;
        if (remainder > 0 && random.nextInt(divisor) < remainder) {
            gain++;
        }
        return gain;
    }
}
