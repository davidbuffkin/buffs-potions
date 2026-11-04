package com.buffspotions.mixin;

import com.buffspotions.equipment.FallbackBeltContainer;
import com.buffspotions.item.PotionBeltItem;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import com.buffspotions.BuffsPotions;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.world.inventory.InventoryMenu.class)
public abstract class InventoryMenuMixin extends AbstractContainerMenu {
    protected InventoryMenuMixin(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void buffsPotions$addFallbackBeltSlot(
            Inventory inventory,
            boolean active,
            Player owner,
            CallbackInfo callback
    ) {
        if (!ModList.get().isLoaded("curios")) {
            addSlot(new Slot(new FallbackBeltContainer(owner), 0, 77, 44) {
                @Override
                public Identifier getNoItemIcon() {
                    return Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "container/slot/belt");
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.getItem() instanceof PotionBeltItem;
                }
            });
        }
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$quickMoveBelt(Player player, int index, CallbackInfoReturnable<ItemStack> callback) {
        if (index < 0 || index >= slots.size()) {
            return;
        }
        Slot clicked = slots.get(index);
        Slot beltSlot = beltSlot();
        if (beltSlot == null) {
            return;
        }
        if (clicked == beltSlot) {
            ItemStack original = clicked.getItem().copy();
            if (original.isEmpty() || !moveItemStackTo(clicked.getItem(), 9, 45, true)) {
                callback.setReturnValue(ItemStack.EMPTY);
                return;
            }
            clicked.setChanged();
            callback.setReturnValue(original);
            return;
        }
        ItemStack moving = clicked.getItem();
        if (moving.getItem() instanceof PotionBeltItem && !beltSlot.hasItem() && beltSlot.mayPlace(moving)) {
            beltSlot.set(moving.copyWithCount(1));
            moving.shrink(1);
            clicked.setChanged();
            callback.setReturnValue(ItemStack.EMPTY);
        }
    }

    private Slot beltSlot() {
        for (Slot slot : slots) {
            if (slot.container instanceof FallbackBeltContainer) {
                return slot;
            }
        }
        return null;
    }
}
