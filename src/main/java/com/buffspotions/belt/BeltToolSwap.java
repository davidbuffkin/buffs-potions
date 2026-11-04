package com.buffspotions.belt;

import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.registry.ModCriteria;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.state.BlockState;

/** A worn belt swaps the held tool for the first one that actually mines the clicked block. */
public final class BeltToolSwap {
    private BeltToolSwap() {
    }

    public static void swap(ServerPlayer player, BlockPos pos) {
        if (player.isShiftKeyDown() || player.isSpectator() || !player.isWithinBlockInteractionRange(pos, 0.0)) {
            return;
        }
        if (!(BeltSource.equipped(player).getItem() instanceof PotionBeltItem)) {
            return;
        }
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        Inventory inventory = player.getInventory();
        int selected = inventory.getSelectedSlot();
        ItemStack held = inventory.getItem(selected);
        if (!isTool(held) || suits(held, state)) {
            return;
        }
        int replacement = findTool(inventory, selected, state);
        if (replacement < 0) {
            return;
        }
        ItemStack found = inventory.getItem(replacement);
        inventory.setItem(selected, found.copy());
        inventory.setItem(replacement, held.copy());
        player.inventoryMenu.broadcastChanges();
        ModCriteria.HOT_SWAPPING.get().trigger(player);
    }

    private static int findTool(Inventory inventory, int selected, BlockState state) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (slot == selected) {
                continue;
            }
            ItemStack stack = inventory.getItem(slot);
            if (isTool(stack) && suits(stack, state)) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean isTool(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(ItemTags.SWORDS) && stack.has(DataComponents.TOOL);
    }

    private static boolean suits(ItemStack stack, BlockState state) {
        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null) {
            return false;
        }
        return tool.getMiningSpeed(state) > tool.defaultMiningSpeed() || tool.isCorrectForDrops(state);
    }
}
