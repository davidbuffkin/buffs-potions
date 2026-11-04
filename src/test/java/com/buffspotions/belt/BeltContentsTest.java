package com.buffspotions.belt;

import com.buffspotions.equipment.BeltSlotAccess;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModPotions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseCooldown;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class BeltContentsTest {
    @Test
    void preservesNestedStackComponentsThroughCopy(MinecraftServer server) {
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();
        ItemStack potion = new ItemStack(Items.POTION);
        potion.set(DataComponents.CUSTOM_NAME, Component.literal("Test Dose"));

        assertEquals(0, BeltContents.insertFirst(belt, potion, BeltContents.BASE_SLOTS));
        assertTrue(potion.isEmpty());

        ItemStack copy = belt.copy();
        assertEquals(1, BeltContents.get(copy, 0).getCount());
        assertEquals("Test Dose", BeltContents.get(copy, 0).getHoverName().getString());
    }

    @Test
    void acceptsDrinkableAndThrowablePotions(MinecraftServer server) {
        assertTrue(BeltContents.isAcceptedDose(new ItemStack(Items.SPLASH_POTION)));
        assertTrue(BeltContents.isAcceptedDose(new ItemStack(Items.LINGERING_POTION)));
        assertTrue(BeltContents.isThrowableDose(new ItemStack(Items.SPLASH_POTION)));
        assertTrue(BeltContents.isThrowableDose(new ItemStack(Items.LINGERING_POTION)));
        assertFalse(BeltContents.isDrinkableDose(new ItemStack(Items.SPLASH_POTION)));
        assertFalse(BeltContents.isAcceptedDose(new ItemStack(Items.DIAMOND)));
        assertTrue(BeltContents.isAcceptedDose(new ItemStack(Items.POTION)));
        assertTrue(BeltContents.isDrinkableDose(new ItemStack(Items.POTION)));
        assertTrue(BeltContents.isAcceptedDose(new ItemStack(Items.HONEY_BOTTLE)));
    }

    @Test
    void enforcesOneItemPerCell(MinecraftServer server) {
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();
        ItemStack honey = new ItemStack(Items.HONEY_BOTTLE, 8);

        BeltContents.set(belt, 2, honey);

        assertEquals(1, BeltContents.get(belt, 2).getCount());
        assertEquals(8, honey.getCount());
    }

    @Test
    void scrollingSkipsEmptyAndResidueCells(MinecraftServer server) {
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();
        BeltContents.set(belt, 0, new ItemStack(Items.GLASS_BOTTLE));
        BeltContents.set(belt, 4, new ItemStack(Items.HONEY_BOTTLE));
        BeltContents.setActiveSlot(belt, 0);

        assertEquals(4, BeltContents.scrollConsumable(belt, 1, BeltContents.BASE_SLOTS));
        assertEquals(4, BeltContents.firstConsumable(belt, BeltContents.BASE_SLOTS));
    }

    @Test
    void selectionCanAddressAllReservedCellsWithoutExposingThem(MinecraftServer server) {
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();

        BeltContents.setActiveSlot(belt, 17);

        assertEquals(17, BeltContents.activeSlot(belt));
        assertEquals(-1, BeltContents.firstConsumable(belt, BeltContents.BASE_SLOTS));
    }

    @Test
    void glassResidueIsSkippedAndRemovable(MinecraftServer server) {
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();
        BeltContents.set(belt, 0, new ItemStack(Items.GLASS_BOTTLE));
        BeltContents.set(belt, 1, new ItemStack(Items.POTION));

        assertFalse(BeltContents.isAcceptedDose(BeltContents.get(belt, 0)));
        assertTrue(BeltContents.isResidue(BeltContents.get(belt, 0)));
        assertEquals(1, BeltContents.firstConsumable(belt, BeltContents.BASE_SLOTS));
        BeltContents.set(belt, 0, ItemStack.EMPTY);
        assertTrue(BeltContents.get(belt, 0).isEmpty());
    }

    @Test
    void fullBeltRejectsAnotherDose(MinecraftServer server) {
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();
        for (int slot = 0; slot < BeltContents.BASE_SLOTS; slot++) {
            BeltContents.set(belt, slot, new ItemStack(Items.HONEY_BOTTLE));
        }
        ItemStack extra = new ItemStack(Items.POTION);

        assertEquals(-1, BeltContents.insertFirst(belt, extra, BeltContents.BASE_SLOTS));
        assertEquals(1, extra.getCount());
    }

    @Test
    void acceptsWaterHoneyAndMilk(MinecraftServer server) {
        assertTrue(BeltContents.isAcceptedDose(new ItemStack(Items.POTION)));
        assertTrue(BeltContents.isAcceptedDose(new ItemStack(Items.HONEY_BOTTLE)));
        assertTrue(BeltContents.isAcceptedDose(FlagonContents.milkTemplate()));
        assertFalse(BeltContents.isAcceptedDose(new ItemStack(Items.GLASS_BOTTLE)));
        assertFalse(BeltContents.isAcceptedDose(ModItems.POTION_BELT.get().getDefaultInstance()));
    }

    @Test
    void beltsShareACooldownGroup(MinecraftServer server) {
        UseCooldown cooldown = ModItems.POTION_BELT.get().getDefaultInstance().get(DataComponents.USE_COOLDOWN);
        assertTrue(cooldown != null && cooldown.cooldownGroup().isPresent());
        assertEquals("buffs_potions:belts", cooldown.cooldownGroup().get().toString());
    }

    @Test
    void occupiedCuriosSlotSendsFallbackBeltToInventory(MinecraftServer server) {
        ItemStack fallback = ModItems.POTION_BELT.get().getDefaultInstance();
        assertEquals(BeltSlotAccess.Migration.TO_CURIOS, BeltSlotAccess.migrate(fallback, true));
        assertEquals(BeltSlotAccess.Migration.TO_INVENTORY, BeltSlotAccess.migrate(fallback, false));
        assertEquals(BeltSlotAccess.Migration.NONE, BeltSlotAccess.migrate(ItemStack.EMPTY, true));
    }

    @Test
    void equippedDrinkCanBeCancelledWithoutLosingSelection(MinecraftServer server) {
        var state = new com.buffspotions.belt.BeltUseState();
        state.start(3);
        state.requestAdvance();
        assertTrue(state.isDrinking());
        state.cancel();
        assertFalse(state.isDrinking());
        assertEquals(-1, state.slot());
        assertTrue(state.pendingAdvance());
    }

    @Test
    void nextSelectableSkipsEmptyCellsAndGlassBottles(MinecraftServer server) {
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();
        BeltContents.set(belt, 0, new ItemStack(Items.POTION));
        BeltContents.set(belt, 1, new ItemStack(Items.GLASS_BOTTLE));
        BeltContents.set(belt, 3, new ItemStack(Items.HONEY_BOTTLE));
        BeltContents.setActiveSlot(belt, 0);

        assertEquals(3, BeltContents.nextSelectable(belt, 0, BeltContents.BASE_SLOTS));
        assertEquals(0, BeltContents.nextSelectable(belt, 3, BeltContents.BASE_SLOTS));
        assertEquals(-1, BeltContents.nextSelectable(belt, 1, 0));
    }

    @Test
    void bundleRemovalTakesTheLastOccupiedCell(MinecraftServer server) {
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();
        BeltContents.set(belt, 0, new ItemStack(Items.POTION));
        BeltContents.set(belt, 4, new ItemStack(Items.GLASS_BOTTLE));
        BeltContents.set(belt, 8, new ItemStack(Items.HONEY_BOTTLE));

        ItemStack removed = BeltContents.removeLastOccupied(belt, BeltContents.BASE_SLOTS);

        assertTrue(removed.is(Items.HONEY_BOTTLE));
        assertTrue(BeltContents.get(belt, 8).isEmpty());
        assertEquals(4, BeltContents.lastOccupied(belt, BeltContents.BASE_SLOTS));
    }

    @Test
    void bundleRemovalTakesEveryEmptyBottleAtOnce(MinecraftServer server) {
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();
        BeltContents.set(belt, 0, new ItemStack(Items.POTION));
        BeltContents.set(belt, 2, new ItemStack(Items.GLASS_BOTTLE));
        BeltContents.set(belt, 5, new ItemStack(Items.GLASS_BOTTLE));
        BeltContents.set(belt, 8, new ItemStack(Items.HONEY_BOTTLE));

        ItemStack removed = BeltContents.takeEmptyStack(belt, BeltContents.BASE_SLOTS);

        assertTrue(removed.is(Items.GLASS_BOTTLE));
        assertEquals(2, removed.getCount());
        assertTrue(BeltContents.get(belt, 0).is(Items.POTION));
        assertTrue(BeltContents.get(belt, 2).isEmpty());
        assertTrue(BeltContents.get(belt, 5).isEmpty());
        assertTrue(BeltContents.get(belt, 8).is(Items.HONEY_BOTTLE));
        assertTrue(BeltContents.takeEmptyStack(belt, BeltContents.BASE_SLOTS).isEmpty());
    }

    @Test
    void bundleRemovalTakesEveryEmptyFlagonAtOnce(MinecraftServer server) {
        ItemStack belt = ModItems.FLAGON_BELT.get().getDefaultInstance();
        ItemStack empty = new ItemStack(ModItems.FLAGON.get());
        ItemStack filled = new ItemStack(ModItems.FLAGON.get());
        filled.set(
                ModDataComponents.FLAGON_CONTENTS.get(),
                new FlagonContents(PotionContents.createItemStack(Items.POTION, Potions.STRENGTH), 2)
        );
        BeltContents.set(belt, 0, filled);
        BeltContents.set(belt, 1, empty);
        BeltContents.set(belt, 4, empty.copy());
        BeltContents.set(belt, 6, filled.copy());

        ItemStack removed = BeltContents.takeEmptyStack(belt, BeltContents.BASE_SLOTS);

        assertTrue(removed.is(ModItems.FLAGON.get()));
        assertEquals(2, removed.getCount());
        assertTrue(FlagonContents.of(removed).isEmpty());
        assertTrue(BeltContents.get(belt, 1).isEmpty());
        assertTrue(BeltContents.get(belt, 4).isEmpty());
        assertFalse(FlagonContents.of(BeltContents.get(belt, 0)).isEmpty());
        assertFalse(FlagonContents.of(BeltContents.get(belt, 6)).isEmpty());

        for (ItemStack throwing : List.of(
                ModItems.SPLASH_FLAGON_BELT.get().getDefaultInstance(),
                ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance()
        )) {
            BeltContents.set(throwing, 0, filled.copy());
            BeltContents.set(throwing, 1, empty.copy());
            BeltContents.set(throwing, 3, empty.copy());
            ItemStack fromThrowing = BeltContents.takeEmptyStack(throwing, BeltContents.BASE_SLOTS);
            assertTrue(fromThrowing.is(ModItems.FLAGON.get()));
            assertEquals(2, fromThrowing.getCount());
            assertTrue(FlagonContents.of(fromThrowing).isEmpty());
            assertTrue(BeltContents.get(throwing, 1).isEmpty());
            assertTrue(BeltContents.get(throwing, 3).isEmpty());
            assertFalse(FlagonContents.of(BeltContents.get(throwing, 0)).isEmpty());
        }
    }

    @Test
    void bundleRemovalTakesThrowingBeltBottlesBeforeEmptyFlagons(MinecraftServer server) {
        ItemStack empty = new ItemStack(ModItems.FLAGON.get());
        for (ItemStack belt : List.of(
                ModItems.SPLASH_FLAGON_BELT.get().getDefaultInstance(),
                ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance()
        )) {
            BeltContents.setBottle(belt, 0, new ItemStack(Items.GLASS_BOTTLE, 3));
            BeltContents.setBottle(belt, 2, new ItemStack(Items.GLASS_BOTTLE, 4));
            BeltContents.set(belt, 1, empty.copy());

            ItemStack bottles = BeltContents.takeEmptyStack(belt, BeltContents.BASE_SLOTS);
            assertTrue(bottles.is(Items.GLASS_BOTTLE));
            assertEquals(7, bottles.getCount());
            assertEquals(0, BeltContents.bottleCount(belt));
            assertTrue(BeltContents.get(belt, 1).is(ModItems.FLAGON.get()));

            ItemStack flagons = BeltContents.takeEmptyStack(belt, BeltContents.BASE_SLOTS);
            assertTrue(flagons.is(ModItems.FLAGON.get()));
            assertEquals(1, flagons.getCount());
            assertTrue(BeltContents.get(belt, 1).isEmpty());
        }
    }

    @Test
    void flagonAdvanceSkipsEmptyFlagons(MinecraftServer server) {
        ItemStack belt = ModItems.FLAGON_BELT.get().getDefaultInstance();
        ItemStack empty = new ItemStack(ModItems.FLAGON.get());
        ItemStack filled = new ItemStack(ModItems.FLAGON.get());
        filled.set(
                ModDataComponents.FLAGON_CONTENTS.get(),
                new FlagonContents(PotionContents.createItemStack(Items.POTION, Potions.STRENGTH), 2)
        );
        BeltContents.set(belt, 0, filled);
        BeltContents.set(belt, 1, empty);
        BeltContents.set(belt, 3, empty.copy());
        BeltContents.set(belt, 4, filled.copy());

        assertEquals(4, BeltContents.nextDrinkableInRow(null, belt, 0, 0, BeltContents.BASE_SLOTS));
        assertEquals(0, BeltContents.nextDrinkableInRow(null, belt, 4, 0, BeltContents.BASE_SLOTS));

        ItemStack empties = ModItems.FLAGON_BELT.get().getDefaultInstance();
        BeltContents.set(empties, 0, empty.copy());
        BeltContents.set(empties, 2, empty.copy());
        assertEquals(-1, BeltContents.nextDrinkableInRow(null, empties, 0, 0, BeltContents.BASE_SLOTS));
    }

    @Test
    void flagonBeltHoldsFlagonsAndNotPotions(MinecraftServer server) {
        ItemStack potionBelt = ModItems.POTION_BELT.get().getDefaultInstance();
        ItemStack flagonBelt = ModItems.FLAGON_BELT.get().getDefaultInstance();
        ItemStack flagon = ModItems.FLAGON.get().getDefaultInstance();
        ItemStack ender = ModItems.ENDER_FLAGON.get().getDefaultInstance();
        ItemStack potion = new ItemStack(Items.POTION);

        assertEquals(512, flagonBelt.getMaxDamage());
        assertFalse(BeltContents.accepts(potionBelt, flagon));
        assertFalse(BeltContents.accepts(potionBelt, ender));
        assertTrue(BeltContents.accepts(potionBelt, potion));
        assertFalse(BeltContents.accepts(flagonBelt, potion));
        assertFalse(BeltContents.accepts(flagonBelt, new ItemStack(Items.HONEY_BOTTLE)));
        assertTrue(BeltContents.accepts(flagonBelt, flagon));
        assertTrue(BeltContents.accepts(flagonBelt, ender));

        assertEquals(0, BeltContents.insertFirst(flagonBelt, ender, BeltContents.BASE_SLOTS));
        assertTrue(ender.isEmpty());
        assertTrue(BeltContents.get(flagonBelt, 0).is(ModItems.ENDER_FLAGON.get()));
        assertEquals(-1, BeltContents.insertFirst(potionBelt, flagon, BeltContents.BASE_SLOTS));
    }

    @Test
    void undyedBeltHasDurabilityAndNoDyedTooltip(MinecraftServer server) {
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();
        BeltContents.set(belt, 2, new ItemStack(Items.HONEY_BOTTLE));

        assertEquals(64, belt.getMaxDamage());
        assertTrue(belt.get(DataComponents.DYED_COLOR) == null);
        assertFalse(belt.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).shows(DataComponents.DYED_COLOR));
        List<Component> tooltip = new ArrayList<>();
        PotionBeltItem.appendBeltTooltip(belt, tooltip);
        assertEquals(1, tooltip.size());
        assertEquals(
                Component.translatable("item.buffs_potions.potion_belt.count", 1, BeltContents.BASE_SLOTS).getString(),
                tooltip.get(0).getString()
        );
    }

    @Test
    void storedDyeIsClearedOnEveryBelt(MinecraftServer server) {
        for (ItemStack belt : List.of(
                ModItems.POTION_BELT.get().getDefaultInstance(),
                ModItems.FLAGON_BELT.get().getDefaultInstance(),
                ModItems.SPLASH_FLAGON_BELT.get().getDefaultInstance(),
                ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance()
        )) {
            assertFalse(belt.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).shows(DataComponents.DYED_COLOR));
            belt.set(DataComponents.DYED_COLOR, new DyedItemColor(0xFF0000));
            PotionBeltItem.clearStoredDye(belt);
            assertTrue(belt.get(DataComponents.DYED_COLOR) == null);
        }
    }

    @Test
    void protractionFollowsEnchantmentLevel(MinecraftServer server) {
        assertEquals(1.0F, BeltEnchantments.durationMultiplierForLevel(0));
        assertEquals(1.0F + 1.0F / 3.0F, BeltEnchantments.durationMultiplierForLevel(1), 0.0001F);
        assertEquals(2.0F, BeltEnchantments.durationMultiplierForLevel(3));

        var enchantments = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        assertEquals(3, enchantments.getOrThrow(BeltEnchantments.PROTRACTION).value().getMaxLevel());
        ItemStack belt = ModItems.POTION_BELT.get().getDefaultInstance();
        assertEquals(BeltContents.BASE_SLOTS, BeltEnchantments.capacity(belt));
        assertTrue(enchantments.get(BeltEnchantments.GAUGE).isEmpty());

        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING).value();
        var mending = enchantments.getOrThrow(Enchantments.MENDING).value();
        for (ItemStack flagonBelt : java.util.List.of(
                ModItems.FLAGON_BELT.get().getDefaultInstance(),
                ModItems.SPLASH_FLAGON_BELT.get().getDefaultInstance(),
                ModItems.LINGERING_FLAGON_BELT.get().getDefaultInstance()
        )) {
            assertTrue(unbreaking.isSupportedItem(flagonBelt));
            assertTrue(mending.isSupportedItem(flagonBelt));
        }
    }

    @Test
    void gulpRefreshSkipsInstantAndFreshEffects(MinecraftServer server) {
        MobEffectInstance lasting = new MobEffectInstance(MobEffects.STRENGTH, 200, 0);
        assertFalse(BeltEnchantments.effectNeedsRefresh(lasting, new MobEffectInstance(MobEffects.STRENGTH, 200, 0)));
        assertFalse(BeltEnchantments.effectNeedsRefresh(lasting, new MobEffectInstance(MobEffects.STRENGTH, 80, 0)));
        assertTrue(BeltEnchantments.effectNeedsRefresh(lasting, new MobEffectInstance(MobEffects.STRENGTH, 40, 0)));
        assertFalse(BeltEnchantments.effectNeedsRefresh(lasting, new MobEffectInstance(MobEffects.STRENGTH, 40, 0), BeltEnchantments.QUICK_GULP_REMAINING_TICKS));
        assertTrue(BeltEnchantments.effectNeedsRefresh(lasting, new MobEffectInstance(MobEffects.STRENGTH, 10, 0), BeltEnchantments.QUICK_GULP_REMAINING_TICKS));
        assertTrue(BeltEnchantments.effectNeedsRefresh(lasting, null));
        MobEffectInstance instant = new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 0);
        assertFalse(BeltEnchantments.effectNeedsRefresh(instant, null));
        assertTrue(BeltEnchantments.hasRoomForHoney(14));
        assertTrue(BeltEnchantments.hasRoomForHoney(0));
        assertFalse(BeltEnchantments.hasRoomForHoney(15));
        assertFalse(BeltEnchantments.hasRoomForHoney(20));
    }

    @Test
    void unsplashablePotionsStayInTheFlagon(MinecraftServer server) {
        ItemStack rain = PotionContents.createItemStack(Items.POTION, ModPotions.RAIN_CALLING);
        ItemStack sun = PotionContents.createItemStack(Items.POTION, ModPotions.SUN_CALLING);
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        ItemStack incarnation = PotionContents.createItemStack(Items.POTION, ModPotions.INCARNATION);

        assertFalse(BeltConsumptionService.canThrow(rain, false));
        assertFalse(BeltConsumptionService.canThrow(rain, true));
        assertFalse(BeltConsumptionService.canThrow(sun, false));
        assertFalse(BeltConsumptionService.canThrow(sun, true));
        assertTrue(BeltConsumptionService.canThrow(water, false));
        assertTrue(BeltConsumptionService.canThrow(water, true));
        assertTrue(BeltConsumptionService.canThrow(incarnation, false));
        assertFalse(BeltConsumptionService.canThrow(incarnation, true));
    }
}
