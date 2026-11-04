package com.buffspotions.shrieker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShriekerTest {
    @Test
    void formattingCodesStayInTheEditorAndColorTheChatText() {
        assertEquals("H§ci", ShriekerText.clean("H§ci\n"));
        var text = ShriekerText.parse("A§cB");
        assertEquals("AB", text.getString());
        List<TextColor> colors = new ArrayList<>();
        text.visit((Style style, String part) -> {
            colors.add(style.getColor());
            return Optional.empty();
        }, Style.EMPTY);
        assertEquals(null, colors.get(0));
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.RED), colors.get(1));
    }

    @Test
    void aRisingEdgeSendsOnlyAfterTheCooldown() {
        assertTrue(CalibratedSculkShriekerBlockEntity.shouldSend(false, true, 100, 100));
        assertFalse(CalibratedSculkShriekerBlockEntity.shouldSend(true, true, 200, 100));
        assertFalse(CalibratedSculkShriekerBlockEntity.shouldSend(false, false, 200, 100));
        assertFalse(CalibratedSculkShriekerBlockEntity.shouldSend(false, true, 139, 140));
        assertTrue(CalibratedSculkShriekerBlockEntity.shouldSend(false, true, 140, 140));
    }
}
