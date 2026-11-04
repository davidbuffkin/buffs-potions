package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.menu.PotionBeltMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class PotionBeltScreen extends AbstractContainerScreen<PotionBeltMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/gui/container/potion_belt.png");
    private static final Identifier BOTTLE_TEXTURE =
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/gui/container/potion_belt_bottles.png");
    private final Identifier texture;

    public PotionBeltScreen(PotionBeltMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, menu.hasBottleRow() ? 151 : 133);
        inventoryLabelY = menu.hasBottleRow() ? 57 : 39;
        texture = menu.hasBottleRow() ? BOTTLE_TEXTURE : TEXTURE;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        extractTransparentBackground(graphics);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
    }
}
