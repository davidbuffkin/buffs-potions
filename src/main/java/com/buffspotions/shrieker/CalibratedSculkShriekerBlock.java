package com.buffspotions.shrieker;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class CalibratedSculkShriekerBlock extends BaseEntityBlock {
    public static final MapCodec<CalibratedSculkShriekerBlock> CODEC = simpleCodec(CalibratedSculkShriekerBlock::new);

    public CalibratedSculkShriekerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<CalibratedSculkShriekerBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CalibratedSculkShriekerBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        acceptSignal(level, pos);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighbor,
            @Nullable Orientation orientation,
            boolean movedByPiston
    ) {
        acceptSignal(level, pos);
    }

    private static void acceptSignal(Level level, BlockPos pos) {
        if (level instanceof ServerLevel server
                && level.getBlockEntity(pos) instanceof CalibratedSculkShriekerBlockEntity shrieker) {
            shrieker.onSignal(server, level.hasNeighborSignal(pos));
        }
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        return interact(level, pos, player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return interact(level, pos, player);
    }

    private static InteractionResult interact(Level level, BlockPos pos, Player player) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CalibratedSculkShriekerBlockEntity shrieker) {
            if (player.isSecondaryUseActive()) {
                shrieker.toggleListening(serverPlayer);
            } else {
                shrieker.open(serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
