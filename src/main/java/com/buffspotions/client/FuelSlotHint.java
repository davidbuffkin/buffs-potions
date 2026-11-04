package com.buffspotions.client;

import java.util.function.Predicate;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Red hint on a fuel slot, unless the cursor is already holding a valid fuel item. */
public final class FuelSlotHint {
    private FuelSlotHint() {
    }

    public static boolean show(
            GuiGraphicsExtractor graphics,
            Font font,
            @Nullable Slot slot,
            ItemStack carried,
            int containerSlot,
            int machineSlots,
            Predicate<ItemStack> accepts,
            Component text,
            int mouseX,
            int mouseY
    ) {
        if (slot == null || slot.index >= machineSlots || slot.getContainerSlot() != containerSlot) {
            return false;
        }
        if (accepts.test(carried) || (carried.isEmpty() && slot.hasItem())) {
            return false;
        }
        graphics.setTooltipForNextFrame(font, text, mouseX, mouseY);
        return true;
    }
}
