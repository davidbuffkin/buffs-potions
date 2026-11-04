package com.buffspotions.stack;

import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModBlocks;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModPotions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
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
class BrewingStackTest {
    @Test
    void torchflowerAndPitcherMakeTheSecondTier(MinecraftServer server) {
        PotionBrewing brewing = brewing(server);
        int base = BuffsPotionsConfig.baseFor("swiftness");

        MobEffectInstance thick = effect(StackBrewing.mix(
                brewing, potion(Potions.STRONG_SWIFTNESS), new ItemStack(Items.TORCHFLOWER)));
        assertEquals(2, thick.getAmplifier());
        assertEquals(Math.max(1, base / 4), thick.getDuration());

        MobEffectInstance clarified = effect(StackBrewing.mix(
                brewing, potion(Potions.LONG_SWIFTNESS), new ItemStack(Items.TORCHFLOWER)));
        assertEquals(1, clarified.getAmplifier());
        assertEquals(Math.max(1, (int) Math.round(base * 1.5)), clarified.getDuration());

        MobEffectInstance thinned = effect(StackBrewing.mix(
                brewing, potion(Potions.LONG_SWIFTNESS), new ItemStack(Items.PITCHER_PLANT)));
        assertEquals(0, thinned.getAmplifier());
        assertEquals(Math.max(1, base * 4), thinned.getDuration());

        MobEffectInstance fromStrong = effect(StackBrewing.mix(
                brewing, potion(Potions.STRONG_SWIFTNESS), new ItemStack(Items.PITCHER_PLANT)));
        assertEquals(1, fromStrong.getAmplifier());
        assertEquals(Math.max(1, (int) Math.round(base * 1.5)), fromStrong.getDuration());

        assertTrue(StackBrewing.mix(brewing, potion(Potions.SWIFTNESS), new ItemStack(Items.TORCHFLOWER)).isEmpty());
        assertTrue(StackBrewing.mix(brewing, potion(Potions.LONG_NIGHT_VISION), new ItemStack(Items.TORCHFLOWER)).isEmpty());
    }

    @Test
    void aFinishedBrewSpendsTheIngredientAndMovesTheQueueUp(MinecraftServer server) {
        PotionBrewing brewing = brewing(server);
        BrewingStackBlockEntity stack = new BrewingStackBlockEntity(
                BlockPos.ZERO, ModBlocks.BREWING_STACK.get().defaultBlockState());
        stack.setItem(BrewingStackBlockEntity.SLOT_FLAGON, waterFlagon());
        stack.setItem(BrewingStackBlockEntity.SLOT_INGREDIENT, new ItemStack(Items.NETHER_WART));
        stack.setItem(BrewingStackBlockEntity.SLOT_QUEUE_0, new ItemStack(Items.GHAST_TEAR));
        stack.setItem(BrewingStackBlockEntity.SLOT_FUEL, new ItemStack(Items.POPPED_CHORUS_FRUIT));

        for (int tick = 0; tick < BrewingStackBlockEntity.BREW_TIME; tick++) {
            stack.tickBrew(brewing, null);
        }

        assertTrue(awkward(stack));
        assertTrue(stack.getItem(BrewingStackBlockEntity.SLOT_INGREDIENT).is(Items.GHAST_TEAR));
        assertTrue(stack.getItem(BrewingStackBlockEntity.SLOT_QUEUE_0).isEmpty());
        assertTrue(stack.getItem(BrewingStackBlockEntity.SLOT_FUEL).isEmpty());
    }

    @Test
    void gunpowderAndDragonsBreathDoNotBrewInTheStack(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        assertTrue(StackBrewing.isIngredient(brewing, new ItemStack(Items.GUNPOWDER)));
        assertTrue(StackBrewing.isIngredient(brewing, new ItemStack(Items.DRAGON_BREATH)));

        ItemStack latent = potion(ModPotions.LATENT_EYEDENTIFICATION);
        assertTrue(StackBrewing.mix(brewing, latent, new ItemStack(Items.GUNPOWDER)).isEmpty());
        assertTrue(StackBrewing.mix(brewing, latent, new ItemStack(Items.DRAGON_BREATH)).isEmpty());
        assertTrue(StackBrewing.mix(brewing, potion(Potions.SWIFTNESS), new ItemStack(Items.GUNPOWDER)).isEmpty());
        assertTrue(StackBrewing.mix(brewing, potion(Potions.SWIFTNESS), new ItemStack(Items.DRAGON_BREATH)).isEmpty());
    }

    @Test
    void everySlotHoldsOneItem(MinecraftServer server) {
        BrewingStackBlockEntity stack = new BrewingStackBlockEntity(
                BlockPos.ZERO, ModBlocks.BREWING_STACK.get().defaultBlockState());
        assertEquals(1, stack.getMaxStackSize());
        ItemStack flower = new ItemStack(Items.TORCHFLOWER);
        assertTrue(stack.canPlaceItem(BrewingStackBlockEntity.SLOT_INGREDIENT, flower));
        stack.setItem(BrewingStackBlockEntity.SLOT_INGREDIENT, flower.copy());
        assertFalse(stack.canPlaceItem(BrewingStackBlockEntity.SLOT_INGREDIENT, flower));
        assertTrue(stack.canPlaceItem(BrewingStackBlockEntity.SLOT_QUEUE_0, flower));
        stack.setItem(BrewingStackBlockEntity.SLOT_QUEUE_0, flower.copy());
        assertFalse(stack.canPlaceItem(BrewingStackBlockEntity.SLOT_QUEUE_0, flower));
        ItemStack fuel = new ItemStack(Items.POPPED_CHORUS_FRUIT);
        assertTrue(stack.canPlaceItem(BrewingStackBlockEntity.SLOT_FUEL, fuel));
        stack.setItem(BrewingStackBlockEntity.SLOT_FUEL, fuel.copy());
        assertTrue(stack.canPlaceItem(BrewingStackBlockEntity.SLOT_FUEL, fuel));
        stack.setItem(BrewingStackBlockEntity.SLOT_FUEL, fuel.copyWithCount(BrewingStackBlockEntity.FUEL_MAX));
        assertFalse(stack.canPlaceItem(BrewingStackBlockEntity.SLOT_FUEL, fuel));
        ItemStack flagon = new ItemStack(ModItems.FLAGON.get());
        stack.setItem(BrewingStackBlockEntity.SLOT_FLAGON, flagon.copy());
        assertFalse(stack.canPlaceItem(BrewingStackBlockEntity.SLOT_FLAGON, flagon));
    }

    @Test
    void anEnderFlagonIsNotABrewingFlagon(MinecraftServer server) {
        BrewingStackBlockEntity stack = new BrewingStackBlockEntity(
                BlockPos.ZERO, ModBlocks.BREWING_STACK.get().defaultBlockState());
        ItemStack ender = new ItemStack(ModItems.ENDER_FLAGON.get());
        assertFalse(stack.canPlaceItem(BrewingStackBlockEntity.SLOT_FLAGON, ender));
        assertTrue(stack.canPlaceItem(BrewingStackBlockEntity.SLOT_FLAGON, new ItemStack(ModItems.FLAGON.get())));
        assertEquals(1, ender.getMaxStackSize());
        assertTrue(ender.getItem().getName(ender).getContents() instanceof TranslatableContents translated
                && translated.getKey().equals("item.buffs_potions.ender_flagon"));
    }

    private static MobEffectInstance effect(ItemStack stack) {
        List<MobEffectInstance> effects = new ArrayList<>();
        stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).getAllEffects().forEach(effects::add);
        assertEquals(1, effects.size());
        return effects.getFirst();
    }

    private static ItemStack potion(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        return PotionContents.createItemStack(Items.POTION, potion);
    }

    private static ItemStack waterFlagon() {
        ItemStack flagon = new ItemStack(ModItems.FLAGON.get());
        flagon.set(
                ModDataComponents.FLAGON_CONTENTS.get(),
                new FlagonContents(PotionContents.createItemStack(Items.POTION, Potions.WATER), 9)
        );
        return flagon;
    }

    private static boolean awkward(BrewingStackBlockEntity stack) {
        PotionContents contents = FlagonContents.of(stack.getItem(BrewingStackBlockEntity.SLOT_FLAGON)).dose().get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.AWKWARD);
    }

    private static PotionBrewing brewing(MinecraftServer server) {
        return PotionBrewing.bootstrap(FeatureFlags.VANILLA_SET, server.registryAccess());
    }
}
