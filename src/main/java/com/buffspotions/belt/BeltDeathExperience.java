package com.buffspotions.belt;

import net.minecraft.world.entity.player.Player;

/** Experience orbs left behind when a player dies wearing a belt. */
public final class BeltDeathExperience {
    public static int dropped(Player player) {
        return dropped(player.experienceLevel, player.experienceProgress, player.getXpNeededForNextLevel());
    }

    static int dropped(int level, float progress, int pointsToNext) {
        long held = pointsToReach(level);
        long partial = Math.round((double) progress * pointsToNext);
        if (partial > 0 && held <= Long.MAX_VALUE - partial) {
            held += partial;
        }
        long third = held / 3;
        return third > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) third;
    }

    /** Experience points required to arrive at {@code level} with an empty bar. */
    static long pointsToReach(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level > 40_000) {
            return Long.MAX_VALUE;
        }
        long n = level;
        if (n <= 16) {
            return n * n + 6 * n;
        }
        if (n <= 31) {
            return (5 * n * n - 81 * n + 720) / 2;
        }
        return (9 * n * n - 325 * n + 4440) / 2;
    }

    private BeltDeathExperience() {
    }
}
