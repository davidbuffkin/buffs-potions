package com.buffspotions.skin;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.jspecify.annotations.Nullable;

/** Width, height, and pixels packed so a world can keep a skin without asking the network again. */
public final class SkinPixels {
    public static final int MAX_BYTES = 8 + 64 * 64 * 4;

    private SkinPixels() {
    }

    public static byte[] pack(int width, int height, int[] argb) {
        ByteBuffer buffer = ByteBuffer.allocate(8 + argb.length * 4).order(ByteOrder.BIG_ENDIAN);
        buffer.putInt(width);
        buffer.putInt(height);
        for (int pixel : argb) {
            buffer.putInt(pixel);
        }
        return buffer.array();
    }

    public static boolean skin(byte[] packed) {
        return size(packed, true);
    }

    public static boolean extra(byte[] packed) {
        return packed.length == 0 || size(packed, false);
    }

    public static @Nullable Image unpack(byte[] packed) {
        if (!size(packed, false)) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.wrap(packed).order(ByteOrder.BIG_ENDIAN);
        int width = buffer.getInt();
        int height = buffer.getInt();
        int[] argb = new int[width * height];
        for (int i = 0; i < argb.length; i++) {
            argb[i] = buffer.getInt();
        }
        return new Image(width, height, argb);
    }

    private static boolean size(byte[] packed, boolean required) {
        if (packed == null || packed.length < 8) {
            return !required && packed != null && packed.length == 0;
        }
        if (packed.length > MAX_BYTES) {
            return false;
        }
        ByteBuffer buffer = ByteBuffer.wrap(packed).order(ByteOrder.BIG_ENDIAN);
        int width = buffer.getInt();
        int height = buffer.getInt();
        if (width != 64 || (height != 64 && height != 32)) {
            return false;
        }
        return buffer.remaining() == width * height * 4;
    }

    public record Image(int width, int height, int[] argb) {
    }
}
