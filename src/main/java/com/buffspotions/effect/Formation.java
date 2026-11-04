package com.buffspotions.effect;

import com.buffspotions.registry.ModEffects;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** An instant potion that forms its block in air, water, or lava. */
public final class Formation {
    public static final int COLOR = 0xD1935C;
    private static final ThreadLocal<BlockPos> FLAGON = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SUPPRESS = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> LIT = ThreadLocal.withInitial(() -> false);

    private Formation() {
    }

    public static boolean isFormation(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (var effect : contents.getAllEffects()) {
            if (effect.getEffect().unwrapKey().equals(ModEffects.INSTANT_FORMATION.unwrapKey())) {
                return true;
            }
        }
        return false;
    }

    public static boolean canReplace(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.LAVA);
    }

    /**
     * A player drinking a bottle tries the block under their feet when the space at their
     * feet is air and that block is air, water, or lava. Otherwise the block forms at their feet.
     */
    public static BlockPos playerDrinkPos(BlockPos feet, BlockState atFeet, BlockState below) {
        if (atFeet.isAir() && canReplace(below)) {
            return feet.below();
        }
        return feet;
    }

    /** The open block nearest the impact, at most one block from that spot. */
    public static @Nullable BlockPos nearestOpen(Vec3 hit, BlockPos hitBlock, Direction face, Predicate<BlockPos> open) {
        BlockPos preferred = hitBlock.relative(face);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        BlockPos origin = BlockPos.containing(hit);
        for (BlockPos cursor : BlockPos.betweenClosed(origin.offset(-1, -1, -1), origin.offset(1, 1, 1))) {
            BlockPos pos = cursor.immutable();
            if (!open.test(pos)) {
                continue;
            }
            double distance = hit.distanceTo(Vec3.atCenterOf(pos));
            if (distance > 1.0) {
                continue;
            }
            boolean closer = best == null || distance < bestDistance - 1.0E-6;
            boolean tie = best != null && Math.abs(distance - bestDistance) <= 1.0E-6 && pos.equals(preferred);
            if (closer || tie) {
                best = pos;
                bestDistance = distance;
            }
        }
        if (best == null && open.test(preferred) && hit.distanceTo(Vec3.atCenterOf(preferred)) <= 1.0) {
            return preferred;
        }
        return best;
    }

    public static void atFlagon(BlockPos above) {
        FLAGON.set(above);
    }

    public static void clearFlagon() {
        FLAGON.remove();
    }

    public static void suppress() {
        SUPPRESS.set(true);
    }

    public static void light() {
        LIT.set(true);
    }

    public static void clear() {
        SUPPRESS.set(false);
        LIT.set(false);
    }

    public static void drink(ServerLevel level, @Nullable LivingEntity mob) {
        if (SUPPRESS.get()) {
            return;
        }
        Block block = block();
        if (block == null) {
            return;
        }
        BlockPos pos = FLAGON.get();
        if (pos == null && mob != null) {
            BlockPos feet = mob.blockPosition();
            pos = mob instanceof Player
                    ? playerDrinkPos(feet, level.getBlockState(feet), level.getBlockState(feet.below()))
                    : feet;
        }
        if (pos == null) {
            return;
        }
        place(level, block, pos, false, mob);
    }

    public static void splash(ServerLevel level, HitResult hit, @Nullable Entity owner) {
        Block block = block();
        if (block == null || hit == null) {
            return;
        }
        LivingEntity igniter = owner instanceof LivingEntity living ? living : null;
        BlockPos pos;
        if (hit instanceof EntityHitResult entityHit) {
            pos = entityHit.getEntity().blockPosition();
        } else if (hit instanceof BlockHitResult blockHit) {
            pos = nearestOpen(
                    hit.getLocation(),
                    blockHit.getBlockPos(),
                    blockHit.getDirection(),
                    candidate -> canReplace(level.getBlockState(candidate))
            );
        } else {
            return;
        }
        if (pos == null) {
            return;
        }
        place(level, block, pos, LIT.get(), igniter);
    }

    private static void place(ServerLevel level, Block block, BlockPos pos, boolean lit, @Nullable LivingEntity igniter) {
        if (!canReplace(level.getBlockState(pos))) {
            return;
        }
        if (lit && block == Blocks.TNT) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.addFreshEntity(new PrimedTnt(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, igniter));
            return;
        }
        level.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
    }

    private static @Nullable Block block() {
        EssenceSubject subject = Eyedentification.applying();
        if (subject == null) {
            return null;
        }
        return subject.block().map(holder -> holder.value()).orElse(null);
    }
}
