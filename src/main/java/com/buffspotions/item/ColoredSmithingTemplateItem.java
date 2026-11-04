package com.buffspotions.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;

/** A smithing template whose inventory name uses one color. */
public final class ColoredSmithingTemplateItem extends SmithingTemplateItem {
    private final ChatFormatting nameColor;

    public ColoredSmithingTemplateItem(
            Component appliesTo,
            Component ingredients,
            Component baseSlotDescription,
            Component additionsSlotDescription,
            List<Identifier> baseSlotEmptyIcons,
            List<Identifier> additionalSlotEmptyIcons,
            ChatFormatting nameColor,
            Properties properties
    ) {
        super(
                appliesTo,
                ingredients,
                baseSlotDescription,
                additionsSlotDescription,
                baseSlotEmptyIcons,
                additionalSlotEmptyIcons,
                properties
        );
        this.nameColor = nameColor;
    }

    @Override
    public Component getName(ItemStack stack) {
        return super.getName(stack).copy().withStyle(this.nameColor);
    }
}
