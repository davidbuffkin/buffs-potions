package com.buffspotions.effect;

import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.animal.Animal;

/**
 * Vanilla breeds a blue axolotl 1 in 1200 times. Luck I makes that 1 in 800, Luck II 1 in 600.
 * A stronger dose stays at the level II chance.
 */
public final class AxolotlBlue {
    private static final int CHANCES = 2400;

    private AxolotlBlue() {
    }

    public static boolean rare(RandomSource random, Animal parent, Animal partner) {
        return random.nextInt(CHANCES) < weight(bestLuck(parent, partner));
    }

    /** Successes out of {@value #CHANCES}. Two is the vanilla 1 in 1200. */
    static int weight(int luckAmplifier) {
        if (luckAmplifier < 0) {
            return 2;
        }
        if (luckAmplifier == 0) {
            return 3;
        }
        return 4;
    }

    private static int bestLuck(Animal parent, Animal partner) {
        return Math.max(luck(parent), luck(partner));
    }

    private static int luck(Animal animal) {
        if (animal == null || animal.getLoveCause() == null) {
            return -1;
        }
        MobEffectInstance effect = animal.getLoveCause().getEffect(MobEffects.LUCK);
        return effect == null ? -1 : effect.getAmplifier();
    }
}
