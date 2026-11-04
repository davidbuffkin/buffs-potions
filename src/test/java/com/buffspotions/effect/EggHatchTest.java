package com.buffspotions.effect;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EphemeralTestServerProvider.class)
class EggHatchTest {
    @Test
    void luckReplacesTheChickCount(MinecraftServer server) {
        assertEquals(16, chicks(EggHatch.Kind.LUCK_I, 0, 0, 0));
        assertEquals(4, chicks(EggHatch.Kind.LUCK_I, 0, 0, 1));
        assertEquals(1, chicks(EggHatch.Kind.LUCK_I, 0, 1));
        assertEquals(0, chicks(EggHatch.Kind.LUCK_I, 1));

        assertEquals(64, chicks(EggHatch.Kind.LUCK_II, 0, 0, 0, 0));
        assertEquals(16, chicks(EggHatch.Kind.LUCK_II, 0, 0, 0, 1));
        assertEquals(4, chicks(EggHatch.Kind.LUCK_II, 0, 0, 1));
        assertEquals(1, chicks(EggHatch.Kind.LUCK_II, 0, 1));
        assertEquals(0, chicks(EggHatch.Kind.LUCK_II, 1));
    }

    @Test
    void badLuckTradesExtraChicksForMobs(MinecraftServer server) {
        EggHatch.Result chick = EggHatch.roll(new Script(0), EggHatch.Kind.BAD_I);
        assertEquals(new EggHatch.Result(1, false, false), chick);
        assertEquals(new EggHatch.Result(0, true, false), EggHatch.roll(new Script(1, 0), EggHatch.Kind.BAD_I));
        assertEquals(new EggHatch.Result(0, false, false), EggHatch.roll(new Script(1, 1), EggHatch.Kind.BAD_I));

        assertEquals(new EggHatch.Result(1, false, false), EggHatch.roll(new Script(0), EggHatch.Kind.BAD_II));
        Script creeper = new Script(1, 0, 0);
        assertEquals(new EggHatch.Result(0, true, true), EggHatch.roll(creeper, EggHatch.Kind.BAD_II));
        assertEquals(64, creeper.lastBound());
        assertEquals(new EggHatch.Result(0, false, true), EggHatch.roll(new Script(1, 1, 0), EggHatch.Kind.BAD_II));
        assertEquals(new EggHatch.Result(0, true, false), EggHatch.roll(new Script(1, 0, 1), EggHatch.Kind.BAD_II));
    }

    @Test
    void theStrongerEffectWins(MinecraftServer server) {
        assertEquals(EggHatch.Kind.VANILLA, EggHatch.select(-1, -1));
        assertEquals(EggHatch.Kind.VANILLA, EggHatch.select(0, 0));
        assertEquals(EggHatch.Kind.LUCK_I, EggHatch.select(0, -1));
        assertEquals(EggHatch.Kind.LUCK_II, EggHatch.select(1, -1));
        assertEquals(EggHatch.Kind.LUCK_II, EggHatch.select(4, 0));
        assertEquals(EggHatch.Kind.BAD_I, EggHatch.select(-1, 0));
        assertEquals(EggHatch.Kind.BAD_II, EggHatch.select(-1, 1));
        assertEquals(EggHatch.Kind.BAD_II, EggHatch.select(0, 3));
    }

    private static int chicks(EggHatch.Kind kind, int... rolls) {
        return EggHatch.roll(new Script(rolls), kind).chickens();
    }

    private static final class Script implements RandomSource {
        private final int[] rolls;
        private int index;
        private int lastBound;

        private Script(int... rolls) {
            this.rolls = rolls;
        }

        private int lastBound() {
            return this.lastBound;
        }

        @Override
        public int nextInt(int bound) {
            this.lastBound = bound;
            return this.rolls[this.index++];
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
