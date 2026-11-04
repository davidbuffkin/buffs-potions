package com.buffspotions.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

/** Enchantment clues drawn as one tooltip row so the lines share the same spacing. */
public record InsightClueTooltip(List<Component> lines) implements TooltipComponent {
    private static final int LINE_HEIGHT = 10;

    public static final class Client implements ClientTooltipComponent {
        private final List<Component> lines;

        public Client(InsightClueTooltip tooltip) {
            this.lines = tooltip.lines();
        }

        @Override
        public int getWidth(Font font) {
            int width = 0;
            for (FormattedCharSequence line : sequences(font)) {
                width = Math.max(width, font.width(line));
            }
            return width;
        }

        @Override
        public int getHeight(Font font) {
            return sequences(font).size() * LINE_HEIGHT;
        }

        @Override
        public void extractText(GuiGraphicsExtractor graphics, Font font, int x, int y) {
            List<FormattedCharSequence> sequences = sequences(font);
            for (int index = 0; index < sequences.size(); index++) {
                graphics.text(font, sequences.get(index), x, y + index * LINE_HEIGHT, -1, true);
            }
        }

        private List<FormattedCharSequence> sequences(Font font) {
            List<FormattedCharSequence> sequences = new ArrayList<>();
            for (Component line : this.lines) {
                sequences.addAll(font.split(line, Integer.MAX_VALUE));
            }
            return sequences;
        }
    }
}
