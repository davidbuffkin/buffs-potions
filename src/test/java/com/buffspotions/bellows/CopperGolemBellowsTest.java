package com.buffspotions.bellows;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(EphemeralTestServerProvider.class)
class CopperGolemBellowsTest {
    @Test
    void waxAndAxesLeaveTheBellowsUntilTheHandIsEmpty(MinecraftServer server) {
        ItemStack wax = new ItemStack(Items.HONEYCOMB);
        ItemStack axe = new ItemStack(Items.DIAMOND_AXE);
        ItemStack shears = new ItemStack(Items.SHEARS);

        assertEquals(InteractionResult.SUCCESS_SERVER, CopperGolemBellows.bellowsInteract(wax, ItemStack.EMPTY, true));
        assertEquals(InteractionResult.SUCCESS_SERVER, CopperGolemBellows.bellowsInteract(axe, ItemStack.EMPTY, true));
        assertNull(CopperGolemBellows.bellowsInteract(wax, ItemStack.EMPTY, false));
        assertEquals(InteractionResult.PASS, CopperGolemBellows.bellowsInteract(ItemStack.EMPTY, wax, false));
        assertEquals(InteractionResult.PASS, CopperGolemBellows.bellowsInteract(ItemStack.EMPTY, axe, true));
        assertEquals(InteractionResult.PASS, CopperGolemBellows.bellowsInteract(ItemStack.EMPTY, shears, false));
        assertNull(CopperGolemBellows.bellowsInteract(ItemStack.EMPTY, ItemStack.EMPTY, true));
        assertNull(CopperGolemBellows.bellowsInteract(shears, ItemStack.EMPTY, true));
    }
}
