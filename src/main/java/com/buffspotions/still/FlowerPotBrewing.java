package com.buffspotions.still;

import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.SuspiciousBrew;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModPotions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;

/**
 * A flower pot on a still turns suspicious brew into a mundane potion.
 * The overworld seed shuffles the vanilla pot plants, including both eyeblossoms, across the effect list.
 */
public final class FlowerPotBrewing {
    public static final String OPEN_EYEBLOSSOM = "minecraft:open_eyeblossom";
    /** Vanilla pot plants, including the golden dandelion. */
    public static final List<String> PLANTS = List.of(
            "minecraft:acacia_sapling",
            "minecraft:allium",
            "minecraft:azalea",
            "minecraft:azure_bluet",
            "minecraft:bamboo",
            "minecraft:birch_sapling",
            "minecraft:blue_orchid",
            "minecraft:brown_mushroom",
            "minecraft:cactus",
            "minecraft:cherry_sapling",
            "minecraft:closed_eyeblossom",
            "minecraft:cornflower",
            "minecraft:crimson_fungus",
            "minecraft:crimson_roots",
            "minecraft:dandelion",
            "minecraft:dark_oak_sapling",
            "minecraft:dead_bush",
            "minecraft:fern",
            "minecraft:flowering_azalea",
            "minecraft:golden_dandelion",
            "minecraft:jungle_sapling",
            "minecraft:lily_of_the_valley",
            "minecraft:mangrove_propagule",
            "minecraft:oak_sapling",
            "minecraft:open_eyeblossom",
            "minecraft:orange_tulip",
            "minecraft:oxeye_daisy",
            "minecraft:pale_oak_sapling",
            "minecraft:pink_tulip",
            "minecraft:poppy",
            "minecraft:red_mushroom",
            "minecraft:red_tulip",
            "minecraft:spruce_sapling",
            "minecraft:torchflower",
            "minecraft:warped_fungus",
            "minecraft:warped_roots",
            "minecraft:white_tulip",
            "minecraft:wither_rose"
    );
    /** Mundane results. One entry for each plant, including open eyeblossom. */
    public static final List<String> EFFECTS = List.of(
            "minecraft:absorption",
            "minecraft:fire_resistance",
            "minecraft:hunger",
            "minecraft:infested",
            "minecraft:harming",
            "minecraft:healing",
            "minecraft:invisibility",
            "minecraft:leaping",
            "minecraft:levitation",
            "minecraft:luck",
            "minecraft:mining_fatigue",
            "minecraft:night_vision",
            "minecraft:oozing",
            "minecraft:poison",
            "minecraft:regeneration",
            "minecraft:resistance",
            "minecraft:slowness",
            "minecraft:slow_falling",
            "minecraft:swiftness",
            "minecraft:strength",
            "minecraft:water_breathing",
            "minecraft:weakness",
            "minecraft:weaving",
            "buffs_potions:harvesting",
            "minecraft:unluck",
            "buffs_potions:reaching",
            "buffs_potions:finesse",
            "buffs_potions:fumbling",
            "minecraft:wind_charged",
            "buffs_potions:heights",
            "buffs_potions:depths",
            "buffs_potions:partial_visitation",
            "buffs_potions:wind_jumping",
            "buffs_potions:insight",
            "buffs_potions:instant_confusion",
            "buffs_potions:warding",
            "buffs_potions:block_stacking",
            "buffs_potions:block_cracking"
    );

    private FlowerPotBrewing() {
    }

    public static boolean isSuspiciousBrew(ItemStack stack) {
        return stack.is(Items.POTION) && SuspiciousBrew.is(stack);
    }

    public static Map<String, String> roll(long seed) {
        List<String> effects = new ArrayList<>(EFFECTS);
        RandomSource random = RandomSource.create(seed);
        for (int index = effects.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            String held = effects.get(index);
            effects.set(index, effects.get(swap));
            effects.set(swap, held);
        }
        Map<String, String> assigned = new HashMap<>();
        for (int index = 0; index < PLANTS.size(); index++) {
            assigned.put(PLANTS.get(index), effects.get(index));
        }
        return assigned;
    }

    public static String unknown(long seed, String plant) {
        List<String> effects = unknownEffects();
        long mixed = seed ^ (plant.hashCode() * 0x9E3779B97F4A7C15L);
        return effects.get(RandomSource.create(mixed).nextInt(effects.size()));
    }

    public static String target(long seed, Map<String, String> assigned, String plant) {
        if (plant == null || plant.isBlank()) {
            return null;
        }
        String pin = pin(plant);
        if (pin != null) {
            return pin.isEmpty() ? null : pin;
        }
        String known = assigned.get(plant);
        return known != null ? known : unknown(seed, plant);
    }

    public static boolean mapped(net.minecraft.server.level.ServerLevel level, Identifier plant) {
        return plant != null && target(level, plant.toString()) != null;
    }

    public static ItemStack brew(net.minecraft.server.level.ServerLevel level, Identifier plant) {
        if (plant == null) {
            return ItemStack.EMPTY;
        }
        String chosen = target(level, plant.toString());
        return chosen == null ? ItemStack.EMPTY : fromTarget(chosen);
    }

    public static ItemStack brew(long seed, Block plant) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(plant);
        String chosen = target(seed, roll(seed), id.toString());
        return chosen == null ? ItemStack.EMPTY : fromTarget(chosen);
    }

    public static ItemStack fromTarget(String idText) {
        Identifier id;
        try {
            id = Identifier.parse(idText);
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
        var potions = BuiltInRegistries.POTION.get(id);
        if (potions.isPresent()) {
            Holder<Potion> holder = potions.get();
            if (parameterizedPotion(holder)) {
                return ItemStack.EMPTY;
            }
            if (holder.unwrapKey().equals(ModPotions.LATENT_EYEDENTIFICATION.unwrapKey())) {
                return EyedBrewing.latentBottle();
            }
            ItemStack stack = PotionContents.createItemStack(Items.POTION, holder);
            stack.set(ModDataComponents.MUNDANE.get(), true);
            return stack;
        }
        var effects = BuiltInRegistries.MOB_EFFECT.get(id);
        if (effects.isEmpty() || parameterized(effects.get())) {
            return ItemStack.EMPTY;
        }
        Holder<MobEffect> effect = effects.get();
        String name = effect.unwrapKey().map(key -> key.identifier().getPath()).orElse("empty");
        int duration = effect.value().isInstantaneous() ? 1 : BuffsPotionsConfig.baseFor(name);
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.empty(),
                Optional.empty(),
                List.of(new MobEffectInstance(effect, duration)),
                Optional.of(name)
        ));
        stack.set(ModDataComponents.MUNDANE.get(), true);
        return stack;
    }

    private static String target(net.minecraft.server.level.ServerLevel level, String plant) {
        FlowerPotAssignments assignments = FlowerPotAssignments.get(level.getServer());
        if (!assignments.ready()) {
            return null;
        }
        return target(assignments.seed(), assignments.plants(), plant);
    }

    private static List<String> unknownEffects() {
        return EFFECTS;
    }

    private static String pin(String plant) {
        for (String line : BuffsPotionsConfig.flowerPotions()) {
            int split = line.indexOf('=');
            if (split <= 0) {
                continue;
            }
            String block = line.substring(0, split).trim();
            if (!block.equals(plant)) {
                continue;
            }
            return line.substring(split + 1).trim();
        }
        return null;
    }

    private static boolean parameterizedPotion(Holder<Potion> potion) {
        for (MobEffectInstance instance : potion.value().getEffects()) {
            if (parameterized(instance.getEffect())) {
                return true;
            }
        }
        return false;
    }

    private static boolean parameterized(Holder<MobEffect> effect) {
        return effect.unwrapKey().equals(ModEffects.BLOCK_SENSE.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.ITEM_EATER.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.INSTANT_INCARNATION.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.INSTANT_FORMATION.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.ITEM_VOMITING.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.SEEKING.unwrapKey());
    }
}
