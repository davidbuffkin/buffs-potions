package com.buffspotions.mixin.client;

import com.buffspotions.equipment.FallbackBeltContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModList;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {
    private static final Identifier INVENTORY =
            Identifier.withDefaultNamespace("textures/gui/container/inventory.png");

    @Inject(method = "selectTab", at = @At("TAIL"))
    private void buffsPotions$placeBeltSlot(CreativeModeTab tab, CallbackInfo callback) {
        if (ModList.get().isLoaded("curios") || tab.getType() != CreativeModeTab.Type.INVENTORY) {
            return;
        }
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        var slots = screen.getMenu().slots;
        for (int index = 0; index < slots.size(); index++) {
            Slot slot = slots.get(index);
            if (slot.container instanceof FallbackBeltContainer) {
                SlotPositionAccessor moved = (SlotPositionAccessor) slot;
                moved.buffsPotions$setX(17);
                moved.buffsPotions$setY(20);
            }
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void buffsPotions$beltSlotWell(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo callback
    ) {
        if (ModList.get().isLoaded("curios")) {
            return;
        }
        CreativeModeInventoryScreen screen = (CreativeModeInventoryScreen) (Object) this;
        if (!screen.isInventoryOpen()) {
            return;
        }
        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) this;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                INVENTORY,
                accessor.buffsPotions$getLeftPos() + 16,
                accessor.buffsPotions$getTopPos() + 19,
                76,
                61,
                18,
                18,
                256,
                256
        );
    }

    /**
     * The creative inventory listener repeats every belt-slot change back to the server.
     * Send the belt once after a real click instead, so opening creative mode does not
     * re-equip it and a removed belt does not reappear.
     */
    @Inject(method = "slotClicked", at = @At("TAIL"))
    private void buffsPotions$syncBeltAfterClick(
            @Nullable Slot slot,
            int slotId,
            int buttonNum,
            ContainerInput containerInput,
            CallbackInfo callback
    ) {
        if (ModList.get().isLoaded("curios")) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null || !minecraft.player.hasInfiniteMaterials()) {
            return;
        }
        var menu = minecraft.player.inventoryMenu;
        for (int index = 0; index < menu.slots.size(); index++) {
            Slot candidate = menu.slots.get(index);
            if (candidate.container instanceof FallbackBeltContainer) {
                minecraft.gameMode.handleCreativeModeItemAdd(candidate.getItem().copy(), index);
                return;
            }
        }
    }
}
