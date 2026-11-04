package com.buffspotions.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    private static final Identifier INVENTORY =
            Identifier.withDefaultNamespace("textures/gui/container/inventory.png");

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
        AbstractContainerScreenAccessor screen = (AbstractContainerScreenAccessor) (Object) this;
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                INVENTORY,
                screen.buffsPotions$getLeftPos() + 76,
                screen.buffsPotions$getTopPos() + 43,
                76,
                61,
                18,
                18,
                256,
                256
        );
    }
}
