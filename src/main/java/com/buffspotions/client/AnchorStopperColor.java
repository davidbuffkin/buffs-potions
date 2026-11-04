package com.buffspotions.client;

import java.util.UUID;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

/** The locator-bar color for a player: the low 24 bits of {@link UUID#hashCode()}, brightened to 90%. */
public final class AnchorStopperColor {
    private AnchorStopperColor() {
    }

    public static int of(@Nullable UUID player) {
        if (player == null) {
            return 0xFFFFFFFF;
        }
        return ARGB.setBrightness(ARGB.color(255, player.hashCode()), 0.9F);
    }
}
