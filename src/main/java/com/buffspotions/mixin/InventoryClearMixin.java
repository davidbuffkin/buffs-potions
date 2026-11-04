package com.buffspotions.mixin;

import com.buffspotions.equipment.BeltSlotAccess;
import java.util.function.Predicate;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
abstract class InventoryClearMixin {
    @Inject(method = "clearOrCountMatchingItems", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$clearBelt(
            Predicate<ItemStack> predicate,
            int amountToRemove,
            Container craftSlots,
            CallbackInfoReturnable<Integer> callback
    ) {
        Inventory inventory = (Inventory) (Object) this;
        ItemStack belt = BeltSlotAccess.getEquipped(inventory.player);
        if (belt.isEmpty() || !predicate.test(belt)) {
            return;
        }
        int count = callback.getReturnValue();
        boolean countingOnly = amountToRemove == 0;
        if (!countingOnly && amountToRemove > 0 && count >= amountToRemove) {
            return;
        }
        int removed = countingOnly ? belt.getCount() : belt.getCount();
        if (!countingOnly && amountToRemove > 0) {
            removed = Math.min(belt.getCount(), amountToRemove - count);
        }
        if (removed <= 0) {
            return;
        }
        if (!countingOnly) {
            if (removed >= belt.getCount()) {
                BeltSlotAccess.setEquipped(inventory.player, ItemStack.EMPTY);
            } else {
                belt.shrink(removed);
                BeltSlotAccess.setEquipped(inventory.player, belt);
            }
        }
        callback.setReturnValue(count + removed);
    }
}
