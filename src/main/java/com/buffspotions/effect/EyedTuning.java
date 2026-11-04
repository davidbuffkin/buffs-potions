package com.buffspotions.effect;

import com.buffspotions.config.BuffsPotionsConfig;

public final class EyedTuning {
    public static final int MAX_LEVEL = 8;

    private EyedTuning() {
    }

    public static int level(int amplifier) {
        return Math.clamp(amplifier + 1, 1, MAX_LEVEL);
    }

    public static int radius(int amplifier) {
        int[] radii = BuffsPotionsConfig.senseRadii();
        return radii[Math.clamp(level(amplifier) - 1, 0, radii.length - 1)];
    }

    /** How far a sensed block may sit from air, for levels I–IV. */
    public static int airDistance(int amplifier) {
        int index = Math.clamp(level(amplifier) - 1, 0, AIR_DISTANCE.length - 1);
        return AIR_DISTANCE[index];
    }

    private static final int[] AIR_DISTANCE = {2, 3, 4, 5};

    public static int pillarHeight(int amplifier) {
        return Math.min(64, BuffsPotionsConfig.stackBase() << (level(amplifier) - 1));
    }

    public static int crackDepth(int amplifier) {
        return Math.min(64, BuffsPotionsConfig.crackBase() << (level(amplifier) - 1));
    }

    public static float eatChance(int amplifier) {
        return (float) (BuffsPotionsConfig.eatChancePerLevel() * level(amplifier));
    }

    public static int attractDistance(int amplifier) {
        return BuffsPotionsConfig.attractBlocksPerLevel() * level(amplifier);
    }

    public static double attractSpeed(double closeness) {
        return BuffsPotionsConfig.attractSpeed() + BuffsPotionsConfig.attractGain() * closeness * closeness;
    }

    public static int vomitRange(int amplifier) {
        return BuffsPotionsConfig.vomitRangePerLevel() * level(amplifier);
    }

    public static int vomitDepositInterval(int amplifier) {
        return interval(BuffsPotionsConfig.vomitDepositIntervals(), amplifier);
    }

    public static int vomitThrowInterval(int amplifier) {
        return interval(BuffsPotionsConfig.vomitThrowIntervals(), amplifier);
    }

    /** Chunk radius for seeking, levels I–IV. Higher amplifiers stay at 64. */
    public static int seekChunks(int amplifier) {
        int index = Math.clamp(level(amplifier) - 1, 0, SEEK_CHUNKS.length - 1);
        return SEEK_CHUNKS[index];
    }

    private static final int[] SEEK_CHUNKS = {16, 32, 48, 64};

    private static int interval(int[] values, int amplifier) {
        return values[Math.clamp(amplifier, 0, values.length - 1)];
    }
}
