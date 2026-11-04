package com.buffspotions.belt;

import com.buffspotions.equipment.BeltSlotAccess;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.registry.ModAttachments;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class BeltSource {
    public static ItemStack held(Player player) {
        ItemStack main = player.getMainHandItem();
        return main.getItem() instanceof PotionBeltItem ? main : ItemStack.EMPTY;
    }

    public static InteractionHand heldHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof PotionBeltItem) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getOffhandItem().getItem() instanceof PotionBeltItem) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    /** The belt the Use Belt HUD and its hotkeys control: a belt in either hand, otherwise the worn one. */
    public static ItemStack shown(Player player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof PotionBeltItem) {
            return main;
        }
        ItemStack offhand = player.getOffhandItem();
        if (offhand.getItem() instanceof PotionBeltItem) {
            return offhand;
        }
        return BeltSlotAccess.getEquipped(player);
    }

    public static void saveShown(Player player, ItemStack belt) {
        InteractionHand hand = heldHand(player);
        if (hand != null && player.getItemInHand(hand) == belt) {
            player.setItemInHand(hand, belt);
            return;
        }
        BeltSlotAccess.setEquipped(player, belt);
    }

    public static ItemStack preferred(Player player) {
        return shown(player);
    }

    public static ItemStack equipped(Player player) {
        return BeltSlotAccess.getEquipped(player);
    }

    /** The belt whose dose is being drunk. A gulp uses the inventory belt, not the worn one. */
    public static ItemStack drinking(Player player) {
        BeltUseState state = player.getData(ModAttachments.BELT_USE);
        if (!state.gulp()) {
            return equipped(player);
        }
        if (state.inventorySlot() == -2) {
            return player.getOffhandItem();
        }
        if (state.inventorySlot() >= 9 && state.inventorySlot() < 36) {
            return player.getInventory().getItem(state.inventorySlot());
        }
        return ItemStack.EMPTY;
    }

    /** A gulp is drawn as that belt with the dose being drunk selected. */
    public static ItemStack drinkingShown(Player player) {
        ItemStack belt = drinking(player);
        BeltUseState state = player.getData(ModAttachments.BELT_USE);
        if (belt.isEmpty() || !state.gulp()) {
            return belt;
        }
        ItemStack shown = belt.copy();
        BeltContents.setActiveSlot(shown, state.slot());
        return shown;
    }

    private BeltSource() {
    }
}
