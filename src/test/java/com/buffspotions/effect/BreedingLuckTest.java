package com.buffspotions.effect;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EphemeralTestServerProvider.class)
class BreedingLuckTest {
    @Test
    void oneInFiveReplacesTheSingleBaby(MinecraftServer server) {
        assertEquals(BreedingLuck.Litter.TWINS, BreedingLuck.roll(new Script(0), 0, -1));
        assertEquals(1, BreedingLuck.Litter.TWINS.extraBabies());
        assertEquals(BreedingLuck.Litter.VANILLA, BreedingLuck.roll(new Script(1), 0, -1));
        assertEquals(BreedingLuck.Litter.VANILLA, BreedingLuck.roll(new Script(4), 0, -1));

        assertEquals(BreedingLuck.Litter.QUADS, BreedingLuck.roll(new Script(0), 1, -1));
        assertEquals(3, BreedingLuck.Litter.QUADS.extraBabies());
        assertEquals(BreedingLuck.Litter.VANILLA, BreedingLuck.roll(new Script(1), 4, -1));

        assertEquals(BreedingLuck.Litter.NONE, BreedingLuck.roll(new Script(0), -1, 0));
        assertEquals(BreedingLuck.Litter.VANILLA, BreedingLuck.roll(new Script(1), -1, 0));
        assertEquals(BreedingLuck.Litter.VEX, BreedingLuck.roll(new Script(0), -1, 1));
        assertEquals(BreedingLuck.Litter.VANILLA, BreedingLuck.roll(new Script(1), -1, 3));
    }

    @Test
    void aTieLeavesTheLitterAlone(MinecraftServer server) {
        assertEquals(BreedingLuck.Litter.VANILLA, BreedingLuck.roll(new Throwing(), -1, -1));
        assertEquals(BreedingLuck.Litter.VANILLA, BreedingLuck.roll(new Throwing(), 0, 0));
        assertEquals(BreedingLuck.Litter.VANILLA, BreedingLuck.roll(new Throwing(), 2, 2));
        assertEquals(BreedingLuck.Litter.QUADS, BreedingLuck.roll(new Script(0), 1, 0));
        assertEquals(BreedingLuck.Litter.VEX, BreedingLuck.roll(new Script(0), 0, 1));
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
