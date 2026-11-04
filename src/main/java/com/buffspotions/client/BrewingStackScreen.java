package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.menu.BrewingStackMenu;
import com.buffspotions.stack.BrewingStackBlockEntity;
import com.buffspotions.stack.StackBrewing;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public final class BrewingStackScreen extends AbstractRecipeBookScreen<BrewingStackMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/gui/container/brewing_stack.png");
    private static final Identifier BUBBLES =
            Identifier.withDefaultNamespace("container/brewing_stand/bubbles");
    private static final Identifier BREW_PROGRESS =
            Identifier.withDefaultNamespace("container/brewing_stand/brew_progress");
    private static final int[] BUBBLE_LENGTHS = {29, 24, 20, 16, 11, 6, 0};

    public BrewingStackScreen(BrewingStackMenu menu, Inventory inventory, Component title) {
        super(menu, new StackRecipeBookComponent(menu), inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        boolean handled = super.mouseClicked(event, doubleClick);
        if (this.getFocused() instanceof ImageButton) {
            this.setFocused(null);
        }
        return handled;
    }

    /** Sits under the left queue, in the open part of the stack. */
    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + 21, this.topPos + 45);
    }

    @Override
    protected boolean isBiggerResultSlot() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        int brewTime = menu.brewTime();
        int length = (int) (28.0F * brewTime / BrewingStackBlockEntity.BREW_TIME);
        if (length > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BREW_PROGRESS,
                    9,
                    28,
                    0,
                    0,
                    leftPos + BrewingStackMenu.PROGRESS_X,
                    topPos + BrewingStackMenu.PROGRESS_Y,
                    9,
                    length
            );
        }
        int bubbles = brewTime > 0
                ? BUBBLE_LENGTHS[(BrewingStackBlockEntity.BREW_TIME - brewTime) / 2 % BUBBLE_LENGTHS.length]
                : 0;
        if (bubbles > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BUBBLES,
                    12,
                    29,
                    0,
                    29 - bubbles,
                    leftPos + BrewingStackMenu.BUBBLE_X,
                    topPos + BrewingStackMenu.BUBBLE_Y + 29 - bubbles,
                    12,
                    bubbles
            );
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (FuelSlotHint.show(
                graphics,
                this.font,
                this.hoveredSlot,
                this.menu.getCarried(),
                BrewingStackBlockEntity.SLOT_FUEL,
                BrewingStackBlockEntity.SLOT_COUNT,
                StackBrewing::isFuel,
                Component.translatable("container.buffs_potions.requires_chorus").withStyle(ChatFormatting.RED),
                mouseX,
                mouseY
        )) {
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }
}
