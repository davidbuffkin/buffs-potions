package com.buffspotions.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Moves a living entity up or down the column it is standing in. */
public final class ColumnShift {
    private ColumnShift() {
    }

    public static void ascend(ServerLevel level, LivingEntity entity, int amplification) {
        shift(level, entity, amplification, true);
    }

    public static void descend(ServerLevel level, LivingEntity entity, int amplification) {
        shift(level, entity, amplification, false);
    }

    private static void shift(ServerLevel level, LivingEntity entity, int amplification, boolean ascent) {
        int headroom = headroom(entity);
        int x = Mth.floor(entity.getX());
        int z = Mth.floor(entity.getZ());
        int feet = Mth.floor(entity.getY());
        List<Integer> floors = floors(level, x, z, headroom);
        // Heights starts at the waist. On soul sand the feet sit inside that short block, so a feet search treats it as the next floor.
        int from = ascent ? ascentStart(entity.getY(), entity.getBbHeight()) : feet;
        Integer floor = ascent ? pickAbove(floors, from, amplification) : pickBelow(floors, feet, amplification);
        if (floor == null) {
            return;
        }
        int destination = floor + 1;
        if (cryingObsidianBetween(level, x, z, feet, destination)) {
            return;
        }
        double y = floor + 1.0;
        if (!move(level, entity, y)) {
            return;
        }
        entity.resetFallDistance();
        level.playSound(null, entity.getX(), y, entity.getZ(), SoundEvents.LADDER_STEP, entity.getSoundSource(), 1.0F, 1.0F);
    }

    /** The block containing the entity's waist. */
    static int ascentStart(double y, float height) {
        return Mth.floor(y + height * 0.5F);
    }

    /** Standing height, so a crawl or a swim does not count as room to stand up into. */
    private static int headroom(Entity entity) {
        float height = entity.getDimensions(Pose.STANDING).height();
        return Math.max(1, Mth.ceil(height));
    }

    private static boolean move(ServerLevel level, LivingEntity entity, double y) {
        if (entity instanceof ServerPlayer player) {
            return player.teleportTo(level, player.getX(), y, player.getZ(), Set.of(), player.getYRot(), player.getXRot(), false);
        }
        entity.setDeltaMovement(Vec3.ZERO);
        entity.teleportTo(entity.getX(), y, entity.getZ());
        return true;
    }

    /** Level I is the next spot. Each further level skips one more. Past the end, use the furthest. */
    private static Integer pickAbove(List<Integer> floors, int feet, int amplification) {
        List<Integer> above = new ArrayList<>();
        for (int floor : floors) {
            if (floor >= feet) {
                above.add(floor);
            }
        }
        if (!above.isEmpty()) {
            return above.get(Math.min(Math.max(amplification, 0), above.size() - 1));
        }
        return floors.isEmpty() ? null : floors.get(floors.size() - 1);
    }

    private static Integer pickBelow(List<Integer> floors, int feet, int amplification) {
        List<Integer> below = new ArrayList<>();
        for (int index = floors.size() - 1; index >= 0; index--) {
            int floor = floors.get(index);
            // Two blocks under the block the player is in. A jump lifts that block into the old cutoff.
            if (floor < feet - 2) {
                below.add(floor);
            }
        }
        if (!below.isEmpty()) {
            return below.get(Math.min(Math.max(amplification, 0), below.size() - 1));
        }
        return null;
    }

    /** Crying obsidian anywhere strictly between the two feet blocks stops the shift. */
    private static boolean cryingObsidianBetween(ServerLevel level, int x, int z, int from, int to) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        return cryingObsidianBetween(from, to, y -> level.getBlockState(cursor.set(x, y, z)).is(Blocks.CRYING_OBSIDIAN));
    }

    static boolean cryingObsidianBetween(int from, int to, IntPredicate cryingObsidian) {
        int low = Math.min(from, to);
        int high = Math.max(from, to);
        for (int y = low + 1; y < high; y++) {
            if (cryingObsidian.test(y)) {
                return true;
            }
        }
        return false;
    }

    /** Floors from the bottom of the world upward. */
    private static List<Integer> floors(ServerLevel level, int x, int z, int headroom) {
        List<Integer> floors = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int top = level.getMaxY() - 1 - headroom;
        for (int y = level.getMinY(); y <= top; y++) {
            if (landing(level, cursor.set(x, y, z), headroom)) {
                floors.add(y);
            }
        }
        return floors;
    }

    /** A solid block or water surface with enough open blocks above it for the entity to stand. */
    private static boolean landing(ServerLevel level, BlockPos pos, int headroom) {
        BlockState floor = level.getBlockState(pos);
        for (int above = 1; above <= headroom; above++) {
            if (!open(level, pos.above(above))) {
                return false;
            }
        }
        return standable(level, pos, floor) || waterSurface(level, pos, floor);
    }

    private static boolean standable(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.getFluidState().is(FluidTags.LAVA)) {
            return false;
        }
        return !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean waterSurface(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getFluidState().is(FluidTags.WATER) && state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean open(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return state.getCollisionShape(level, pos).isEmpty();
    }
}
