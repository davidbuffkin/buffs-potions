package com.buffspotions.structure;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModPotions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

/** Rolls for the apothecary's pots, shelves, flagons, and the two chest tables. */
public final class ApothecaryLoot {
    public static final ResourceKey<LootTable> LOWER_CHEST = chest("apothecary_lower");
    public static final ResourceKey<LootTable> UPPER_CHEST = chest("apothecary_upper");
    /** The saved chest sits on the ground floor. A chest at this height or above is the upstairs one. */
    public static final int UPPER_FLOOR_Y = 6;

    private static final List<Weighted<Block>> POTS = List.of(
            pot(Blocks.POTTED_OAK_SAPLING, 1),
            pot(Blocks.POTTED_SPRUCE_SAPLING, 1),
            pot(Blocks.POTTED_BIRCH_SAPLING, 1),
            pot(Blocks.POTTED_JUNGLE_SAPLING, 1),
            pot(Blocks.POTTED_ACACIA_SAPLING, 1),
            pot(Blocks.POTTED_DARK_OAK_SAPLING, 1),
            pot(Blocks.POTTED_MANGROVE_PROPAGULE, 1),
            pot(Blocks.POTTED_CHERRY_SAPLING, 1),
            pot(Blocks.POTTED_PALE_OAK_SAPLING, 1),
            pot(Blocks.POTTED_AZALEA, 3),
            pot(Blocks.POTTED_FLOWERING_AZALEA, 3),
            pot(Blocks.POTTED_DANDELION, 3),
            pot(Blocks.POTTED_POPPY, 3),
            pot(Blocks.POTTED_ALLIUM, 3),
            pot(Blocks.POTTED_AZURE_BLUET, 3),
            pot(Blocks.POTTED_RED_TULIP, 3),
            pot(Blocks.POTTED_WHITE_TULIP, 3),
            pot(Blocks.POTTED_PINK_TULIP, 3),
            pot(Blocks.POTTED_ORANGE_TULIP, 3),
            pot(Blocks.POTTED_OXEYE_DAISY, 3),
            pot(Blocks.POTTED_CORNFLOWER, 3),
            pot(Blocks.POTTED_LILY_OF_THE_VALLEY, 3),
            pot(Blocks.POTTED_BAMBOO, 3),
            pot(Blocks.POTTED_CACTUS, 3)
    );
    private static final List<Weighted<ShelfRoll>> SHELF = List.of(
            new Weighted<>(ShelfRoll.EMPTY, 20),
            new Weighted<>(ShelfRoll.WATER, 5),
            new Weighted<>(ShelfRoll.HONEY, 5),
            new Weighted<>(ShelfRoll.SPLASH_MILK, 5),
            new Weighted<>(ShelfRoll.EXPERIENCE, 4),
            new Weighted<>(ShelfRoll.SUSPICIOUS, 5),
            new Weighted<>(ShelfRoll.SLOWNESS_REMEDY, 1),
            new Weighted<>(ShelfRoll.POISON_REMEDY, 5),
            new Weighted<>(ShelfRoll.WEAKNESS_REMEDY, 2),
            new Weighted<>(ShelfRoll.WARDING, 5)
    );
    private static final List<Weighted<FlagonRoll>> FLAGON = List.of(
            new Weighted<>(FlagonRoll.HARVESTING, 3),
            new Weighted<>(FlagonRoll.WIND_WALKING, 1),
            new Weighted<>(FlagonRoll.CONFOUNDING, 1),
            new Weighted<>(FlagonRoll.ACUITY, 1),
            new Weighted<>(FlagonRoll.SWIFTNESS, 1),
            new Weighted<>(FlagonRoll.NIGHT_VISION, 1),
            new Weighted<>(FlagonRoll.BAT, 1),
            new Weighted<>(FlagonRoll.DIAMOND_ORE, 1),
            new Weighted<>(FlagonRoll.HONEY, 1),
            new Weighted<>(FlagonRoll.MILK, 1)
    );
    private static final List<BarrelSupply> BARREL = List.of(
            BarrelSupply.BEETROOT,
            BarrelSupply.CARROT,
            BarrelSupply.HONEYCOMB,
            BarrelSupply.BOTTLE,
            BarrelSupply.BEETROOT_SEEDS,
            BarrelSupply.WATER,
            BarrelSupply.COPPER_NUGGET,
            BarrelSupply.GOLD_NUGGET
    );

    private ApothecaryLoot() {
    }

    public static ResourceKey<LootTable> chest(int templateY) {
        return templateY >= UPPER_FLOOR_Y ? UPPER_CHEST : LOWER_CHEST;
    }

    public static BlockState flowerPot(RandomSource random) {
        return pick(POTS, random).defaultBlockState();
    }

    public static ItemStack shelfSlot(RandomSource random) {
        return switch (pick(SHELF, random)) {
            case EMPTY -> ItemStack.EMPTY;
            case WATER -> PotionContents.createItemStack(Items.POTION, Potions.WATER);
            case HONEY -> new ItemStack(Items.HONEY_BOTTLE);
            case SPLASH_MILK -> PotionContents.createItemStack(Items.SPLASH_POTION, ModPotions.MILK);
            case EXPERIENCE -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case SUSPICIOUS -> PotionContents.createItemStack(Items.POTION, ModPotions.SUSPICIOUS_BREW);
            case SLOWNESS_REMEDY -> Remedies.create(MobEffects.SLOWNESS, Items.POTION);
            case POISON_REMEDY -> Remedies.create(MobEffects.POISON, Items.POTION);
            case WEAKNESS_REMEDY -> Remedies.create(MobEffects.WEAKNESS, Items.POTION);
            case WARDING -> mundane(ModPotions.WARDING);
        };
    }

    public static FlagonContents flagon(RandomSource random) {
        int doses = random.nextInt(4) + 1;
        ItemStack dose = switch (pick(FLAGON, random)) {
            case HARVESTING -> mundane(ModPotions.HARVESTING);
            case WIND_WALKING -> mundane(ModPotions.WIND_JUMPING);
            case CONFOUNDING -> mundane(ModPotions.INSTANT_CONFUSION);
            case ACUITY -> mundane(ModPotions.INSIGHT);
            case SWIFTNESS -> mundane(Potions.SWIFTNESS);
            case NIGHT_VISION -> mundane(Potions.NIGHT_VISION);
            case BAT -> incarnation();
            case DIAMOND_ORE -> formation();
            case HONEY -> new ItemStack(Items.HONEY_BOTTLE);
            case MILK -> FlagonContents.milkTemplate();
        };
        return new FlagonContents(dose, doses);
    }

    /** Two to four singles of one item. Each one is its own stack so a barrel can scatter them. */
    public static List<ItemStack> barrelSupply(RandomSource random) {
        int count = random.nextInt(3) + 2;
        BarrelSupply supply = BARREL.get(random.nextInt(BARREL.size()));
        List<ItemStack> stacks = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            stacks.add(supply == BarrelSupply.WATER
                    ? PotionContents.createItemStack(Items.POTION, Potions.WATER)
                    : new ItemStack(supply.item));
        }
        return stacks;
    }

    private static ItemStack mundane(Holder<Potion> potion) {
        ItemStack stack = PotionContents.createItemStack(Items.POTION, potion);
        stack.set(ModDataComponents.MUNDANE.get(), true);
        return stack;
    }

    private static ItemStack incarnation() {
        ItemStack stack = PotionContents.createItemStack(Items.POTION, ModPotions.INCARNATION);
        stack.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofEntity(entity("minecraft:bat")));
        return stack;
    }

    private static ItemStack formation() {
        ItemStack stack = PotionContents.createItemStack(Items.POTION, ModPotions.FORMATION);
        stack.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofItem(Items.DIAMOND_ORE.builtInRegistryHolder()));
        return stack;
    }

    private static Holder<EntityType<?>> entity(String id) {
        return BuiltInRegistries.ENTITY_TYPE.get(Identifier.parse(id)).orElseThrow();
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

    private static Weighted<Block> pot(Block block, int weight) {
        return new Weighted<>(block, weight);
    }

    private static ResourceKey<LootTable> chest(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "chests/" + path));
    }

    private record Weighted<T>(T value, int weight) {
    }

    private enum ShelfRoll {
        EMPTY,
        WATER,
        HONEY,
        SPLASH_MILK,
        EXPERIENCE,
        SUSPICIOUS,
        SLOWNESS_REMEDY,
        POISON_REMEDY,
        WEAKNESS_REMEDY,
        WARDING
    }

    private enum FlagonRoll {
        HARVESTING,
        WIND_WALKING,
        CONFOUNDING,
        ACUITY,
        SWIFTNESS,
        NIGHT_VISION,
        BAT,
        DIAMOND_ORE,
        HONEY,
        MILK
    }

    private enum BarrelSupply {
        BEETROOT(Items.BEETROOT),
        CARROT(Items.CARROT),
        HONEYCOMB(Items.HONEYCOMB),
        BOTTLE(Items.GLASS_BOTTLE),
        BEETROOT_SEEDS(Items.BEETROOT_SEEDS),
        WATER(Items.POTION),
        COPPER_NUGGET(Items.COPPER_NUGGET),
        GOLD_NUGGET(Items.GOLD_NUGGET);

        private final Item item;

        BarrelSupply(Item item) {
            this.item = item;
        }
    }
}
