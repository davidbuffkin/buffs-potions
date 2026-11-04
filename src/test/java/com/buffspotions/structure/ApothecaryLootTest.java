package com.buffspotions.structure;

import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.potion.Remedies;
import com.buffspotions.potion.SuspiciousBrew;
import com.buffspotions.registry.ModBlocks;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModPotions;
import com.buffspotions.still.BrewingStillBlock;
import com.buffspotions.still.BrewingStillBlockEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class ApothecaryLootTest {
    @Test
    void potsShelvesAndFlagonsStayInsideTheirPools(MinecraftServer server) {
        RandomSource random = RandomSource.create(7);
        Set<String> pots = new HashSet<>();
        boolean shelfEmpty = false;
        boolean shelfFilled = false;
        boolean mundaneFlagon = false;
        boolean bat = false;
        boolean diamond = false;
        boolean honey = false;
        boolean milk = false;
        for (int i = 0; i < 400; i++) {
            BlockState pot = ApothecaryLoot.flowerPot(random);
            assertFalse(pot.is(Blocks.FLOWER_POT));
            assertFalse(pot.isAir());
            pots.add(BuiltInRegistries.BLOCK.getKey(pot.getBlock()).toString());
            ItemStack slot = ApothecaryLoot.shelfSlot(random);
            if (slot.isEmpty()) {
                shelfEmpty = true;
            } else {
                shelfFilled = true;
                assertTrue(knownShelfItem(slot));
            }
            FlagonContents contents = ApothecaryLoot.flagon(random);
            assertTrue(contents.doses() >= 1 && contents.doses() <= 4);
            if (MundaneBrewing.isMundane(contents.dose())) {
                mundaneFlagon = true;
            }
            var essence = contents.dose().get(ModDataComponents.ESSENCE.get());
            if (essence != null && essence.entity().isPresent()) {
                bat = true;
                assertFalse(MundaneBrewing.isMundane(contents.dose()));
            }
            if (essence != null && essence.item().isPresent()) {
                diamond = true;
                assertTrue(essence.item().get().value() == Items.DIAMOND_ORE);
            }
            honey |= contents.isHoney();
            milk |= contents.isMilk();
        }
        assertTrue(shelfEmpty);
        assertTrue(shelfFilled);
        assertTrue(mundaneFlagon);
        assertTrue(bat);
        assertTrue(diamond);
        assertTrue(honey);
        assertTrue(milk);
        assertTrue(pots.contains("minecraft:potted_oak_sapling"));
        assertTrue(pots.contains("minecraft:potted_dandelion"));
        assertTrue(pots.contains("minecraft:potted_cactus"));
    }

    @Test
    void lowerChestIsTheGroundFloor(MinecraftServer server) {
        assertEquals(ApothecaryLoot.LOWER_CHEST, ApothecaryLoot.chest(2));
        assertEquals(ApothecaryLoot.UPPER_CHEST, ApothecaryLoot.chest(ApothecaryLoot.UPPER_FLOOR_Y));
    }

    @Test
    void savedCreaturesLoseTheFreeze(MinecraftServer server) {
        CompoundTag villager = new CompoundTag();
        villager.putString("id", "minecraft:villager");
        villager.putBoolean("NoAI", true);
        villager.put("Brain", new CompoundTag());
        villager.put("Offers", new CompoundTag());
        CompoundTag stripped = process(villager).nbt;
        assertFalse(stripped.getBooleanOr("NoAI", false));
        assertTrue(stripped.getCompound("Brain").isEmpty());
        assertTrue(stripped.getCompound("Offers").isEmpty());
        assertTrue(stripped.getBooleanOr("PersistenceRequired", false));
        assertEquals(1, stripped.getIntOr("Xp", 0));
        CompoundTag data = stripped.getCompound("VillagerData").orElseThrow();
        assertEquals("minecraft:cleric", data.getStringOr("profession", ""));
        assertEquals(1, data.getIntOr("level", 0));

        CompoundTag golem = new CompoundTag();
        golem.putString("id", "minecraft:copper_golem");
        golem.putBoolean("NoAI", true);
        CompoundTag kept = new CompoundTag();
        kept.putBoolean("given", true);
        golem.put("neoforge:attachments", kept);
        CompoundTag woken = process(golem).nbt;
        assertFalse(woken.getBooleanOr("NoAI", false));
        assertTrue(woken.getCompound("neoforge:attachments").isPresent());
    }

    @Test
    void flagonContentsRoundTripThroughTheStructureTag(MinecraftServer server) {
        FlagonContents contents = ApothecaryLoot.flagon(RandomSource.create(3));
        CompoundTag written = ApothecaryProcessor.flagonContents(server.registryAccess(), new CompoundTag(), contents);
        FlagonContents stored = FlagonContents.CODEC.parse(
                server.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                written.get("Contents")
        ).getOrThrow();
        assertEquals(contents.doses(), stored.doses());
        assertTrue(ItemStack.isSameItemSameComponents(contents.dose(), stored.dose()));
    }

    @Test
    void chestsStoreALootTableAndStillsKeepTheirFuel(MinecraftServer server) {
        CompoundTag lower = ApothecaryProcessor.chestLoot(new CompoundTag(), 2, RandomSource.create(1));
        CompoundTag upper = ApothecaryProcessor.chestLoot(new CompoundTag(), 7, RandomSource.create(1));
        assertEquals("buffs_potions:chests/apothecary_lower", lower.getStringOr("LootTable", ""));
        assertEquals("buffs_potions:chests/apothecary_upper", upper.getStringOr("LootTable", ""));

        CompoundTag still = new CompoundTag();
        ItemStack charcoal = new ItemStack(Items.CHARCOAL, 2);
        CompoundTag prepared = ApothecaryProcessor.stillBottle(
                server.registryAccess(),
                withItem(server, still, BrewingStillBlockEntity.SLOT_FUEL, charcoal)
        );
        List<ItemStackWithSlot> slots = ItemStackWithSlot.CODEC.listOf().parse(
                server.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                prepared.get("Items")
        ).getOrThrow();
        assertTrue(slots.stream().anyMatch(slot -> slot.slot() == BrewingStillBlockEntity.SLOT_FUEL && slot.stack().is(Items.CHARCOAL)));
        assertTrue(slots.stream().anyMatch(slot -> slot.slot() == BrewingStillBlockEntity.SLOT_BOTTLE && SuspiciousBrew.is(slot.stack())));

        BlockState east = ModBlocks.BREWING_STILL.get().defaultBlockState().setValue(BrewingStillBlock.FACING, Direction.EAST);
        assertEquals(Direction.SOUTH, east.rotate(Rotation.CLOCKWISE_90).getValue(BrewingStillBlock.FACING));

        CompoundTag furnace = ApothecaryProcessor.furnaceSupplies(
                server.registryAccess(),
                withItem(server, new CompoundTag(), 0, new ItemStack(Items.SPRUCE_LOG, 4))
        );
        List<ItemStackWithSlot> fired = ItemStackWithSlot.CODEC.listOf().parse(
                server.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                furnace.get("Items")
        ).getOrThrow();
        assertTrue(fired.stream().anyMatch(slot -> slot.slot() == 0 && slot.stack().is(Items.SPRUCE_LOG) && slot.stack().getCount() == 18));
        assertTrue(fired.stream().anyMatch(slot -> slot.slot() == 1 && slot.stack().is(Items.COAL) && slot.stack().getCount() == 2));

        ItemStack disguise = PotionContents.createItemStack(Items.POTION, ModPotions.DISGUISE);
        disguise.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofPlayer("Big_Buff"));
        CompoundTag barrel = ApothecaryProcessor.splashDisguise(
                server.registryAccess(),
                withItem(server, new CompoundTag(), 13, disguise)
        );
        List<ItemStackWithSlot> stored = ItemStackWithSlot.CODEC.listOf().parse(
                server.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                barrel.get("Items")
        ).getOrThrow();
        ItemStack splash = stored.getFirst().stack();
        assertTrue(splash.is(Items.SPLASH_POTION));
        assertEquals(13, stored.getFirst().slot());
        assertEquals("Big_Buff", splash.get(ModDataComponents.ESSENCE.get()).playerName().orElseThrow());
        assertEquals(Boolean.TRUE, splash.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        PotionContents boundless = splash.get(DataComponents.POTION_CONTENTS);
        assertTrue(boundless.potion().isEmpty());
        assertEquals(1, boundless.customEffects().size());
        assertTrue(boundless.customEffects().getFirst().is(com.buffspotions.registry.ModEffects.DISGUISE));
        assertTrue(boundless.customEffects().getFirst().isInfiniteDuration());
        assertEquals(
                "item.buffs_potions.boundless.disguise",
                ((net.minecraft.network.chat.contents.TranslatableContents) splash.get(DataComponents.CUSTOM_NAME).getContents()).getKey()
        );
    }

    @Test
    void barrelsAddOneSupplyUnlessTheyHoldDisguise(MinecraftServer server) {
        Set<String> supplies = new HashSet<>();
        boolean scattered = false;
        for (int i = 0; i < 80; i++) {
            CompoundTag filled = ApothecaryProcessor.barrelContents(
                    server.registryAccess(),
                    new CompoundTag(),
                    RandomSource.create(i)
            );
            List<ItemStackWithSlot> slots = items(server, filled);
            assertFalse(slots.isEmpty());
            ItemStack first = slots.getFirst().stack();
            Set<Integer> used = new HashSet<>();
            for (ItemStackWithSlot slot : slots) {
                assertEquals(1, slot.stack().getCount());
                assertTrue(ItemStack.isSameItemSameComponents(first, slot.stack()));
                assertTrue(used.add(slot.slot()));
                if (slot.slot() > 3) {
                    scattered = true;
                }
            }
            assertTrue(slots.size() >= 2 && slots.size() <= 4);
            supplies.add(supplyName(first));
        }
        assertTrue(scattered);
        assertEquals(
                Set.of(
                        "minecraft:beetroot",
                        "minecraft:carrot",
                        "minecraft:honeycomb",
                        "minecraft:glass_bottle",
                        "minecraft:beetroot_seeds",
                        "minecraft:potion",
                        "minecraft:copper_nugget",
                        "minecraft:gold_nugget"
                ),
                supplies
        );

        ItemStack disguise = PotionContents.createItemStack(Items.POTION, ModPotions.DISGUISE);
        disguise.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofPlayer("Big_Buff"));
        CompoundTag barrel = ApothecaryProcessor.barrelContents(
                server.registryAccess(),
                withItem(server, new CompoundTag(), 13, disguise),
                RandomSource.create(1)
        );
        List<ItemStackWithSlot> stored = items(server, barrel);
        assertEquals(1, stored.size());
        assertEquals(13, stored.getFirst().slot());
        assertTrue(stored.getFirst().stack().is(Items.SPLASH_POTION));
    }

    @Test
    void beehiveHoldsTwoBees(MinecraftServer server) {
        CompoundTag hive = ApothecaryProcessor.beehiveBees(server.registryAccess(), new CompoundTag());
        ListTag bees = hive.getList("bees").orElseThrow();
        assertEquals(2, bees.size());
        for (Tag bee : bees) {
            CompoundTag occupant = (CompoundTag) bee;
            assertEquals("minecraft:bee", occupant.getCompound("entity_data").orElseThrow().getStringOr("id", ""));
            assertEquals(0, occupant.getIntOr("ticks_in_hive", -1));
        }
    }

    private static List<ItemStackWithSlot> items(MinecraftServer server, CompoundTag nbt) {
        return ItemStackWithSlot.CODEC.listOf().parse(
                server.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                nbt.get("Items")
        ).getOrThrow();
    }

    private static String supplyName(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.is(Potions.WATER)) {
            return "minecraft:potion";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    private static CompoundTag withItem(MinecraftServer server, CompoundTag nbt, int slot, ItemStack stack) {
        Tag encoded = ItemStackWithSlot.CODEC.listOf().encodeStart(
                server.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                List.of(new ItemStackWithSlot(slot, stack))
        ).getOrThrow();
        nbt.put("Items", encoded);
        return nbt;
    }

    private static StructureTemplate.StructureEntityInfo process(CompoundTag nbt) {
        StructureTemplate.StructureEntityInfo info = new StructureTemplate.StructureEntityInfo(Vec3.ZERO, BlockPos.ZERO, nbt);
        return new ApothecaryProcessor().processEntity(null, BlockPos.ZERO, info, info, null, null);
    }

    private static boolean knownShelfItem(ItemStack stack) {
        if (stack.is(Items.HONEY_BOTTLE) || stack.is(Items.EXPERIENCE_BOTTLE)) {
            return true;
        }
        if (SuspiciousBrew.is(stack) || Remedies.isRemedy(stack)) {
            return true;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        return contents.is(Potions.WATER)
                || contents.is(ModPotions.MILK)
                || (MundaneBrewing.isMundane(stack) && contents.is(ModPotions.WARDING));
    }
}
