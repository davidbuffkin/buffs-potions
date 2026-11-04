package com.buffspotions.mixin.client;

import com.buffspotions.equipment.FallbackBeltContainer;
import net.minecraft.client.gui.screens.inventory.CreativeInventoryListener;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeInventoryListener.class)
public class CreativeInventoryListenerMixin {
    @Inject(method = "slotChanged", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$skipBeltEcho(AbstractContainerMenu container, int slotIndex, ItemStack itemStack, CallbackInfo callback) {
        if (slotIndex < 0 || slotIndex >= container.slots.size()) {
            return;
        }
        if (container.getSlot(slotIndex).container instanceof FallbackBeltContainer) {
            callback.cancel();
        }
    }
}
