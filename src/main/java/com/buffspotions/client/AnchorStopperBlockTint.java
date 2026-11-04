package com.buffspotions.client;

import com.buffspotions.flagon.AnchoredEnderFlagonBlockEntity;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Tints a placed anchored flagon's stopper. Break particles stay the texture color. */
public final class AnchorStopperBlockTint implements BlockTintSource {
    @Override
    public int color(BlockState state) {
        return 0xFFFFFFFF;
    }

    @Override
    public int colorInWorld(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AnchoredEnderFlagonBlockEntity flagon) {
            return AnchorStopperColor.of(flagon.playerId());
        }
        return 0xFFFFFFFF;
    }

    @Override
    public int colorAsTerrainParticle(BlockState state, BlockAndTintGetter level, BlockPos pos) {
        return 0xFFFFFFFF;
    }
}
