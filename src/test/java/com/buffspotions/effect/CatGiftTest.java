package com.buffspotions.effect;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EphemeralTestServerProvider.class)
class CatGiftTest {
    @Test
    void luckTriplesMembraneAndMatchesTheNewGifts(MinecraftServer server) {
        assertEquals(6, CatGift.weight(CatGift.Kind.LUCK_I, CatGift.Gift.MEMBRANE));
        assertEquals(6, CatGift.weight(CatGift.Kind.LUCK_I, CatGift.Gift.WING));
        assertEquals(0, CatGift.weight(CatGift.Kind.LUCK_I, CatGift.Gift.SHARD));
        assertEquals(0, CatGift.weight(CatGift.Kind.LUCK_I, CatGift.Gift.BOTTLE));
        assertEquals(72, CatGift.total(CatGift.Kind.LUCK_I));

        assertEquals(6, CatGift.weight(CatGift.Kind.LUCK_II, CatGift.Gift.MEMBRANE));
        assertEquals(6, CatGift.weight(CatGift.Kind.LUCK_II, CatGift.Gift.WING));
        assertEquals(6, CatGift.weight(CatGift.Kind.LUCK_II, CatGift.Gift.SHARD));
        assertEquals(6, CatGift.weight(CatGift.Kind.LUCK_II, CatGift.Gift.BOTTLE));
        assertEquals(84, CatGift.total(CatGift.Kind.LUCK_II));

        assertEquals(CatGift.Gift.MEMBRANE, CatGift.roll(new Script(60), CatGift.Kind.LUCK_I));
        assertEquals(CatGift.Gift.WING, CatGift.roll(new Script(66), CatGift.Kind.LUCK_I));
        assertEquals(CatGift.Gift.SHARD, CatGift.roll(new Script(72), CatGift.Kind.LUCK_II));
        assertEquals(CatGift.Gift.BOTTLE, CatGift.roll(new Script(78), CatGift.Kind.LUCK_II));
    }

    @Test
    void badLuckAddsAMobInsteadOfAnItem(MinecraftServer server) {
        assertEquals(2, CatGift.weight(CatGift.Kind.BAD_I, CatGift.Gift.MEMBRANE));
        assertEquals(2, CatGift.weight(CatGift.Kind.BAD_I, CatGift.Gift.ENDERMITE));
        assertEquals(0, CatGift.weight(CatGift.Kind.BAD_I, CatGift.Gift.PUFFERFISH));
        assertEquals(0, CatGift.weight(CatGift.Kind.BAD_I, CatGift.Gift.WING));
        assertEquals(64, CatGift.total(CatGift.Kind.BAD_I));

        assertEquals(6, CatGift.weight(CatGift.Kind.BAD_II, CatGift.Gift.ENDERMITE));
        assertEquals(2, CatGift.weight(CatGift.Kind.BAD_II, CatGift.Gift.PUFFERFISH));
        assertEquals(2, CatGift.weight(CatGift.Kind.BAD_II, CatGift.Gift.MEMBRANE));
        assertEquals(70, CatGift.total(CatGift.Kind.BAD_II));

        assertEquals(CatGift.Gift.ENDERMITE, CatGift.roll(new Script(62), CatGift.Kind.BAD_I));
        assertEquals(CatGift.Gift.MEMBRANE, CatGift.roll(new Script(60), CatGift.Kind.BAD_I));
        assertEquals(CatGift.Gift.ENDERMITE, CatGift.roll(new Script(62), CatGift.Kind.BAD_II));
        assertEquals(CatGift.Gift.PUFFERFISH, CatGift.roll(new Script(68), CatGift.Kind.BAD_II));
    }

    @Test
    void theStrongerEffectWins(MinecraftServer server) {
        assertEquals(CatGift.Kind.VANILLA, CatGift.select(-1, -1));
        assertEquals(CatGift.Kind.VANILLA, CatGift.select(0, 0));
        assertEquals(CatGift.Kind.LUCK_I, CatGift.select(0, -1));
        assertEquals(CatGift.Kind.LUCK_II, CatGift.select(1, -1));
        assertEquals(CatGift.Kind.LUCK_II, CatGift.select(4, 0));
        assertEquals(CatGift.Kind.BAD_I, CatGift.select(-1, 0));
        assertEquals(CatGift.Kind.BAD_II, CatGift.select(-1, 1));
        assertEquals(CatGift.Kind.BAD_II, CatGift.select(0, 3));
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
}
