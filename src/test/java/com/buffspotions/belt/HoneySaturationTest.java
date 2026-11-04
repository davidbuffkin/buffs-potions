package com.buffspotions.belt;

import com.buffspotions.effect.ItemEating;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class HoneySaturationTest {
    @Test
    void doublesTheVanillaGainThenScalesProtraction(MinecraftServer server) {
        float vanilla = 1.2F;
        assertEquals(1, HoneySaturation.multiplier(0));
        assertEquals(2, HoneySaturation.multiplier(1));
        assertEquals(3, HoneySaturation.multiplier(2));
        assertEquals(4, HoneySaturation.multiplier(3));
        assertEquals(4, HoneySaturation.multiplier(9));
        assertEquals(vanilla, HoneySaturation.extra(vanilla, 1), 0.001F);
        assertEquals(vanilla * 3.0F, HoneySaturation.extra(vanilla, 2), 0.001F);
        assertEquals(vanilla * 5.0F, HoneySaturation.extra(vanilla, 3), 0.001F);
        assertEquals(vanilla * 7.0F, HoneySaturation.extra(vanilla, 4), 0.001F);
    }

    @Test
    void usesTheBeltThatSuppliedTheDrink(MinecraftServer server) {
        ItemStack drunk = new ItemStack(Items.STICK);
        ItemStack gulp = new ItemStack(Items.APPLE);
        ItemStack worn = new ItemStack(Items.BREAD);
        assertSame(drunk, HoneySaturation.sourceBelt(drunk, true, gulp, true, worn));
        assertSame(gulp, HoneySaturation.sourceBelt(ItemStack.EMPTY, true, gulp, true, worn));
        assertSame(worn, HoneySaturation.sourceBelt(ItemStack.EMPTY, false, gulp, true, worn));
        assertTrue(HoneySaturation.sourceBelt(ItemStack.EMPTY, false, gulp, false, worn).isEmpty());
    }

    @Test
    void foodItemsKeepAFoodValue(MinecraftServer server) {
        ItemStack bread = new ItemStack(Items.BREAD);
        FoodProperties meal = bread.get(DataComponents.FOOD);
        assertTrue(meal.nutrition() > 0);
        assertTrue(ItemEating.hasFoodValue(bread));
        assertTrue(ItemEating.hasFoodValue(new ItemStack(Items.HONEY_BOTTLE)));
        assertFalse(ItemEating.hasFoodValue(new ItemStack(Items.STICK)));
    }
}
