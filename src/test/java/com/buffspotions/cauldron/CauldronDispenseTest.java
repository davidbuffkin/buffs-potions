package com.buffspotions.cauldron;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class CauldronDispenseTest {
    @Test
    void anEmptyBottleOrEmptyFlagonFillsWithWater(MinecraftServer server) {
        ItemStack bottle = CauldronDispense.fill(new ItemStack(Items.GLASS_BOTTLE));
        assertTrue(bottle.is(Items.POTION));
        assertTrue(bottle.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER));

        ItemStack flagon = CauldronDispense.fill(new ItemStack(ModItems.FLAGON.get()));
        FlagonContents contents = FlagonContents.of(flagon);
        assertTrue(contents.isWater());
        assertEquals(FlagonContents.MAX_DOSES, contents.doses());

        ItemStack partial = new ItemStack(ModItems.FLAGON.get());
        partial.set(
                ModDataComponents.FLAGON_CONTENTS.get(),
                new FlagonContents(PotionContents.createItemStack(Items.POTION, Potions.WATER), 3)
        );
        assertTrue(CauldronDispense.fill(partial).isEmpty());
        assertTrue(CauldronDispense.fill(new ItemStack(Items.POTION)).isEmpty());
        assertTrue(CauldronDispense.fill(new ItemStack(ModItems.ENDER_FLAGON.get())).isEmpty());
    }
}
