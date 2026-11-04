package com.buffspotions.stack;

import com.buffspotions.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class BrewingStackBlock extends BaseEntityBlock {
    public static final MapCodec<BrewingStackBlock> CODEC = simpleCodec(BrewingStackBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            Block.box(2.0, 0.0, 2.0, 14.0, 2.0, 14.0),
            Block.box(7.0, 3.0, 7.0, 9.0, 15.0, 9.0),
            Block.box(6.0, 15.0, 2.0, 10.0, 16.0, 10.0),
            Block.box(6.0, 0.0, 0.0, 10.0, 16.0, 2.0)
    );
    private static final VoxelShape[] SHAPES = facingShapes(SOUTH_SHAPE);

    public BrewingStackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public MapCodec<BrewingStackBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BrewingStackBlockEntity stack) {
            player.openMenu(stack);
        }
        return InteractionResult.SUCCESS;
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
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static VoxelShape[] facingShapes(VoxelShape south) {
        VoxelShape west = rotateClockwise(south);
        VoxelShape north = rotateClockwise(west);
        VoxelShape east = rotateClockwise(north);
        VoxelShape[] shapes = new VoxelShape[4];
        shapes[Direction.SOUTH.get2DDataValue()] = south;
        shapes[Direction.WEST.get2DDataValue()] = west;
        shapes[Direction.NORTH.get2DDataValue()] = north;
        shapes[Direction.EAST.get2DDataValue()] = east;
        return shapes;
    }

    private static VoxelShape rotateClockwise(VoxelShape shape) {
        List<VoxelShape> boxes = new ArrayList<>();
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                boxes.add(Shapes.box(1.0 - maxZ, minY, minX, 1.0 - minZ, maxY, maxX)));
        VoxelShape rotated = Shapes.empty();
        for (VoxelShape box : boxes) {
            rotated = Shapes.or(rotated, box);
        }
        return rotated;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BrewingStackBlockEntity stack && stack.isWorking()) {
            // One mote a tick, on the glass. An idle end rod sparks once every five ticks.
            spawnOnFlagon(level, pos, random);
            return;
        }
        if (random.nextInt(5) != 0) {
            return;
        }
        Direction up = Direction.UP;
        double x = pos.getX() + 0.55 - random.nextFloat() * 0.1F;
        double y = pos.getY() + 0.55 - random.nextFloat() * 0.1F;
        double z = pos.getZ() + 0.55 - random.nextFloat() * 0.1F;
        double along = 0.4F - (random.nextFloat() + random.nextFloat()) * 0.4F;
        level.addParticle(
                ParticleTypes.END_ROD,
                x + up.getStepX() * along,
                y + up.getStepY() * along,
                z + up.getStepZ() * along,
                random.nextGaussian() * 0.005,
                random.nextGaussian() * 0.005,
                random.nextGaussian() * 0.005
        );
    }

    /** A point just outside the flagon rendered in this block. The model is lifted two pixels. */
    private static void spawnOnFlagon(Level level, BlockPos pos, RandomSource random) {
        double outside = 1.0 / 16.0;
        double bodyMin = 4.0 / 16.0;
        double bodyMax = 12.0 / 16.0;
        double yMin = 2.0 / 16.0;
        double yMax = 14.0 / 16.0;
        double x;
        double y;
        double z;
        int face = random.nextInt(5);
        if (face == 4) {
            double neckMin = 5.0 / 16.0;
            double neckMax = 11.0 / 16.0;
            x = bodyMin + random.nextDouble() * (bodyMax - bodyMin);
            z = bodyMin + random.nextDouble() * (bodyMax - bodyMin);
            boolean neck = x > neckMin && x < neckMax && z > neckMin && z < neckMax;
            y = (neck ? 15.0 : 14.0) / 16.0 + outside;
        } else {
            double along = bodyMin + random.nextDouble() * (bodyMax - bodyMin);
            y = yMin + random.nextDouble() * (yMax - yMin);
            double plane = (face % 2 == 0 ? bodyMin : bodyMax) + (face % 2 == 0 ? -outside : outside);
            if (face < 2) {
                x = plane;
                z = along;
            } else {
                x = along;
                z = plane;
            }
        }
        level.addParticle(
                ParticleTypes.END_ROD,
                pos.getX() + x,
                pos.getY() + y,
                pos.getZ() + z,
                random.nextGaussian() * 0.005,
                random.nextGaussian() * 0.005,
                random.nextGaussian() * 0.005
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewingStackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.BREWING_STACK.get(), BrewingStackBlockEntity::serverTick);
    }
}
