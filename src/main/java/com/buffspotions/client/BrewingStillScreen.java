package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.menu.BrewingStillMenu;
import com.buffspotions.still.BrewingStillBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

public final class BrewingStillScreen extends AbstractRecipeBookScreen<BrewingStillMenu> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/gui/container/brewing_still.png");
    private static final Identifier FLAME =
            Identifier.withDefaultNamespace("container/furnace/lit_progress");
    private static final Identifier BREW_PROGRESS =
            Identifier.withDefaultNamespace("container/brewing_stand/brew_progress");

    public BrewingStillScreen(BrewingStillMenu menu, Inventory inventory, Component title) {
        super(menu, new StillRecipeBookComponent(menu), inventory, title);
    }

    @Override
    public void init() {
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

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + 20, this.height / 2 - 49);
    }

    @Override
    protected boolean isBiggerResultSlot() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, 256, 256);
        if (menu.isLit()) {
            int height = Mth.ceil(menu.litProgress() * 13.0F) + 1;
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    FLAME,
                    14,
                    14,
                    0,
                    14 - height,
                    leftPos + BrewingStillMenu.FLAME_X,
                    topPos + BrewingStillMenu.FLAME_Y + 14 - height,
                    14,
                    height
            );
        }
        int length = (int) (28.0F * menu.brewTime() / BrewingStillBlockEntity.BREW_TIME);
        if (length > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    BREW_PROGRESS,
                    9,
                    28,
                    0,
                    0,
                    leftPos + BrewingStillMenu.PROGRESS_X,
                    topPos + BrewingStillMenu.PROGRESS_Y,
                    9,
                    length
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
                BrewingStillBlockEntity.SLOT_FUEL,
                BrewingStillBlockEntity.SLOT_COUNT,
                stack -> stack.is(Items.CHARCOAL),
                Component.translatable("container.buffs_potions.requires_charcoal").withStyle(ChatFormatting.RED),
                mouseX,
                mouseY
        )) {
            return;
        }
        super.extractTooltip(graphics, mouseX, mouseY);
    }
}
