package com.buffspotions.mixin.client;

import com.buffspotions.client.FuelSlotHint;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
abstract class BrewingStandScreenMixin {
    private static final int BLAZE_POWDER_SLOT = 4;
    private static final int BREWING_STAND_SLOTS = 5;

    @Inject(method = "extractTooltip", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$blazePowder(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo callback) {
        if (!((Object) this instanceof BrewingStandScreen screen)) {
            return;
        }
        if (FuelSlotHint.show(
                graphics,
                ((Screen) (Object) this).getFont(),
                ((AbstractContainerScreenAccessor) this).buffsPotions$getHoveredSlot(),
                screen.getMenu().getCarried(),
                BLAZE_POWDER_SLOT,
                BREWING_STAND_SLOTS,
                stack -> stack.is(ItemTags.BREWING_FUEL),
                Component.translatable("container.buffs_potions.requires_blaze_powder").withStyle(ChatFormatting.RED),
                mouseX,
                mouseY
        )) {
            callback.cancel();
        }
    }
}
