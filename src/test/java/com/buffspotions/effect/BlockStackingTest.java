package com.buffspotions.effect;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStackingTest {
    @Test
    void aPillarContinuesThroughAirAndWater() {
        assertTrue(BlockStacking.continuesThrough(Blocks.AIR.defaultBlockState()));
        assertTrue(BlockStacking.continuesThrough(Blocks.CAVE_AIR.defaultBlockState()));
        assertTrue(BlockStacking.continuesThrough(Blocks.WATER.defaultBlockState()));
        assertTrue(BlockStacking.continuesThrough(Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, 1)));
        assertFalse(BlockStacking.continuesThrough(Blocks.LAVA.defaultBlockState()));
        assertFalse(BlockStacking.continuesThrough(Blocks.STONE.defaultBlockState()));
        assertFalse(BlockStacking.continuesThrough(Blocks.KELP.defaultBlockState()));
        assertFalse(BlockStacking.continuesThrough(
                Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true)
        ));
    }
}
