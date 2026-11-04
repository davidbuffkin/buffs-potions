package com.buffspotions.effect;

import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.registry.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Starting numbers for Quiet and Riotous. Levels I–IV are amplifiers 0–3;
 * higher amplifiers stay on the last step. Only I and II are brewable.
 */
public final class CrowdTuning {
    public static final double VANILLA_SAFE_DISTANCE = 24.0;
    public static final double FLEE_DISTANCE = 8.0;

    public enum Kind {
        NONE,
        QUIET,
        RIOTOUS
    }

    /**
     * Player whose {@code playSound} is on the stack. Footsteps and hurt sounds are played at a
     * position with the player excluded from the packet, so the event itself does not name them.
     */
    private static final ThreadLocal<Player> SOUND_SOURCE = new ThreadLocal<>();
    private static final ThreadLocal<Integer> SOUND_DEPTH = new ThreadLocal<>();

    private CrowdTuning() {
    }

    public static int tier(int amplifier) {
        return Math.clamp(amplifier, 0, 3);
    }

    public static void beginPlayerSound(Player player) {
        Integer depth = SOUND_DEPTH.get();
        if (depth == null) {
            SOUND_SOURCE.set(player);
            SOUND_DEPTH.set(1);
        } else {
            SOUND_DEPTH.set(depth + 1);
        }
    }

    public static void endPlayerSound(Player player) {
        Integer depth = SOUND_DEPTH.get();
        if (depth == null) {
            return;
        }
        if (depth > 1) {
            SOUND_DEPTH.set(depth - 1);
            return;
        }
        if (SOUND_SOURCE.get() == player) {
            SOUND_SOURCE.remove();
            SOUND_DEPTH.remove();
        }
    }

    public static Player soundSource() {
        return SOUND_SOURCE.get();
    }

    /** Absent effect is {@code -1}. */
    public static int quietAmplifier(Player player) {
        MobEffectInstance effect = player.getEffect(ModEffects.QUIET);
        return effect == null ? -1 : effect.getAmplifier();
    }

    /** Absent effect is {@code -1}. */
    public static int riotAmplifier(Player player) {
        MobEffectInstance effect = player.getEffect(ModEffects.RIOTOUS);
        return effect == null ? -1 : effect.getAmplifier();
    }

    public static int effectLevel(int amplifier) {
        return amplifier < 0 ? 0 : tier(amplifier) + 1;
    }

    /**
     * Each potion contributes its difference from normal. One potion alone keeps its own factor.
     * Both together are {@code quiet + riot - 1}.
     */
    public static float combineFactors(float quietFactor, float riotFactor) {
        return Math.max(0.0F, quietFactor + riotFactor - 1.0F);
    }

    public static boolean calms(Player player) {
        return effectLevel(quietAmplifier(player)) > effectLevel(riotAmplifier(player));
    }

    public static boolean riots(Player player) {
        return effectLevel(riotAmplifier(player)) > effectLevel(quietAmplifier(player));
    }

    public static boolean changesSafeZone(Player player) {
        return quietAmplifier(player) >= 0 || riotAmplifier(player) >= 0;
    }

    public static float volumeScale(Kind kind, int amplifier) {
        return switch (kind) {
            case QUIET -> at(BuffsPotionsConfig.quietVolume(), amplifier);
            case RIOTOUS -> at(BuffsPotionsConfig.riotVolume(), amplifier);
            case NONE -> 1.0F;
        };
    }

    public static float volumeScale(int quietAmplifier, int riotAmplifier) {
        return combineFactors(
                factor(BuffsPotionsConfig.quietVolume(), quietAmplifier),
                factor(BuffsPotionsConfig.riotVolume(), riotAmplifier)
        );
    }

    public static float volumeScale(Player player) {
        return volumeScale(quietAmplifier(player), riotAmplifier(player));
    }

    public static float skulkIgnoreChance(int amplifier) {
        return at(BuffsPotionsConfig.quietSkulk(), amplifier);
    }

    public static boolean ignoresCommotion(Player player, RandomSource random) {
        int quiet = quietAmplifier(player);
        return quiet >= 0 && random.nextFloat() < skulkIgnoreChance(quiet);
    }

    public static int sensorRadius(Player player, int radius) {
        int riot = riotAmplifier(player);
        if (riot < 0) {
            return radius;
        }
        return Math.max(1, Math.round(radius * volumeScale(Kind.RIOTOUS, riot)));
    }

    public static double safeDistance(Kind kind, int amplifier) {
        return switch (kind) {
            case QUIET -> at(BuffsPotionsConfig.quietSafe(), amplifier);
            case RIOTOUS -> at(BuffsPotionsConfig.riotSafe(), amplifier);
            case NONE -> VANILLA_SAFE_DISTANCE;
        };
    }

    public static double safeDistance(int quietAmplifier, int riotAmplifier) {
        double quiet = quietAmplifier < 0 ? VANILLA_SAFE_DISTANCE : at(BuffsPotionsConfig.quietSafe(), quietAmplifier);
        double riot = riotAmplifier < 0 ? VANILLA_SAFE_DISTANCE : at(BuffsPotionsConfig.riotSafe(), riotAmplifier);
        return Math.max(1.0, quiet + riot - VANILLA_SAFE_DISTANCE);
    }

    public static double safeDistance(Player player) {
        return safeDistance(quietAmplifier(player), riotAmplifier(player));
    }

    public static int localCap(MobCategory category, Kind kind, int amplifier) {
        return switch (kind) {
            case QUIET -> localCap(category, amplifier, -1);
            case RIOTOUS -> localCap(category, -1, amplifier);
            case NONE -> category.getMaxInstancesPerChunk();
        };
    }

    public static int localCap(MobCategory category, int quietAmplifier, int riotAmplifier) {
        int base = category.getMaxInstancesPerChunk();
        if (base < 0 || (quietAmplifier < 0 && riotAmplifier < 0)) {
            return base;
        }
        return Math.max(1, Math.round(base * capMultiplier(category, quietAmplifier, riotAmplifier)));
    }

    public static int localCap(Player player, MobCategory category) {
        return localCap(category, quietAmplifier(player), riotAmplifier(player));
    }

    /**
     * World mob cap scale. Each affected player's difference from 1 adds, the same way one
     * player's two potions add. {@code 1} means leave the vanilla cap alone.
     */
    public static float globalCapScale(Iterable<? extends Player> players, MobCategory category) {
        if (category == MobCategory.MISC || category.getMaxInstancesPerChunk() < 0) {
            return 1.0F;
        }
        float scale = 1.0F;
        for (Player player : players) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            int quiet = quietAmplifier(player);
            int riot = riotAmplifier(player);
            if (quiet < 0 && riot < 0) {
                continue;
            }
            scale += capMultiplier(category, quiet, riot) - 1.0F;
        }
        return Math.max(0.0F, scale);
    }

    public static int spawnerNearbyCap(int base, Kind kind, int amplifier) {
        return switch (kind) {
            case QUIET -> spawnerNearbyCap(base, amplifier, -1);
            case RIOTOUS -> spawnerNearbyCap(base, -1, amplifier);
            case NONE -> base;
        };
    }

    public static int spawnerNearbyCap(int base, int quietAmplifier, int riotAmplifier) {
        if (quietAmplifier < 0 && riotAmplifier < 0) {
            return base;
        }
        return Math.max(1, Math.round(base * spawnerMultiplier(quietAmplifier, riotAmplifier)));
    }

    public static float spawnerMultiplier(ServerLevel level, Vec3 center, double range) {
        double rangeSqr = range * range;
        float scale = 1.0F;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.distanceToSqr(center) > rangeSqr) {
                continue;
            }
            int quiet = quietAmplifier(player);
            int riot = riotAmplifier(player);
            if (quiet < 0 && riot < 0) {
                continue;
            }
            scale += spawnerMultiplier(quiet, riot) - 1.0F;
        }
        return Math.max(0.0F, scale);
    }

    /**
     * @return the delay after this tick's extra pause or hurry, before vanilla decrements it once
     */
    public static int adjustedDelay(int delay, Kind kind, int amplifier, RandomSource random) {
        return switch (kind) {
            case QUIET -> adjustedDelay(delay, amplifier, -1, random);
            case RIOTOUS -> adjustedDelay(delay, -1, amplifier, random);
            case NONE -> delay;
        };
    }

    public static int adjustedDelay(int delay, int quietAmplifier, int riotAmplifier, RandomSource random) {
        if (delay <= 0) {
            return delay;
        }
        if (quietAmplifier >= 0 && random.nextFloat() < at(BuffsPotionsConfig.quietDelaySkip(), quietAmplifier)) {
            delay++;
        }
        if (riotAmplifier >= 0) {
            int extra = random.nextFloat() < at(BuffsPotionsConfig.riotDelayHurry(), riotAmplifier) ? 1 : 0;
            if (random.nextFloat() < at(BuffsPotionsConfig.riotDelayAgain(), riotAmplifier)) {
                extra++;
            }
            delay = Math.max(0, delay - extra);
        }
        return delay;
    }

    public static int adjustSpawnerDelay(ServerLevel level, Vec3 center, double range, int delay, RandomSource random) {
        double rangeSqr = range * range;
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator() || player.distanceToSqr(center) > rangeSqr) {
                continue;
            }
            delay = adjustedDelay(delay, quietAmplifier(player), riotAmplifier(player), random);
        }
        return delay;
    }

    private static float at(float[] table, int amplifier) {
        if (table.length == 0) {
            return 1.0F;
        }
        return table[Math.min(tier(amplifier), table.length - 1)];
    }

    private static int at(int[] table, int amplifier) {
        if (table.length == 0) {
            return (int) VANILLA_SAFE_DISTANCE;
        }
        return table[Math.min(tier(amplifier), table.length - 1)];
    }

    private static float factor(float[] table, int amplifier) {
        return amplifier < 0 ? 1.0F : at(table, amplifier);
    }

    private static float capMultiplier(MobCategory category, int quietAmplifier, int riotAmplifier) {
        boolean monster = category == MobCategory.MONSTER;
        float quiet = factor(monster ? BuffsPotionsConfig.quietHostile() : BuffsPotionsConfig.quietPassive(), quietAmplifier);
        float riot = factor(monster ? BuffsPotionsConfig.riotHostile() : BuffsPotionsConfig.riotPassive(), riotAmplifier);
        return combineFactors(quiet, riot);
    }

    private static float spawnerMultiplier(int quietAmplifier, int riotAmplifier) {
        return combineFactors(
                factor(BuffsPotionsConfig.quietSpawner(), quietAmplifier),
                factor(BuffsPotionsConfig.riotSpawner(), riotAmplifier)
        );
    }

    public static boolean shouldFlee(PathfinderMob mob) {
        if (!mob.isAlive() || mob.isPassenger() || mob.isLeashed()) {
            return false;
        }
        MobCategory category = mob.getType().getCategory();
        if (category == MobCategory.MONSTER || category == MobCategory.MISC || !category.isFriendly()) {
            return false;
        }
        if (mob instanceof TamableAnimal tamable && tamable.isTame()) {
            return false;
        }
        return !(mob instanceof AbstractHorse horse && horse.isTamed());
    }
}
