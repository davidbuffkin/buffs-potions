package com.buffspotions.still;

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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class BrewingStillBlock extends BaseEntityBlock {
    public static final MapCodec<BrewingStillBlock> CODEC = simpleCodec(BrewingStillBlock::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty HAS_BOTTLE = BooleanProperty.create("has_bottle");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SOUTH_SHAPE = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0),
            Block.box(5.0, 0.0, 9.0, 11.0, 2.0, 15.0),
            Block.box(6.0, 12.0, 10.0, 10.0, 16.0, 14.0),
            Block.box(7.0, 13.0, 8.0, 9.0, 15.0, 10.0),
            Block.box(7.0, 6.0, 11.0, 9.0, 13.0, 13.0)
    );
    private static final VoxelShape[] SHAPES = facingShapes(SOUTH_SHAPE);

    public BrewingStillBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false)
                .setValue(HAS_BOTTLE, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public MapCodec<BrewingStillBlock> codec() {
        return CODEC;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BrewingStillBlockEntity still) {
            player.openMenu(still);
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
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double spread = random.nextDouble() * 0.6 - 0.3;
        double dy = random.nextDouble() * 6.0 / 16.0;
        double[] front = rotateFromSouth(0.5 + spread, 0.55, state.getValue(FACING));
        double x = pos.getX() + front[0];
        double y = pos.getY() + dy;
        double z = pos.getZ() + front[1];
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BrewingStillBlockEntity still) {
            still.tryFlowerBrew(level, pos, random);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, HAS_BOTTLE);
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

    private static double[] rotateFromSouth(double x, double z, Direction facing) {
        int turns = switch (facing) {
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
        for (int i = 0; i < turns; i++) {
            double nextX = 1.0 - z;
            z = x;
            x = nextX;
        }
        return new double[]{x, z};
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrewingStillBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.BREWING_STILL.get(), BrewingStillBlockEntity::serverTick);
    }
}
