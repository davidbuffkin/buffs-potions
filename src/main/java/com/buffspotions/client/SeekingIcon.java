package com.buffspotions.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;

/**
 * Draws Seeking's icon as the compass the player is holding in their inventory.
 * The needle uses the same angle, wobble, and search spin as that compass.
 */
public final class SeekingIcon implements IClientMobEffectExtensions {
    private static final int ITEM_SIZE = 16;
    /** Created on first draw. Item components are not bound when this class loads. */
    private static ItemStack compass;

    @Override
    public boolean extractInventoryIcon(
            MobEffectInstance instance,
            AbstractContainerScreen<?> screen,
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        return compass(graphics, x, y, width, height);
    }

    @Override
    public boolean extractHudIcon(
            MobEffectInstance instance,
            Hud hud,
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        return compass(graphics, x, y, width, height);
    }

    private static boolean compass(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        if (compass == null) {
            compass = new ItemStack(Items.COMPASS);
        }
        graphics.item(compass, x + (width - ITEM_SIZE) / 2, y + (height - ITEM_SIZE) / 2);
        return true;
    }
}
