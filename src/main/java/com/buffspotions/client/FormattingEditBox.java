package com.buffspotions.client;

import com.buffspotions.mixin.client.EditBoxAccess;
import com.buffspotions.shrieker.ShriekerText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.network.chat.Component;

/** An edit box that keeps section-sign formatting codes. */
public final class FormattingEditBox extends EditBox {
    public FormattingEditBox(Font font, int x, int y, int width, int height, Component narration) {
        super(font, x, y, width, height, narration);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (event.codepoint() == ChatFormatting.PREFIX_CODE && this.canConsumeInput()) {
            this.insertText(event.codepointAsString());
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public void insertText(String input) {
        EditBoxAccess access = (EditBoxAccess) (EditBox) this;
        String value = access.buffsPotions$value();
        int start = Math.min(access.buffsPotions$cursorPos(), access.buffsPotions$highlightPos());
        int end = Math.max(access.buffsPotions$cursorPos(), access.buffsPotions$highlightPos());
        int room = access.buffsPotions$maxLength() - value.length() - (start - end);
        if (room <= 0) {
            return;
        }
        String text = ShriekerText.clean(input);
        if (text.length() > room) {
            if (Character.isHighSurrogate(text.charAt(room - 1))) {
                room--;
            }
            text = text.substring(0, room);
        }
        String next = new StringBuilder(value).replace(start, end, text).toString();
        access.buffsPotions$value(next);
        access.buffsPotions$onValueChange(next);
        int cursor = start + text.length();
        this.setCursorPosition(cursor);
        this.setHighlightPos(cursor);
    }
}
