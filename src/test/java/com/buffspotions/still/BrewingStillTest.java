package com.buffspotions.still;

import com.buffspotions.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class BrewingStillTest {
    @Test
    void oneCharcoalBrewsFourPotionsAndThenGoesOut(MinecraftServer server) {
        PotionBrewing brewing = brewing(server);
        BrewingStillBlockEntity still = still();
        still.setItem(BrewingStillBlockEntity.SLOT_BOTTLE, mundane());
        still.setItem(BrewingStillBlockEntity.SLOT_INGREDIENT, new ItemStack(Items.GOLDEN_CARROT));
        still.setItem(BrewingStillBlockEntity.SLOT_FUEL, new ItemStack(Items.CHARCOAL));

        int completed = 0;
        for (int tick = 1; tick <= 1601; tick++) {
            still.tickBrew(brewing, null);
            if (!nightVision(still.getItem(BrewingStillBlockEntity.SLOT_BOTTLE))) {
                continue;
            }
            completed++;
            if (completed == 1) {
                assertEquals(400, tick);
                assertEquals(1201, still.litTime);
                assertTrue(still.getItem(BrewingStillBlockEntity.SLOT_FUEL).isEmpty());
            }
            if (completed == 4) {
                assertEquals(1600, tick);
                assertEquals(1, still.litTime);
            }
            still.setItem(BrewingStillBlockEntity.SLOT_BOTTLE, mundane());
            still.setItem(BrewingStillBlockEntity.SLOT_INGREDIENT, new ItemStack(Items.GOLDEN_CARROT));
        }

        assertEquals(4, completed);
        assertEquals(0, still.litTime);
    }

    @Test
    void brewResetsWhenTheFireGoesOutOrTheMixIsRemoved(MinecraftServer server) {
        PotionBrewing brewing = brewing(server);
        BrewingStillBlockEntity still = still();
        still.setItem(BrewingStillBlockEntity.SLOT_BOTTLE, mundane());
        still.setItem(BrewingStillBlockEntity.SLOT_INGREDIENT, new ItemStack(Items.GOLDEN_CARROT));
        still.setItem(BrewingStillBlockEntity.SLOT_FUEL, new ItemStack(Items.COAL));
        for (int i = 0; i < 20; i++) {
            still.tickBrew(brewing, null);
        }
        assertEquals(0, still.brewTime);
        assertEquals(1, still.getItem(BrewingStillBlockEntity.SLOT_FUEL).getCount());

        still.setItem(BrewingStillBlockEntity.SLOT_FUEL, new ItemStack(Items.CHARCOAL));
        for (int i = 0; i < 200; i++) {
            still.tickBrew(brewing, null);
        }
        assertEquals(200, still.brewTime);
        still.litTime = 0;
        still.tickBrew(brewing, null);
        assertEquals(0, still.brewTime);
        assertFalse(nightVision(still.getItem(BrewingStillBlockEntity.SLOT_BOTTLE)));

        still.setItem(BrewingStillBlockEntity.SLOT_INGREDIENT, ItemStack.EMPTY);
        still.tickBrew(brewing, null);
        assertEquals(0, still.brewTime);
    }

    private static PotionBrewing brewing(MinecraftServer server) {
        return PotionBrewing.bootstrap(FeatureFlags.VANILLA_SET, server.registryAccess());
    }

    private static BrewingStillBlockEntity still() {
        return new BrewingStillBlockEntity(BlockPos.ZERO, ModBlocks.BREWING_STILL.get().defaultBlockState());
    }

    private static ItemStack mundane() {
        return PotionContents.createItemStack(Items.POTION, Potions.MUNDANE);
    }

    private static boolean nightVision(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null
                && contents.is(Potions.NIGHT_VISION)
                && stack.getOrDefault(com.buffspotions.registry.ModDataComponents.MUNDANE.get(), false);
    }
}
