package com.buffspotions.effect;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class LuckCritsTest {
    @Test
    void luckKeepsTheUpperPartOfAFullCrit(MinecraftServer server) {
        assertRange(6, 0, -1, 7, 10);
        assertRange(6, 1, -1, 8, 10);
        assertRange(15, 0, -1, 17, 23);
        assertRange(15, 1, -1, 19, 23);
    }

    @Test
    void badLuckKeepsTheLowerPartOfAFullCrit(MinecraftServer server) {
        assertRange(6, -1, 0, 6, 9);
        assertRange(6, -1, 1, 6, 8);
        assertRange(15, -1, 0, 15, 21);
        assertRange(15, -1, 1, 15, 19);
    }

    private static void assertRange(int base, int luck, int badLuck, int minHit, int maxHit) {
        RandomSource random = RandomSource.create(1);
        boolean[] seen = new boolean[maxHit - minHit + 1];
        for (int i = 0; i < 400; i++) {
            int hit = base + LuckCrits.bonus(base, luck, badLuck, random);
            assertTrue(hit >= minHit && hit <= maxHit, hit + " outside " + minHit + "-" + maxHit);
            seen[hit - minHit] = true;
        }
        for (boolean value : seen) {
            assertEquals(true, value);
        }
    }
}
