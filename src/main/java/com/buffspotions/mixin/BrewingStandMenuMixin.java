package com.buffspotions.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.tags.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Shift-click prefers a bottle slot, then blaze powder, then the ingredient slot.
 * Vanilla asks the ingredient slot first, and latent brewing treats every item as an ingredient,
 * so a water bottle never reached the bottle slots.
 */
@Mixin(BrewingStandMenu.class)
abstract class BrewingStandMenuMixin extends AbstractContainerMenu {
    private BrewingStandMenuMixin() {
        super(null, 0);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$shiftClick(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> callback) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        PotionBrewing brewing = player.level().potionBrewing();
        if (slotIndex >= 0 && slotIndex <= 4) {
            if (!moveItemStackTo(stack, 5, 41, true)) {
                callback.setReturnValue(ItemStack.EMPTY);
                return;
            }
            slot.onQuickCraft(stack, clicked);
        } else if (stack.getItem() instanceof PotionItem && brewing.isInput(stack)) {
            if (!moveItemStackTo(stack, 0, 3, false)) {
                callback.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else if (stack.is(ItemTags.BREWING_FUEL)) {
            if (!moveItemStackTo(stack, 4, 5, false)) {
                callback.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else if (this.slots.get(3).mayPlace(stack)) {
            if (!moveItemStackTo(stack, 3, 4, false)) {
                callback.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else if (slotIndex >= 5 && slotIndex < 32) {
            if (!moveItemStackTo(stack, 32, 41, false)) {
                callback.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else if (slotIndex >= 32 && slotIndex < 41) {
            if (!moveItemStackTo(stack, 5, 32, false)) {
                callback.setReturnValue(ItemStack.EMPTY);
                return;
            }
        } else if (!moveItemStackTo(stack, 5, 41, false)) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) {
            callback.setReturnValue(ItemStack.EMPTY);
            return;
        }
        slot.onTake(player, clicked);
        callback.setReturnValue(clicked);
    }
}
