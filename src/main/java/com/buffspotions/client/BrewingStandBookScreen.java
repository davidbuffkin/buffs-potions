package com.buffspotions.client;

import com.buffspotions.menu.StandBookMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

public final class BrewingStandBookScreen extends AbstractRecipeBookScreen<StandBookMenu> {
    private static final Identifier FUEL_LENGTH_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/fuel_length");
    private static final Identifier BREW_PROGRESS_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/brew_progress");
    private static final Identifier BUBBLES_SPRITE = Identifier.withDefaultNamespace("container/brewing_stand/bubbles");
    private static final Identifier BREWING_STAND_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/brewing_stand.png");
    private static final int[] BUBBLELENGTHS = {29, 24, 20, 16, 11, 6, 0};

    public BrewingStandBookScreen(StandBookMenu menu, Inventory inventory, Component title) {
        super(menu, new StandRecipeBookComponent(menu), inventory, title);
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

    /** Sits under the blaze-powder slot on the left side of the stand. */
    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + 16, this.topPos + 40);
    }

    @Override
    protected boolean isBiggerResultSlot() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BREWING_STAND_LOCATION,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                256,
                256
        );
        int fuelLength = Mth.clamp((18 * this.menu.getFuel() + 20 - 1) / 20, 0, 18);
        if (fuelLength > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FUEL_LENGTH_SPRITE, 18, 4, 0, 0, this.leftPos + 60, this.topPos + 44, fuelLength, 4);
        }
        int tickCount = this.menu.getBrewingTicks();
        if (tickCount > 0) {
            int length = (int) (28.0F * (1.0F - tickCount / 400.0F));
            if (length > 0) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BREW_PROGRESS_SPRITE, 9, 28, 0, 0, this.leftPos + 97, this.topPos + 16, 9, length);
            }
            length = BUBBLELENGTHS[tickCount / 2 % 7];
            if (length > 0) {
                graphics.blitSprite(
                        RenderPipelines.GUI_TEXTURED,
                        BUBBLES_SPRITE,
                        12,
                        29,
                        0,
                        29 - length,
                        this.leftPos + 63,
                        this.topPos + 14 + 29 - length,
                        12,
                        length
                );
            }
        }
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (FuelSlotHint.show(
                graphics,
                this.font,
                this.hoveredSlot,
                this.menu.getCarried(),
                4,
                5,
                stack -> stack.is(ItemTags.BREWING_FUEL),
                Component.translatable("container.buffs_potions.requires_blaze_powder").withStyle(ChatFormatting.RED),
                mouseX,
                mouseY
        )) {
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }
}
