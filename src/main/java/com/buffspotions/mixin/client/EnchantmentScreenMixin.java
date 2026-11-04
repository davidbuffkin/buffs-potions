package com.buffspotions.mixin.client;

import com.buffspotions.client.InsightClueTooltip;
import com.buffspotions.client.InsightClues;
import com.mojang.datafixers.util.Either;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(EnchantmentScreen.class)
abstract class EnchantmentScreenMixin extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<net.minecraft.world.inventory.EnchantmentMenu> {
    protected EnchantmentScreenMixin() {
        super(null, null, null);
    }

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;setComponentTooltipForNextFrame(Lnet/minecraft/client/gui/Font;Ljava/util/List;II)V"
            )
    )
    private void buffsPotions$insightClues(GuiGraphicsExtractor graphics, Font font, List<Component> lines, int mouseX, int mouseY) {
        if (this.minecraft == null || this.minecraft.player == null || lines.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        int slot = -1;
        for (int offer = 0; offer < 3; offer++) {
            if (this.isHovering(60, 14 + 19 * offer, 108, 17, mouseX, mouseY) && this.menu.costs[offer] > 0) {
                slot = offer;
                break;
            }
        }
        List<Component> extra = slot < 0 ? List.of() : InsightClues.extraLines(this.menu, slot, this.minecraft.player);
        if (extra.isEmpty()) {
            graphics.setComponentTooltipForNextFrame(font, lines, mouseX, mouseY);
            return;
        }
        List<Component> clues = new ArrayList<>(extra.size() + 1);
        clues.add(lines.getFirst());
        clues.addAll(extra);
        for (int index = 0; index < clues.size() - 1; index++) {
            clues.set(index, InsightClues.settled(clues.get(index)));
        }
        List<Either<FormattedText, TooltipComponent>> elements = new ArrayList<>(lines.size());
        elements.add(Either.right(new InsightClueTooltip(List.copyOf(clues))));
        for (int index = 1; index < lines.size(); index++) {
            elements.add(Either.left(lines.get(index)));
        }
        graphics.setComponentTooltipFromElementsForNextFrame(font, elements, mouseX, mouseY, ItemStack.EMPTY);
    }
}
