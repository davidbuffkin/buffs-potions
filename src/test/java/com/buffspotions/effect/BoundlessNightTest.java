package com.buffspotions.effect;

import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModPotions;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.Vec3;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class BoundlessNightTest {
    @Test
    void midnightIsOneTickHighAboveTheWorldLookingUp(MinecraftServer server) {
        assertTrue(BoundlessNight.midnight(18000));
        assertTrue(BoundlessNight.midnight(18000 + 24000L));
        assertFalse(BoundlessNight.midnight(17999));
        assertFalse(BoundlessNight.midnight(18001));
        assertFalse(BoundlessNight.highEnough(319.75));
        assertTrue(BoundlessNight.highEnough(319.76));
        assertTrue(BoundlessNight.lookingUp(new Vec3(0, 1, 0)));
        assertTrue(BoundlessNight.lookingUp(new Vec3(0, Math.cos(Math.toRadians(4.9)), Math.sin(Math.toRadians(4.9)))));
        assertFalse(BoundlessNight.lookingUp(new Vec3(0, Math.cos(Math.toRadians(6)), Math.sin(Math.toRadians(6)))));
        assertFalse(BoundlessNight.luckTwo(null));
        assertFalse(BoundlessNight.luckTwo(new MobEffectInstance(MobEffects.LUCK, 20, 0)));
        assertTrue(BoundlessNight.luckTwo(new MobEffectInstance(MobEffects.LUCK, 20, 1)));
        assertTrue(server.getTickCount() >= 0);
    }

    @Test
    void onlyADisguiseBottleAndAClockCount(MinecraftServer server) {
        ItemStack bottle = disguise(Items.POTION, ModPotions.DISGUISE);
        ItemStack splash = disguise(Items.SPLASH_POTION, ModPotions.LONG_DISGUISE);
        ItemStack lingering = disguise(Items.LINGERING_POTION, ModPotions.DISGUISE);
        ItemStack clock = new ItemStack(Items.CLOCK);
        assertTrue(BoundlessNight.offered(bottle, clock));
        assertTrue(BoundlessNight.offered(clock, splash));
        assertTrue(BoundlessNight.offered(lingering, clock));

        ItemStack flagon = new ItemStack(ModItems.FLAGON.get());
        flagon.set(DataComponents.POTION_CONTENTS, new PotionContents(ModPotions.DISGUISE));
        flagon.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofPlayer("Ada"));
        assertFalse(BoundlessNight.offered(flagon, new ItemStack(Items.CLOCK)));

        ItemStack healing = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        healing.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofPlayer("Ada"));
        assertFalse(BoundlessNight.disguiseBottle(healing));

        ItemStack unnamed = PotionContents.createItemStack(Items.POTION, ModPotions.DISGUISE);
        assertFalse(BoundlessNight.disguiseBottle(unnamed));
        assertFalse(BoundlessNight.offered(bottle, bottle));
        assertTrue(server.getTickCount() >= 0);
    }

    @Test
    void bindingRenamesTheBottleAndKeepsThePlayerForever(MinecraftServer server) {
        ItemStack bottle = disguise(Items.SPLASH_POTION, ModPotions.DISGUISE);
        int color = bottle.get(DataComponents.POTION_CONTENTS).getColor();
        ItemStack clocks = new ItemStack(Items.CLOCK, 3);
        assertTrue(BoundlessNight.completeHands(clocks, bottle));
        assertEquals(2, clocks.getCount());
        assertEquals(Items.SPLASH_POTION, bottle.getItem());
        assertEquals(Boolean.TRUE, bottle.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE));
        assertEquals("Ada", bottle.get(ModDataComponents.ESSENCE.get()).playerName().orElseThrow());

        PotionContents contents = bottle.get(DataComponents.POTION_CONTENTS);
        assertTrue(contents.potion().isEmpty());
        assertEquals(color, contents.getColor());
        List<MobEffectInstance> effects = contents.customEffects();
        assertEquals(1, effects.size());
        assertTrue(effects.getFirst().is(ModEffects.DISGUISE));
        assertTrue(effects.getFirst().isInfiniteDuration());

        Component name = bottle.get(DataComponents.CUSTOM_NAME);
        TranslatableContents translated = assertInstanceOf(TranslatableContents.class, name.getContents());
        assertEquals("item.buffs_potions.boundless.disguise", translated.getKey());
        assertEquals("Ada", ((Component) translated.getArgs()[0]).getString());

        EyedDisplay.TOOLTIP.set(EssenceSubject.ofPlayer("Ada"));
        EyedDisplay.BOUNDLESS.set(true);
        try {
            Component line = EyedDisplay.effectLine(ModEffects.DISGUISE, 0);
            TranslatableContents effect = assertInstanceOf(TranslatableContents.class, line.getContents());
            assertEquals("effect.buffs_potions.disguise.boundless", effect.getKey());
        } finally {
            EyedDisplay.TOOLTIP.remove();
            EyedDisplay.BOUNDLESS.remove();
        }

        ItemStack dropped = disguise(Items.POTION, ModPotions.DISGUISE);
        assertFalse(BoundlessNight.completeHands(dropped, ItemStack.EMPTY));
        assertFalse(dropped.has(DataComponents.CUSTOM_NAME));

        List<ParameterDose> merged = ParameterDoses.merge(
                List.of(new ParameterDose("buffs_potions:disguise", EssenceSubject.ofPlayer("Ada"), 0, 100)),
                new ParameterDose("buffs_potions:disguise", EssenceSubject.ofPlayer("Ada"), 0, ParameterDose.NEVER)
        );
        assertEquals(ParameterDose.NEVER, merged.getFirst().expiresAt());
        assertTrue(server.getTickCount() >= 0);
    }

    private static ItemStack disguise(net.minecraft.world.item.Item item, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        ItemStack stack = PotionContents.createItemStack(item, potion);
        stack.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofPlayer("Ada"));
        return stack;
    }
}
