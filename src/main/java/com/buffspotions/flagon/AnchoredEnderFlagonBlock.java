package com.buffspotions.flagon;

import com.buffspotions.belt.BeltKey;
import com.buffspotions.item.FlagonBeltItem;
import com.buffspotions.registry.ModDataComponents;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class AnchoredEnderFlagonBlock extends BaseEntityBlock {
    public static final MapCodec<AnchoredEnderFlagonBlock> CODEC = simpleCodec(AnchoredEnderFlagonBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(4.0, 0.0, 4.0, 12.0, 12.0, 12.0);

    public AnchoredEnderFlagonBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MapCodec<AnchoredEnderFlagonBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof AnchoredEnderFlagonBlockEntity flagon && flagon.anchor() != null) {
            stack.set(ModDataComponents.FLAGON_ANCHOR.get(), flagon.anchor());
            stack.set(ModDataComponents.FLAGON_CONTENTS.get(), flagon.contents());
        }
        return stack;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!(level.getBlockEntity(pos) instanceof AnchoredEnderFlagonBlockEntity flagon)) {
            return InteractionResult.PASS;
        }
        if (BeltKey.isDown(player)) {
            ItemStack picked = new ItemStack(this);
            if (flagon.anchor() != null) {
                picked.set(ModDataComponents.FLAGON_ANCHOR.get(), flagon.anchor());
                picked.set(ModDataComponents.FLAGON_CONTENTS.get(), flagon.contents());
            }
            if (FlagonBeltItem.pickup(player, level, pos, picked)) {
                return InteractionResult.SUCCESS;
            }
        }
        return FlagonInteractions.useAnchored(flagon, player, hand);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (level.getBlockEntity(pos) instanceof AnchoredEnderFlagonBlockEntity flagon) {
            return flagon.contents().doses();
        }
        return 0;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(15) != 0) {
            return;
        }
        Direction side = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        double along = 0.25 + random.nextDouble() * 0.5;
        double x = side.getStepX() == 0 ? along : 0.5 + side.getStepX() * 0.3;
        double z = side.getStepZ() == 0 ? along : 0.5 + side.getStepZ() * 0.3;
        level.addParticle(
                ParticleTypes.DRIPPING_OBSIDIAN_TEAR,
                pos.getX() + x,
                pos.getY() + 0.3 + random.nextDouble() * 0.45,
                pos.getZ() + z,
                0.0,
                0.0,
                0.0
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnchoredEnderFlagonBlockEntity(pos, state);
    }
}
