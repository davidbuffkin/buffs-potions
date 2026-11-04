package com.buffspotions.flagon;

/**
 * Remembers that this mouse-hold already drank from a placed flagon.
 * The client clears it when the use key is released.
 */
public final class FlagonDrinkLatch {
    private static boolean armed;

    private FlagonDrinkLatch() {
    }

    public static void arm() {
        armed = true;
    }

    public static boolean isArmed() {
        return armed;
    }

    public static void clear() {
        armed = false;
    }
}
