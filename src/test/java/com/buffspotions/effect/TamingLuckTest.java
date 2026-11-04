package com.buffspotions.effect;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class TamingLuckTest {
    @Test
    void theStrongerEffectWins(MinecraftServer server) {
        assertEquals(TamingLuck.Kind.VANILLA, TamingLuck.select(-1, -1));
        assertEquals(TamingLuck.Kind.VANILLA, TamingLuck.select(0, 0));
        assertEquals(TamingLuck.Kind.LUCK_I, TamingLuck.select(0, -1));
        assertEquals(TamingLuck.Kind.LUCK_II, TamingLuck.select(1, -1));
        assertEquals(TamingLuck.Kind.LUCK_II, TamingLuck.select(4, 0));
        assertEquals(TamingLuck.Kind.BAD_I, TamingLuck.select(-1, 0));
        assertEquals(TamingLuck.Kind.BAD_II, TamingLuck.select(-1, 1));
        assertEquals(TamingLuck.Kind.BAD_II, TamingLuck.select(0, 3));
    }

    @Test
    void itemOddsHalveDoubleAndQuadruple(MinecraftServer server) {
        assertTrue(TamingLuck.itemSucceeds(new Throwing(), 3, 1, -1));
        assertTrue(TamingLuck.itemSucceeds(new Script(1), 3, 0, -1));
        assertFalse(TamingLuck.itemSucceeds(new Script(2), 3, 0, -1));
        assertTrue(TamingLuck.itemSucceeds(new Script(0), 3, -1, 0));
        assertFalse(TamingLuck.itemSucceeds(new Script(1), 3, -1, 0));
        assertTrue(TamingLuck.itemSucceeds(new Script(0), 3, -1, 1));
        assertFalse(TamingLuck.itemSucceeds(new Script(1), 3, -1, 1));

        assertTrue(TamingLuck.itemSucceeds(new Script(1), 10, 0, -1));
        assertFalse(TamingLuck.itemSucceeds(new Script(2), 10, 0, -1));
        assertFalse(TamingLuck.itemSucceeds(new Script(1), 10, -1, 0));
        assertFalse(TamingLuck.itemSucceeds(new Script(1), 10, -1, 1));

        assertTrue(TamingLuck.itemSucceeds(new Script(0), 3, 0, 0));
        assertFalse(TamingLuck.itemSucceeds(new Script(1), 3, 0, 0));
        assertTrue(TamingLuck.itemSucceeds(new Throwing(), 10, 4, 0));
    }

    @Test
    void temperGainMatchesTheSameFactors(MinecraftServer server) {
        assertEquals(10, TamingLuck.temperGain(new Throwing(), 5, 0, -1));
        assertEquals(6, TamingLuck.temperGain(new Throwing(), 3, 0, -1));
        assertEquals(5, TamingLuck.temperGain(new Throwing(), 5, 1, -1));
        assertEquals(5, TamingLuck.temperGain(new Throwing(), 5, 0, 0));

        assertEquals(3, TamingLuck.temperGain(new Script(0), 5, -1, 0));
        assertEquals(2, TamingLuck.temperGain(new Script(1), 5, -1, 0));
        assertEquals(2, TamingLuck.temperGain(new Script(0), 5, -1, 1));
        assertEquals(1, TamingLuck.temperGain(new Script(1), 5, -1, 1));
        assertEquals(1, TamingLuck.temperGain(new Script(0), 3, -1, 1));
        assertEquals(0, TamingLuck.temperGain(new Script(3), 3, -1, 1));
    }

    @Test
    void theFirstRideTamesAtLuckTwo(MinecraftServer server) {
        assertTrue(TamingLuck.mountSucceeds(new Throwing(), 0, 100, 1, -1));
        assertTrue(TamingLuck.mountSucceeds(new Throwing(), 0, 30, 4, 0));
        assertFalse(TamingLuck.mountSucceeds(new Script(0), 0, 100, 0, -1));
        assertTrue(TamingLuck.mountSucceeds(new Script(19), 10, 100, 0, -1));
        assertFalse(TamingLuck.mountSucceeds(new Script(20), 10, 100, 0, -1));
        assertTrue(TamingLuck.mountSucceeds(new Script(9), 10, 100, -1, 0));
        assertFalse(TamingLuck.mountSucceeds(new Script(10), 10, 100, -1, 0));
        assertTrue(TamingLuck.mountSucceeds(new Script(9), 10, 100, -1, 1));
        assertFalse(TamingLuck.mountSucceeds(new Script(10), 10, 100, -1, 1));
        assertFalse(TamingLuck.mountSucceeds(new Script(10), 10, 100, 0, 0));
    }

    @Test
    void rideCountsLandNearHalfTwiceAndFourTimes(MinecraftServer server) {
        double horses = averageRides(100, -1, -1);
        double llamas = averageRides(30, -1, -1);
        assertEquals(1.0, averageRides(100, 1, -1));
        assertNear(0.5, averageRides(100, 0, -1) / horses);
        assertNear(2.0, averageRides(100, -1, 0) / horses);
        assertNear(4.0, averageRides(100, -1, 1) / horses);
        assertNear(0.5, averageRides(30, 0, -1) / llamas);
        assertNear(2.0, averageRides(30, -1, 0) / llamas);
        assertNear(4.0, averageRides(30, -1, 1) / llamas);
    }

    private static void assertNear(double target, double ratio) {
        assertTrue(ratio > target * 0.75 && ratio < target * 1.25, ratio + " vs " + target);
    }

    private static double averageRides(int maxTemper, int luck, int badLuck) {
        RandomSource random = RandomSource.create(42);
        int trials = 4000;
        long total = 0;
        for (int i = 0; i < trials; i++) {
            int temper = 0;
            int rides = 0;
            while (true) {
                rides++;
                if (TamingLuck.mountSucceeds(random, temper, maxTemper, luck, badLuck)) {
                    break;
                }
                temper = Math.min(maxTemper, temper + TamingLuck.temperGain(random, 5, luck, badLuck));
                if (rides > 10000) {
                    throw new AssertionError("tame did not finish");
                }
            }
            total += rides;
        }
        return total / (double) trials;
    }

    private static final class Script implements RandomSource {
        private final int roll;

        private Script(int roll) {
            this.roll = roll;
        }

        @Override
        public int nextInt(int bound) {
            return this.roll;
        }

        @Override
        public RandomSource fork() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSeed(long seed) {
        }

        @Override
        public int nextInt() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean nextBoolean() {
            throw new UnsupportedOperationException();
        }

        @Override
        public float nextFloat() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double nextDouble() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double nextGaussian() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class Throwing implements RandomSource {
        @Override
        public int nextInt(int bound) {
            throw new AssertionError("roll");
        }

        @Override
        public RandomSource fork() {
            throw new UnsupportedOperationException();
        }

        @Override
        public PositionalRandomFactory forkPositional() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setSeed(long seed) {
        }

        @Override
        public int nextInt() {
            throw new UnsupportedOperationException();
        }

        @Override
        public long nextLong() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean nextBoolean() {
            throw new UnsupportedOperationException();
        }

        @Override
        public float nextFloat() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double nextDouble() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double nextGaussian() {
            throw new UnsupportedOperationException();
        }
    }
}
