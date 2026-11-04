package com.buffspotions.flagon;

import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class FlagonContentsTest {
    @Test
    void storesNineMatchingDosesAndRejectsADifferentPotion(MinecraftServer server) {
        ItemStack strength = PotionContents.createItemStack(Items.POTION, Potions.STRENGTH);
        ItemStack swiftness = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
        FlagonContents contents = FlagonContents.EMPTY;
        for (int i = 0; i < FlagonContents.MAX_DOSES; i++) {
            contents = contents.insertDose(strength);
        }

        assertEquals(FlagonContents.MAX_DOSES, contents.doses());
        assertEquals(contents, contents.insertDose(strength));
        assertEquals(contents, contents.insertDose(swiftness));
        assertTrue(contents.bottledDose().is(Items.POTION));
        assertEquals(FlagonContents.EMPTY, contents.remove(FlagonContents.MAX_DOSES));
    }

    @Test
    void customNamesStayDistinctAndBucketsMoveThreeDoses(MinecraftServer server) {
        ItemStack water = FlagonContents.waterTemplate();
        ItemStack named = water.copy();
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Spring"));
        FlagonContents contents = FlagonContents.EMPTY.insertDose(water);

        assertFalse(contents.canInsertDose(named));
        FlagonContents almostFull = new FlagonContents(water, 7);
        assertFalse(almostFull.canAccept(water, FlagonContents.BUCKET_DOSES));
        assertTrue(almostFull.canAccept(water, 2));
        assertEquals(4, contents.insert(water, FlagonContents.BUCKET_DOSES).doses());
        assertTrue(contents.isWater());
        assertEquals(3, new FlagonContents(FlagonContents.milkTemplate(), 6).remove(FlagonContents.BUCKET_DOSES).doses());
    }

    @Test
    void milkFillsToNineAndSplashPotionsAreRejected(MinecraftServer server) {
        assertTrue(FlagonContents.EMPTY.canMilk());
        assertEquals(FlagonContents.MAX_DOSES, FlagonContents.EMPTY.fillWithMilk().doses());
        assertFalse(FlagonContents.isDoseItem(new ItemStack(Items.SPLASH_POTION)));
        assertFalse(FlagonContents.isDoseItem(new ItemStack(Items.LINGERING_POTION)));
        assertTrue(FlagonContents.isDoseItem(FlagonContents.milkTemplate()));
        assertTrue(FlagonContents.isDoseItem(new ItemStack(Items.HONEY_BOTTLE)));
        ItemStack glass = new ItemStack(Items.GLASS_BOTTLE);
        FlagonContents milk = FlagonContents.EMPTY.fillWithMilk();
        assertTrue(milk.isMilk());
        assertTrue(milk.automationResult(glass).is(Items.POTION));
        assertTrue(milk.bottledDose().getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.MILK));
        FlagonContents eight = milk.applyAutomation(glass);
        assertEquals(8, eight.doses());
        FlagonInteractions.DispenseMove poured = FlagonInteractions.dispenseMove(eight, FlagonContents.milkTemplate());
        assertTrue(poured.handled());
        assertEquals(FlagonContents.MAX_DOSES, poured.contents().doses());
        assertTrue(poured.slot().is(Items.GLASS_BOTTLE));
        FlagonInteractions.DispenseMove filled = FlagonInteractions.dispenseMove(poured.contents(), new ItemStack(Items.GLASS_BOTTLE));
        assertEquals(8, filled.contents().doses());
        assertTrue(filled.slot().is(Items.POTION));
    }

    @Test
    void dispenserPoursAMatchingFlagon(MinecraftServer server) {
        ItemStack strength = PotionContents.createItemStack(Items.POTION, Potions.STRENGTH);
        ItemStack swiftness = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
        ItemStack source = new ItemStack(ModItems.FLAGON.get());
        source.set(ModDataComponents.FLAGON_CONTENTS.get(), new FlagonContents(strength, 6));
        FlagonContents placed = new FlagonContents(strength, 5);

        FlagonInteractions.DispenseMove poured = FlagonInteractions.dispenseMove(placed, source);
        assertTrue(poured.handled());
        assertEquals(FlagonContents.MAX_DOSES, poured.contents().doses());
        assertEquals(2, FlagonContents.of(poured.slot()).doses());

        ItemStack other = source.copy();
        other.set(ModDataComponents.FLAGON_CONTENTS.get(), new FlagonContents(swiftness, 4));
        assertFalse(FlagonInteractions.dispenseMove(placed, other).handled());
        assertFalse(FlagonInteractions.dispenseMove(placed, new ItemStack(ModItems.FLAGON.get())).handled());
    }

    @Test
    void namesFollowTheContainedLiquid(MinecraftServer server) {
        assertTranslation("item.buffs_potions.flagon", FlagonContents.EMPTY.itemName());
        assertTranslation("item.buffs_potions.flagon.of_milk", FlagonContents.EMPTY.fillWithMilk().itemName());
        assertTranslation("item.buffs_potions.flagon.of_water", new FlagonContents(FlagonContents.waterTemplate(), 1).itemName());
        assertTranslation(
                "item.buffs_potions.flagon.of_honey",
                new FlagonContents(new ItemStack(Items.HONEY_BOTTLE), 1).itemName()
        );
        assertTranslation(
                "item.buffs_potions.flagon.effect.swiftness",
                new FlagonContents(PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS), 2).itemName()
        );
        assertTranslation(
                "item.buffs_potions.flagon.effect.suspicious_brew",
                new FlagonContents(PotionContents.createItemStack(Items.POTION, ModPotions.SUSPICIOUS_BREW), 1).itemName()
        );
    }

    private static void assertTranslation(String key, Component component) {
        assertTrue(component.getContents() instanceof TranslatableContents);
        assertEquals(key, ((TranslatableContents) component.getContents()).getKey());
    }
}
