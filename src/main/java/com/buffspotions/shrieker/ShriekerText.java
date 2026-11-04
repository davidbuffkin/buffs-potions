package com.buffspotions.shrieker;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.StringUtil;

/** Legacy section-sign formatting for a calibrated shrieker's saved message. */
public final class ShriekerText {
    public static final int MAX_LENGTH = 1024;

    private ShriekerText() {
    }

    /** Keeps section signs and ordinary chat characters, and drops the rest. */
    public static String clean(String input) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < input.length() && builder.length() < MAX_LENGTH; i++) {
            char character = input.charAt(i);
            if (character == ChatFormatting.PREFIX_CODE || StringUtil.isAllowedChatCharacter(character)) {
                builder.append(character);
            }
        }
        return builder.toString();
    }

    public static MutableComponent parse(String raw) {
        MutableComponent result = Component.empty();
        StringBuilder run = new StringBuilder();
        Style style = Style.EMPTY;
        for (int i = 0; i < raw.length(); i++) {
            char character = raw.charAt(i);
            if (character == ChatFormatting.PREFIX_CODE && i + 1 < raw.length()) {
                ChatFormatting format = ChatFormatting.getByCode(raw.charAt(i + 1));
                if (format != null) {
                    appendRun(result, run, style);
                    style = style.applyLegacyFormat(format);
                    i++;
                    continue;
                }
            }
            run.append(character);
        }
        appendRun(result, run, style);
        return result;
    }

    private static void appendRun(MutableComponent result, StringBuilder run, Style style) {
        if (run.isEmpty()) {
            return;
        }
        result.append(Component.literal(run.toString()).withStyle(style));
        run.setLength(0);
    }
}
