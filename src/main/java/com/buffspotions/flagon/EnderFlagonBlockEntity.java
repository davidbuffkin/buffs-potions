package com.buffspotions.flagon;

import com.buffspotions.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Present so the block can draw liquid. The liquid itself lives on the player, not here,
 * and nothing that moves items (hoppers, droppers) has a container to talk to.
 */
public final class EnderFlagonBlockEntity extends BlockEntity {
    public EnderFlagonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENDER_FLAGON.get(), pos, state);
    }
}
