package com.buffspotions.config;

import com.buffspotions.effect.MobDrops;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class BuffsPotionsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue BELT_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown shared by potion and flagon belts after consuming a dose.")
            .defineInRange("belt.cooldownTicks", 20, 0, 20 * 60);

    public static final ModConfigSpec.IntValue BELT_DRINK_DURATION_TICKS = BUILDER
            .comment("Base time required to drink from an equipped belt.")
            .defineInRange("belt.drinkDurationTicks", 32, 1, 20 * 60);

    public static final ModConfigSpec.IntValue POTION_BASE_DURATION = BUILDER
            .comment(
                    "Base duration in ticks for a normal 3:00 potion. Restart to apply. Instant potions stay at 1 tick.",
                    "One redstone extend (a thinned potion) doubles this. One glowstone thicken (a viscid potion) halves it.",
                    "A second extend or thicken waits on the brewing stack. A potion that is both extended and thickened (a clarified potion) lasts 1.5 times this base.",
                    "Potions with a different vanilla base, such as regeneration, keep that base unless listed in potions.baseOverrides."
            )
            .defineInRange("potions.baseDuration", 3600, 1, 720000);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> POTION_BASE_OVERRIDES = BUILDER
            .comment(
                    "Override one potion's base duration, as potion_name=ticks. Restart to apply.",
                    "The name is the effect name, such as regeneration=900 or finesse=3600.",
                    "Extend, thicken, and the future clarified duration are calculated from that base."
            )
            .defineListAllowEmpty(
                    "potions.baseOverrides",
                    List.of(),
                    () -> "regeneration=900",
                    value -> value instanceof String line && line.contains("=")
            );

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> SENSE_RADII = BUILDER
            .comment("Block sense radius for levels I, II, III, and IV. Higher levels use the last value.")
            .defineListAllowEmpty(
                    "sense.radii",
                    List.of(10, 13, 16, 19),
                    () -> 15,
                    value -> value instanceof Integer radius && radius >= 1 && radius <= 256
            );

    public static final ModConfigSpec.IntValue STACK_BASE = BUILDER
            .comment("Pillar height at level I. Each higher level doubles it.")
            .defineInRange("stacking.baseHeight", 2, 1, 256);

    public static final ModConfigSpec.IntValue CRACK_BASE = BUILDER
            .comment("Crack depth at level I, counting the block you mined. Each higher level doubles it.")
            .defineInRange("cracking.baseDepth", 2, 1, 256);

    public static final ModConfigSpec.DoubleValue EAT_CHANCE = BUILDER
            .comment("Unused.")
            .defineInRange("eating.chancePerLevel", 0.1, 0.0, 1.0);

    public static final ModConfigSpec.IntValue ATTRACT_BLOCKS = BUILDER
            .comment(
                    "Blocks added to an attraction radius for each level.",
                    "Item Eater uses this many blocks per level for matching drops.",
                    "Insight adds this many blocks per level to the usual 8-block experience orb follow."
            )
            .defineInRange("eating.attractBlocksPerLevel", 3, 1, 64);

    public static final ModConfigSpec.DoubleValue ATTRACT_SPEED = BUILDER
            .comment("Pull speed at the edge of the attraction radius.")
            .defineInRange("eating.attractSpeed", 0.08, 0.0, 4.0);

    public static final ModConfigSpec.DoubleValue ATTRACT_GAIN = BUILDER
            .comment("Extra pull added as the target approaches, scaled by closeness squared.")
            .defineInRange("eating.attractGain", 0.80, 0.0, 8.0);

    public static final ModConfigSpec.IntValue VOMIT_RANGE = BUILDER
            .comment("Chest search radius per vomiting level.")
            .defineInRange("vomiting.rangePerLevel", 5, 1, 64);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> VOMIT_DEPOSIT = BUILDER
            .comment("Ticks between chest deposits for vomiting levels I through IV.")
            .defineListAllowEmpty(
                    "vomiting.depositIntervals",
                    List.of(64, 16, 4, 1),
                    () -> 1,
                    value -> value instanceof Integer ticks && ticks >= 1 && ticks <= 72000
            );

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> VOMIT_THROW = BUILDER
            .comment("Ticks between thrown items for vomiting levels I through IV.")
            .defineListAllowEmpty(
                    "vomiting.throwIntervals",
                    List.of(16, 8, 4, 2),
                    () -> 1,
                    value -> value instanceof Integer ticks && ticks >= 1 && ticks <= 72000
            );

    public static final ModConfigSpec.IntValue SEEK_RANGE = BUILDER
            .comment("Unused. Seeking radii are fixed at 16, 32, 48, and 64 chunks for levels I–IV, for blocks, mobs, and players.")
            .defineInRange("seeking.rangePerLevel", 16, 1, 512);

    public static final ModConfigSpec.DoubleValue FLOWER_SPROUT = BUILDER
            .comment("Chance per flower-walking level to sprout something when stepping onto a new grass block.")
            .defineInRange("flowerWalking.sproutChancePerLevel", 0.025, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue FLOWER_INTERESTING = BUILDER
            .comment("Chance that a flower-walking sprout is one of the less common plants.")
            .defineInRange("flowerWalking.interestingChance", 0.15, 0.0, 1.0);

    public static final ModConfigSpec.DoubleValue FINESSE_REMAINING = BUILDER
            .comment("Each finesse level multiplies the chance that a durability item still takes damage by this. 0.5 skips half, then three quarters.")
            .defineInRange("finesse.durabilityChance", 0.5, 0.0, 1.0);

    public static final ModConfigSpec.IntValue FUMBLING_FACTOR = BUILDER
            .comment("Fumbling multiplies durability damage by this once per level. 2 is double, then quadruple.")
            .defineInRange("fumbling.damageFactor", 2, 1, 16);

    public static final ModConfigSpec.DoubleValue REACHING_BLOCKS = BUILDER
            .comment("Extra block-reach distance per reaching level.")
            .defineInRange("reaching.blocksPerLevel", 2.0, 0.0, 32.0);

    public static final ModConfigSpec.IntValue AIR_STEP_JUMPS = BUILDER
            .comment("Air jumps at Air Step I before the next landing. Each higher level doubles it, through level IV.")
            .defineInRange("airStep.jumps", 12, 1, 10000);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> REGEN_INTERVALS = BUILDER
            .comment(
                    "Ticks between regeneration heals for levels I, II, III, and IV. Later levels use the last value.",
                    "One health point is half a heart."
            )
            .defineListAllowEmpty(
                    "vanilla.regeneration.intervals",
                    List.of(50, 25, 17, 13),
                    () -> 50,
                    value -> value instanceof Integer ticks && ticks >= 1 && ticks <= 72000
            );

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> POISON_INTERVALS = BUILDER
            .comment("Ticks between poison damage for levels I, II, III, and IV. Later levels use the last value.")
            .defineListAllowEmpty(
                    "vanilla.poison.intervals",
                    List.of(25, 20, 14, 9),
                    () -> 25,
                    value -> value instanceof Integer ticks && ticks >= 1 && ticks <= 72000
            );

    public static final ModConfigSpec.IntValue WITHER_INTERVAL = BUILDER
            .comment("Ticks between wither damage at level I. Each higher level halves the wait.")
            .defineInRange("vanilla.wither.interval", 40, 1, 72000);

    public static final ModConfigSpec.IntValue HEALING_BASE = BUILDER
            .comment(
                    "Instant Health restores base + perLevel * level health points. Level I is 1, so the default is 4, then 6, then 8.",
                    "A health point is half a heart. Lingering clouds apply half of that, once a second. Splash potions still scale it by distance."
            )
            .defineInRange("vanilla.healing.base", 2, 0, 10000);

    public static final ModConfigSpec.IntValue HEALING_PER_LEVEL = BUILDER
            .comment("Added to Instant Health for each potion level. Level I is 1.")
            .defineInRange("vanilla.healing.perLevel", 2, 0, 10000);

    public static final ModConfigSpec.IntValue HARMING_BASE = BUILDER
            .comment(
                    "Instant Damage deals base + perLevel * level health points. Level I is 1, so the default is 6, then 8, then 10.",
                    "Lingering clouds apply half of that, once a second. Splash potions still scale it by distance."
            )
            .defineInRange("vanilla.harming.base", 4, 0, 10000);

    public static final ModConfigSpec.IntValue HARMING_PER_LEVEL = BUILDER
            .comment("Added to Instant Damage for each potion level. Level I is 1.")
            .defineInRange("vanilla.harming.perLevel", 2, 0, 10000);

    public static final ModConfigSpec.DoubleValue SWIFTNESS_PERCENT = BUILDER
            .comment(
                    "Swiftness multiplies this percent by growth^level. Level I is 1, so the default is 32%, then 64%, then 128%.",
                    "The bonus is a multiplier on walking speed, the same way vanilla's 20% is."
            )
            .defineInRange("vanilla.swiftness.percent", 16.0, 0.0, 1000.0);

    public static final ModConfigSpec.DoubleValue SWIFTNESS_GROWTH = BUILDER
            .comment("Swiftness raises the percent by this factor once per potion level. 2 doubles it at each level.")
            .defineInRange("vanilla.swiftness.growth", 2.0, 0.0, 16.0);

    public static final ModConfigSpec.DoubleValue SLOWNESS_PERCENT = BUILDER
            .comment(
                    "Slowness subtracts this percent of movement speed per level. Level I is 1, so the default is 22%, then 44%, then 66%.",
                    "The penalty is a multiplier on walking speed, the same way vanilla's 15% is."
            )
            .defineInRange("vanilla.slowness.percent", 22.0, 0.0, 1000.0);

    public static final ModConfigSpec.IntValue STRENGTH_BASE = BUILDER
            .comment("Strength adds base + perLevel * level melee damage. Level I is 1, so the default is +3, then +5, then +7.")
            .defineInRange("vanilla.strength.base", 1, -1000, 1000);

    public static final ModConfigSpec.IntValue STRENGTH_PER_LEVEL = BUILDER
            .comment("Added to the Strength bonus for each potion level. Level I is 1.")
            .defineInRange("vanilla.strength.perLevel", 2, -1000, 1000);

    public static final ModConfigSpec.IntValue WEAKNESS_BASE = BUILDER
            .comment("Weakness subtracts base + perLevel * level melee damage. Level I is 1, so the default is -3, then -5, then -7.")
            .defineInRange("vanilla.weakness.base", 1, -1000, 1000);

    public static final ModConfigSpec.IntValue WEAKNESS_PER_LEVEL = BUILDER
            .comment("Added to the Weakness penalty for each potion level. Level I is 1.")
            .defineInRange("vanilla.weakness.perLevel", 2, -1000, 1000);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> QUIET_VOLUME = BUILDER
            .comment("Quiet volume scale for levels I through IV. Higher levels use the last value. Restart to apply.")
            .defineListAllowEmpty("quiet.volume", List.of(0.50, 0.20, 0.10, 0.05), () -> 0.50, BuffsPotionsConfig::scale);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> RIOT_VOLUME = BUILDER
            .comment("Riotous volume scale for levels I through IV. Higher levels use the last value. Restart to apply.")
            .defineListAllowEmpty("riot.volume", List.of(1.25, 1.50, 1.75, 2.00), () -> 1.25, BuffsPotionsConfig::scale);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> QUIET_SKULK = BUILDER
            .comment("Chance that Quiet ignores a sculk vibration, for levels I through IV.")
            .defineListAllowEmpty("quiet.skulkIgnore", List.of(0.50, 0.80, 0.90, 0.95), () -> 0.50, BuffsPotionsConfig::chance);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> QUIET_SAFE = BUILDER
            .comment("Passive-mob safe distance while Quiet, for levels I through IV. Vanilla is 24.")
            .defineListAllowEmpty("quiet.safeDistance", List.of(32, 40, 48, 56), () -> 32, BuffsPotionsConfig::distance);

    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> RIOT_SAFE = BUILDER
            .comment("Passive-mob safe distance while Riotous, for levels I through IV. Vanilla is 24.")
            .defineListAllowEmpty("riot.safeDistance", List.of(20, 16, 12, 8), () -> 20, BuffsPotionsConfig::distance);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> QUIET_HOSTILE = BUILDER
            .comment("Hostile mob cap multiplier while Quiet, for levels I through IV.")
            .defineListAllowEmpty("quiet.hostileCap", List.of(0.75, 0.55, 0.40, 0.25), () -> 0.75, BuffsPotionsConfig::scale);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> RIOT_HOSTILE = BUILDER
            .comment("Hostile mob cap multiplier while Riotous, for levels I through IV.")
            .defineListAllowEmpty("riot.hostileCap", List.of(1.25, 1.50, 1.75, 2.00), () -> 1.25, BuffsPotionsConfig::scale);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> QUIET_PASSIVE = BUILDER
            .comment("Passive mob cap multiplier while Quiet, for levels I through IV.")
            .defineListAllowEmpty("quiet.passiveCap", List.of(1.50, 2.00, 2.50, 3.00), () -> 1.50, BuffsPotionsConfig::scale);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> RIOT_PASSIVE = BUILDER
            .comment("Passive mob cap multiplier while Riotous, for levels I through IV.")
            .defineListAllowEmpty("riot.passiveCap", List.of(0.75, 0.50, 0.35, 0.25), () -> 0.75, BuffsPotionsConfig::scale);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> QUIET_SPAWNER = BUILDER
            .comment("Spawner nearby-mob cap multiplier while Quiet, for levels I through IV.")
            .defineListAllowEmpty("quiet.spawner", List.of(0.50, 0.40, 0.30, 0.20), () -> 0.50, BuffsPotionsConfig::scale);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> RIOT_SPAWNER = BUILDER
            .comment("Spawner nearby-mob cap multiplier while Riotous, for levels I through IV.")
            .defineListAllowEmpty("riot.spawner", List.of(1.50, 2.00, 2.50, 3.00), () -> 1.50, BuffsPotionsConfig::scale);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> QUIET_DELAY_SKIP = BUILDER
            .comment("Chance that Quiet adds one extra tick to a spawner delay, for levels I through IV.")
            .defineListAllowEmpty("quiet.delaySkip", List.of(0.34, 0.50, 0.67, 0.75), () -> 0.34, BuffsPotionsConfig::chance);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> RIOT_DELAY_HURRY = BUILDER
            .comment(
                    "Chance that Riotous removes one tick from a spawner delay, for levels I through IV.",
                    "1.0 removes that tick every time. Levels III and IV default to 1.0."
            )
            .defineListAllowEmpty("riot.delayHurry", List.of(0.25, 0.55, 1.0, 1.0), () -> 0.25, BuffsPotionsConfig::chance);

    public static final ModConfigSpec.ConfigValue<List<? extends Double>> RIOT_DELAY_AGAIN = BUILDER
            .comment("Chance that Riotous removes a second tick from a spawner delay, for levels I through IV.")
            .defineListAllowEmpty("riot.delayHurryAgain", List.of(0.0, 0.0, 0.0, 0.65), () -> 0.0, BuffsPotionsConfig::chance);

    public static final ModConfigSpec.DoubleValue STILL_FLOWER_CHANCE = BUILDER
            .comment(
                    "Chance each random tick that a flower pot sitting on a brewing still turns a plain mundane potion into that flower's potion.",
                    "The plain mundane potion is the one brewed from redstone into a water bottle. Restart to apply."
            )
            .defineInRange("still.flowerChance", 0.05, 0.0, 1.0);

    public static final ModConfigSpec.ConfigValue<List<? extends String>> STILL_FLOWER_POTIONS = BUILDER
            .comment(
                    "Optional pins for the per-world flower pot map, as namespace:block=namespace:potion.",
                    "Each world shuffles the vanilla pot plants across the mundane effects from its seed. These lines override that shuffle.",
                    "A potion id is used when one exists. Otherwise the id is read as a mob effect.",
                    "An empty right side, such as minecraft:poppy=, makes that plant do nothing. Parameterized effects are skipped.",
                    "Both eyeblossoms are shuffled with the other plants."
            )
            .defineListAllowEmpty(
                    "still.flowerPotions",
                    List.of(),
                    () -> "minecraft:poppy=minecraft:night_vision",
                    value -> value instanceof String line && line.contains("=")
            );

    public static final ModConfigSpec.ConfigValue<List<? extends String>> MOB_ITEMS = BUILDER
            .comment(
                    "Which item identifies a mob for incarnation and seeking, as namespace:entity=namespace:item.",
                    "Every mob is listed. An empty item, such as minecraft:allay=, means no item identifies that mob.",
                    "Restart to apply."
            )
            .defineListAllowEmpty(
                    "incarnation.mobItems",
                    MobDrops.defaultLines(),
                    () -> "minecraft:zombie=minecraft:rotten_flesh",
                    value -> value instanceof String line && line.contains("=")
            );

    public static final ModConfigSpec SPEC = BUILDER.build();

    private BuffsPotionsConfig() {
    }

    public static int baseDuration() {
        return SPEC.isLoaded() ? POTION_BASE_DURATION.get() : 3600;
    }

    public static int baseFor(String potionName) {
        Integer override = overrides().get(potionName);
        if (override != null) {
            return override;
        }
        return switch (potionName) {
            case "slowness", "weakness", "slow_falling" -> 1800;
            case "poison", "regeneration" -> 900;
            case "levitation" -> 20 * 45;
            case "turtle_master" -> 1200;
            case "luck" -> 6000;
            default -> baseDuration();
        };
    }

    public static int ticks(String potionName, String kind) {
        int base = baseFor(potionName);
        return switch (kind) {
            case "longDuration" -> Math.max(1, base * 2);
            case "strongDuration" -> Math.max(1, base / 2);
            default -> base;
        };
    }

    public static int longDuration() {
        return Math.max(1, baseDuration() * 2);
    }

    public static int strongDuration() {
        return Math.max(1, baseDuration() / 2);
    }

    public static int secondExtendDuration() {
        return Math.max(1, baseDuration() * 4);
    }

    public static int secondThickenDuration() {
        return Math.max(1, baseDuration() / 4);
    }

    public static int extendedThickDuration() {
        return Math.max(1, (int) Math.round(baseDuration() * 1.5));
    }

    public static double flowerSproutChance() {
        return SPEC.isLoaded() ? FLOWER_SPROUT.get() : 0.025;
    }

    public static double flowerInterestingChance() {
        return SPEC.isLoaded() ? FLOWER_INTERESTING.get() : 0.15;
    }

    public static double finesseDurabilityChance() {
        return SPEC.isLoaded() ? FINESSE_REMAINING.get() : 0.5;
    }

    public static int fumblingFactor() {
        return SPEC.isLoaded() ? FUMBLING_FACTOR.get() : 2;
    }

    public static double reachingBlocks() {
        return SPEC.isLoaded() ? REACHING_BLOCKS.get() : 2.0;
    }

    public static int airStepJumps() {
        return SPEC.isLoaded() ? AIR_STEP_JUMPS.get() : 12;
    }

    public static int[] regenerationIntervals() {
        return ints(REGEN_INTERVALS, new int[] {50, 25, 17, 13});
    }

    public static int[] poisonIntervals() {
        return ints(POISON_INTERVALS, new int[] {25, 20, 14, 9});
    }

    public static int witherInterval() {
        return SPEC.isLoaded() ? WITHER_INTERVAL.get() : 40;
    }

    public static int healing(int level) {
        int base = SPEC.isLoaded() ? HEALING_BASE.get() : 2;
        int perLevel = SPEC.isLoaded() ? HEALING_PER_LEVEL.get() : 2;
        return Math.max(0, linear(base, perLevel, level));
    }

    public static int harming(int level) {
        int base = SPEC.isLoaded() ? HARMING_BASE.get() : 4;
        int perLevel = SPEC.isLoaded() ? HARMING_PER_LEVEL.get() : 2;
        return Math.max(0, linear(base, perLevel, level));
    }

    public static double swiftnessBonus(int level) {
        double percent = SPEC.isLoaded() ? SWIFTNESS_PERCENT.get() : 16.0;
        double growth = SPEC.isLoaded() ? SWIFTNESS_GROWTH.get() : 2.0;
        return percent / 100.0 * Math.pow(growth, level);
    }

    public static double slownessPenalty(int level) {
        double percent = SPEC.isLoaded() ? SLOWNESS_PERCENT.get() : 22.0;
        return percent / 100.0 * Math.max(0, level);
    }

    public static double strengthBonus(int level) {
        int base = SPEC.isLoaded() ? STRENGTH_BASE.get() : 1;
        int perLevel = SPEC.isLoaded() ? STRENGTH_PER_LEVEL.get() : 2;
        return linear(base, perLevel, level);
    }

    public static double weaknessPenalty(int level) {
        int base = SPEC.isLoaded() ? WEAKNESS_BASE.get() : 1;
        int perLevel = SPEC.isLoaded() ? WEAKNESS_PER_LEVEL.get() : 2;
        return linear(base, perLevel, level);
    }

    private static int linear(int base, int perLevel, int level) {
        long value = (long) base + (long) perLevel * Math.max(0, level);
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    private static Map<String, Integer> overrides() {
        Map<String, Integer> parsed = new HashMap<>();
        if (!SPEC.isLoaded()) {
            return parsed;
        }
        for (String line : POTION_BASE_OVERRIDES.get()) {
            int split = line.indexOf('=');
            if (split <= 0) {
                continue;
            }
            try {
                int ticks = Integer.parseInt(line.substring(split + 1).trim());
                if (ticks > 0) {
                    parsed.put(line.substring(0, split).trim(), ticks);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return parsed;
    }

    public static int[] senseRadii() {
        return ints(SENSE_RADII, new int[] {10, 13, 16, 19});
    }

    public static int stackBase() {
        return SPEC.isLoaded() ? STACK_BASE.get() : 2;
    }

    public static int crackBase() {
        return SPEC.isLoaded() ? CRACK_BASE.get() : 2;
    }

    public static double eatChancePerLevel() {
        return SPEC.isLoaded() ? EAT_CHANCE.get() : 0.1;
    }

    public static int attractBlocksPerLevel() {
        return SPEC.isLoaded() ? ATTRACT_BLOCKS.get() : 3;
    }

    public static double attractSpeed() {
        return SPEC.isLoaded() ? ATTRACT_SPEED.get() : 0.08;
    }

    public static double attractGain() {
        return SPEC.isLoaded() ? ATTRACT_GAIN.get() : 0.80;
    }

    public static int vomitRangePerLevel() {
        return SPEC.isLoaded() ? VOMIT_RANGE.get() : 5;
    }

    public static int[] vomitDepositIntervals() {
        return ints(VOMIT_DEPOSIT, new int[] {64, 16, 4, 1});
    }

    public static int[] vomitThrowIntervals() {
        return ints(VOMIT_THROW, new int[] {16, 8, 4, 2});
    }

    public static int seekRangePerLevel() {
        return SPEC.isLoaded() ? SEEK_RANGE.get() : 16;
    }

    public static List<? extends String> mobItems() {
        return SPEC.isLoaded() ? MOB_ITEMS.get() : MobDrops.defaultLines();
    }

    public static float[] quietVolume() {
        return floats(QUIET_VOLUME, new float[] {0.50F, 0.20F, 0.10F, 0.05F});
    }

    public static float[] riotVolume() {
        return floats(RIOT_VOLUME, new float[] {1.25F, 1.50F, 1.75F, 2.00F});
    }

    public static float[] quietSkulk() {
        return floats(QUIET_SKULK, new float[] {0.50F, 0.80F, 0.90F, 0.95F});
    }

    public static int[] quietSafe() {
        return ints(QUIET_SAFE, new int[] {32, 40, 48, 56});
    }

    public static int[] riotSafe() {
        return ints(RIOT_SAFE, new int[] {20, 16, 12, 8});
    }

    public static float[] quietHostile() {
        return floats(QUIET_HOSTILE, new float[] {0.75F, 0.55F, 0.40F, 0.25F});
    }

    public static float[] riotHostile() {
        return floats(RIOT_HOSTILE, new float[] {1.25F, 1.50F, 1.75F, 2.00F});
    }

    public static float[] quietPassive() {
        return floats(QUIET_PASSIVE, new float[] {1.50F, 2.00F, 2.50F, 3.00F});
    }

    public static float[] riotPassive() {
        return floats(RIOT_PASSIVE, new float[] {0.75F, 0.50F, 0.35F, 0.25F});
    }

    public static float[] quietSpawner() {
        return floats(QUIET_SPAWNER, new float[] {0.50F, 0.40F, 0.30F, 0.20F});
    }

    public static float[] riotSpawner() {
        return floats(RIOT_SPAWNER, new float[] {1.50F, 2.00F, 2.50F, 3.00F});
    }

    public static float[] quietDelaySkip() {
        return floats(QUIET_DELAY_SKIP, new float[] {0.34F, 0.50F, 0.67F, 0.75F});
    }

    public static float[] riotDelayHurry() {
        return floats(RIOT_DELAY_HURRY, new float[] {0.25F, 0.55F, 1.0F, 1.0F});
    }

    public static float[] riotDelayAgain() {
        return floats(RIOT_DELAY_AGAIN, new float[] {0.0F, 0.0F, 0.0F, 0.65F});
    }

    public static double flowerStillChance() {
        return SPEC.isLoaded() ? STILL_FLOWER_CHANCE.get() : 0.05;
    }

    public static List<? extends String> flowerPotions() {
        return SPEC.isLoaded() ? STILL_FLOWER_POTIONS.get() : List.of();
    }

    private static int[] ints(ModConfigSpec.ConfigValue<List<? extends Integer>> value, int[] fallback) {
        if (!SPEC.isLoaded()) {
            return fallback;
        }
        List<? extends Integer> configured = value.get();
        if (configured.isEmpty()) {
            return fallback;
        }
        int[] radii = new int[configured.size()];
        for (int i = 0; i < configured.size(); i++) {
            radii[i] = configured.get(i);
        }
        return radii;
    }

    private static float[] floats(ModConfigSpec.ConfigValue<List<? extends Double>> value, float[] fallback) {
        if (!SPEC.isLoaded()) {
            return fallback;
        }
        List<? extends Double> configured = value.get();
        if (configured.isEmpty()) {
            return fallback;
        }
        float[] scales = new float[configured.size()];
        for (int i = 0; i < configured.size(); i++) {
            scales[i] = configured.get(i).floatValue();
        }
        return scales;
    }

    private static boolean scale(Object value) {
        return value instanceof Number number && number.doubleValue() >= 0.0 && number.doubleValue() <= 64.0;
    }

    private static boolean chance(Object value) {
        return value instanceof Number number && number.doubleValue() >= 0.0 && number.doubleValue() <= 1.0;
    }

    private static boolean distance(Object value) {
        return value instanceof Integer blocks && blocks >= 1 && blocks <= 512;
    }
}
