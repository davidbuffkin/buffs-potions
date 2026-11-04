package com.buffspotions.structure;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModPotions;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.loot.LootTable;

/** Rolls for the evil apothecary's shelves, flagons, and chest. */
public final class EvilApothecaryLoot {
    public static final ResourceKey<LootTable> CHEST = ResourceKey.create(
            Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "chests/evil_apothecary")
    );
    /** A shelf block holds three items. The two shelves in the house are six slots. */
    public static final int SHELF_SLOTS = 3;

    private static final List<Weighted<ShelfRoll>> SHELF = List.of(
            new Weighted<>(ShelfRoll.EMPTY, 20),
            new Weighted<>(ShelfRoll.LONG_FIRE_RESISTANCE, 5),
            new Weighted<>(ShelfRoll.REGENERATION, 5),
            new Weighted<>(ShelfRoll.STRENGTH, 5),
            new Weighted<>(ShelfRoll.CONFOUNDING, 5),
            new Weighted<>(ShelfRoll.AIR_STEP, 5),
            new Weighted<>(ShelfRoll.DISLOCATED, 5),
            new Weighted<>(ShelfRoll.DESCENT, 5),
            new Weighted<>(ShelfRoll.LATENT, 5)
    );
    private static final List<Weighted<FlagonRoll>> FLAGON = List.of(
            new Weighted<>(FlagonRoll.PARTIAL_VISITATION, 10),
            new Weighted<>(FlagonRoll.PIGLIN_BRUTE, 3),
            new Weighted<>(FlagonRoll.STRIDER, 3),
            new Weighted<>(FlagonRoll.POISON, 5),
            new Weighted<>(FlagonRoll.WEAKNESS, 5),
            new Weighted<>(FlagonRoll.QUARTZ_SENSE, 5),
            new Weighted<>(FlagonRoll.OBSIDIAN_SEEKING, 5),
            new Weighted<>(FlagonRoll.FIRE_RESISTANCE, 5),
            new Weighted<>(FlagonRoll.FINESSE, 5)
    );

    private EvilApothecaryLoot() {
    }

    public static ItemStack shelfSlot(RandomSource random) {
        return switch (pick(SHELF, random)) {
            case EMPTY -> ItemStack.EMPTY;
            case LONG_FIRE_RESISTANCE -> potion(Potions.LONG_FIRE_RESISTANCE);
            // Strong regeneration is 0:22. Four times that length is the requested 1:30.
            case REGENERATION -> scaled(potion(Potions.STRONG_REGENERATION), 4.0F);
            case STRENGTH -> potion(Potions.STRONG_STRENGTH);
            case CONFOUNDING -> potion(ModPotions.INSTANT_CONFUSION);
            case AIR_STEP -> potion(ModPotions.LONG_WIND_JUMPING);
            case DISLOCATED -> potion(ModPotions.STRONG_PARTIAL_VISITATION);
            case DESCENT -> potion(ModPotions.STRONG_DEPTHS);
            case LATENT -> colored(ModPotions.LATENT_EYEDENTIFICATION, EyedBrewing.LATENT_COLOR);
        };
    }

    public static FlagonContents flagon(RandomSource random) {
        int doses = random.nextInt(4) + 1;
        ItemStack dose = switch (pick(FLAGON, random)) {
            case PARTIAL_VISITATION -> potion(ModPotions.STRONG_PARTIAL_VISITATION);
            case PIGLIN_BRUTE -> incarnation("minecraft:piglin_brute");
            case STRIDER -> incarnation("minecraft:strider");
            case POISON -> potion(Potions.STRONG_POISON);
            case WEAKNESS -> potion(ModPotions.STRONG_WEAKNESS);
            case QUARTZ_SENSE -> essence(ModPotions.STRONG_BLOCK_PERCEPTION, Items.NETHER_QUARTZ_ORE);
            case OBSIDIAN_SEEKING -> essence(ModPotions.STRONG_SEEKING, Items.OBSIDIAN);
            case FIRE_RESISTANCE -> potion(Potions.FIRE_RESISTANCE);
            case FINESSE -> potion(ModPotions.STRONG_FINESSE);
        };
        return new FlagonContents(dose, doses);
    }

    private static ItemStack potion(Holder<Potion> potion) {
        return PotionContents.createItemStack(Items.POTION, potion);
    }

    private static ItemStack colored(Holder<Potion> potion, int color) {
        ItemStack stack = new ItemStack(Items.POTION);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.of(potion),
                Optional.of(color),
                List.of(),
                Optional.empty()
        ));
        return stack;
    }

    private static ItemStack scaled(ItemStack stack, float scale) {
        stack.set(DataComponents.POTION_DURATION_SCALE, scale);
        return stack;
    }

    private static ItemStack essence(Holder<Potion> potion, Item item) {
        ItemStack stack = potion(potion);
        stack.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofItem(item.builtInRegistryHolder()));
        return stack;
    }

    private static ItemStack incarnation(String entityId) {
        ItemStack stack = potion(ModPotions.INCARNATION);
        Holder<EntityType<?>> entity = BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(entityId)).orElseThrow();
        stack.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofEntity(entity));
        return stack;
    }

    private static <T> T pick(List<Weighted<T>> options, RandomSource random) {
        int total = 0;
        for (Weighted<T> option : options) {
            total += option.weight;
        }
        int roll = random.nextInt(total);
        for (Weighted<T> option : options) {
            roll -= option.weight;
            if (roll < 0) {
                return option.value;
            }
        }
        return options.get(options.size() - 1).value;
    }

    private record Weighted<T>(T value, int weight) {
    }

    private enum ShelfRoll {
        EMPTY,
        LONG_FIRE_RESISTANCE,
        REGENERATION,
        STRENGTH,
        CONFOUNDING,
        AIR_STEP,
        DISLOCATED,
        DESCENT,
        LATENT
    }

    private enum FlagonRoll {
        PARTIAL_VISITATION,
        PIGLIN_BRUTE,
        STRIDER,
        POISON,
        WEAKNESS,
        QUARTZ_SENSE,
        OBSIDIAN_SEEKING,
        FIRE_RESISTANCE,
        FINESSE
    }
}
