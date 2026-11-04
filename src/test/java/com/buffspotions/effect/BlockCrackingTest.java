package com.buffspotions.effect;

import net.minecraft.world.level.block.Blocks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockCrackingTest {
    @Test
    void aHoleContinuesOnlyThroughTheBlockThatStartedIt() {
        assertTrue(BlockCracking.sameCrack(Blocks.STONE.defaultBlockState(), Blocks.STONE));
        assertFalse(BlockCracking.sameCrack(Blocks.AIR.defaultBlockState(), Blocks.STONE));
        assertFalse(BlockCracking.sameCrack(Blocks.CAVE_AIR.defaultBlockState(), Blocks.STONE));
        assertFalse(BlockCracking.sameCrack(Blocks.COBBLESTONE.defaultBlockState(), Blocks.STONE));
        assertFalse(BlockCracking.sameCrack(Blocks.WATER.defaultBlockState(), Blocks.STONE));
    }
}
