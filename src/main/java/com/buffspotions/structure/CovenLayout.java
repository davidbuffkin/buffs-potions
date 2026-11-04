package com.buffspotions.structure;

/** Blocks saved in the coven template. Empty cells stay as the surrounding stone. */
final class CovenLayout {
    static final int SIZE_X = 12;
    static final int SIZE_Y = 17;
    static final int SIZE_Z = 17;
    /** One block of cave in front of the door, plus a step further at the door itself. */
    static final int[] DOOR_AIR = {
            5, 13, -1, 6, 13, -1, 7, 13, -1,
            5, 14, -1, 6, 14, -1, 7, 14, -1,
            6, 13, -2, 6, 14, -2
    };
    /** Air under the lower chamber's stone ceiling, one above each nether wart. */
    static final int[] DRIPSTONE = {6, 7, 11, 8, 7, 4, 4, 6, 2};
    static final int BAT_X = 6;
    static final int BAT_Y = 5;
    static final int BAT_Z = 7;

    private static final long[] BLOCKS = {
            0x00f0000000000000L, 0x000000000000f00fL, 0x0e00e00e00000000L, 0xc078070000000000L,
            0x000000001fc1fc1fL, 0x0e00e00e00000000L, 0xc1f80700000e00e0L, 0x000003fe3fe3fe1fL,
            0x3f83f80000000000L, 0x81f00603f83f83f8L, 0xf07ff7ff7ff3fc3fL, 0x3f82000000000000L,
            0x81f03f83f83f83f8L, 0xffffffff7fe7fc3fL, 0x2002000000001f8fL, 0x83f83f87f83f83f8L,
            0xfffff7fe7fe3fc1fL, 0x2000000701fcffffL, 0x83f87f83f83f8240L, 0xff7fe3fe1fc0f83fL,
            0x0000f01fcfffffffL, 0x83f83f83f8380380L, 0xfe3fe1fc0f83f83fL, 0x0f01fcfffffffff7L,
            0x83f83f8000000000L, 0xfc1f80f03f83f83fL, 0x3f8ffeffeffe7fc3L, 0x83f80000000000f0L,
            0xf80f03f83f83f83fL, 0xffeffeffe7fc3fc1L, 0x00000000000f03f8L, 0xe00000e00e00e004L,
            0xffeffe7fc7f83f01L, 0x00000000e03f8ffeL, 0x000e00e00e004000L, 0xffe7fc7f83f01e00L,
            0x00000803f8ffeffeL, 0xe00e00e004004000L, 0x7f87f03e01c00000L, 0x00003f0ffeffeffeL,
            0xe00e004004004000L, 0x3e01c00000000e00L, 0x01c07fc7fc7fc3f0L, 0xe004004004024020L,
            0x0000000000e00e00L, 0x03f83f83f81e01c0L, 0x400403c03c038000L, 0x0000000e00e00e00L,
            0x00701f0000000000L, 0x003803803c01e00fL, 0x0000e00e00e00000L, 0x0000000000000000L,
            0x0000000000000000L, 0x0000000000000000L, 0x0000000000000000L
    };

    private CovenLayout() {
    }

    static boolean contains(int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= SIZE_X || y >= SIZE_Y || z >= SIZE_Z) {
            return false;
        }
        int index = x + SIZE_X * (y + SIZE_Y * z);
        return (BLOCKS[index >>> 6] & (1L << (index & 63))) != 0L;
    }
}
