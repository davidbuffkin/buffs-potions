package com.buffspotions.bellows;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class BellowsOxidationTest {
    @Test
    void unwaxedCopperAdvancesOneStageAndWaxedCopperDoesNot(MinecraftServer server) {
        Block fresh = Blocks.COPPER_BLOCK.weathering().unaffected();
        Block exposedBlock = Blocks.COPPER_BLOCK.weathering().exposed();
        Block weatheredBlock = Blocks.COPPER_BLOCK.weathering().weathered();
        Block oxidizedBlock = Blocks.COPPER_BLOCK.weathering().oxidized();
        Block waxed = Blocks.COPPER_BLOCK.waxed().exposed();
        assertTrue(BellowsBoosts.canOxidize(fresh.defaultBlockState()));
        assertFalse(BellowsBoosts.canOxidize(waxed.defaultBlockState()));
        assertFalse(BellowsBoosts.canOxidize(oxidizedBlock.defaultBlockState()));

        BlockState exposed = WeatheringCopper.getNext(fresh).map(block -> block.withPropertiesOf(fresh.defaultBlockState())).orElseThrow();
        assertTrue(exposed.is(exposedBlock));
        BlockState weathered = WeatheringCopper.getNext(exposed.getBlock()).map(block -> block.withPropertiesOf(exposed)).orElseThrow();
        assertTrue(weathered.is(weatheredBlock));
        BlockState oxidized = WeatheringCopper.getNext(weathered.getBlock()).map(block -> block.withPropertiesOf(weathered)).orElseThrow();
        assertTrue(oxidized.is(oxidizedBlock));
        assertTrue(WeatheringCopper.getNext(oxidized.getBlock()).isEmpty());

        BlockState stairs = Blocks.CUT_COPPER_STAIRS.weathering().unaffected().defaultBlockState()
                .setValue(StairBlock.FACING, Direction.WEST);
        BlockState nextStairs = WeatheringCopper.getNext(stairs.getBlock()).map(block -> block.withPropertiesOf(stairs)).orElseThrow();
        assertEquals(Direction.WEST, nextStairs.getValue(StairBlock.FACING));

        for (ServerLevel level : server.getAllLevels()) {
            BlockPos pos = new BlockPos(4, level.getMinY() + 4, 4);
            level.setBlockAndUpdate(pos, fresh.defaultBlockState());
            assertTrue(BellowsBoosts.oxidize(level, pos));
            assertTrue(level.getBlockState(pos).is(exposedBlock));
            level.setBlockAndUpdate(pos, waxed.defaultBlockState());
            assertFalse(BellowsBoosts.oxidize(level, pos));
            assertTrue(level.getBlockState(pos).is(waxed));
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            break;
        }
    }

    @Test
    void bellowsUseBringsTheGolemCountdownForwardUnlessWaxed() {
        assertEquals(1_000L - CopperGolemBellows.OXIDATION_HURRY, CopperGolemBellows.hurriedDeadline(1_000L));
        assertEquals(CopperGolemBellows.OXIDATION_HURRY, 320);
        assertEquals(-2L, CopperGolemBellows.hurriedDeadline(-2L));
        assertEquals(-1L, CopperGolemBellows.hurriedDeadline(-1L));
        assertEquals(0L, CopperGolemBellows.hurriedDeadline(0L));
    }

    @Test
    void aGolemOxidizesOnlyCopperBehindItsOwnStage(MinecraftServer server) {
        Block fresh = Blocks.COPPER_BLOCK.weathering().unaffected();
        Block exposed = Blocks.COPPER_BLOCK.weathering().exposed();
        Block weathered = Blocks.COPPER_BLOCK.weathering().weathered();
        Block waxed = Blocks.COPPER_BLOCK.waxed().unaffected();
        assertFalse(BellowsBoosts.lessOxidizedThan(fresh.defaultBlockState(), WeatheringCopper.WeatherState.UNAFFECTED));
        assertTrue(BellowsBoosts.lessOxidizedThan(fresh.defaultBlockState(), WeatheringCopper.WeatherState.EXPOSED));
        assertFalse(BellowsBoosts.lessOxidizedThan(exposed.defaultBlockState(), WeatheringCopper.WeatherState.EXPOSED));
        assertTrue(BellowsBoosts.lessOxidizedThan(exposed.defaultBlockState(), WeatheringCopper.WeatherState.WEATHERED));
        assertTrue(BellowsBoosts.lessOxidizedThan(weathered.defaultBlockState(), WeatheringCopper.WeatherState.OXIDIZED));
        assertFalse(BellowsBoosts.lessOxidizedThan(waxed.defaultBlockState(), WeatheringCopper.WeatherState.OXIDIZED));
        assertEquals(0.5F, BellowsBoosts.USE_EXHAUSTION);
        assertEquals(0.5, BellowsBoosts.KNOCKBACK);

        for (ServerLevel level : server.getAllLevels()) {
            BlockPos pos = new BlockPos(6, level.getMinY() + 4, 6);
            level.setBlockAndUpdate(pos, Blocks.COBWEB.defaultBlockState());
            assertTrue(BellowsBoosts.clearCobweb(level, pos));
            assertTrue(level.getBlockState(pos).isAir());
            assertTrue(level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0)).stream()
                    .anyMatch(item -> item.getItem().is(Items.STRING)));
            break;
        }
    }
}
