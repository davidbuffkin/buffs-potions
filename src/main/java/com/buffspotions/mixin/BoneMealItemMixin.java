package com.buffspotions.mixin;

import com.buffspotions.effect.GreenThumb;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
abstract class BoneMealItemMixin {
    private static final ThreadLocal<BlockPos[]> FRUIT = new ThreadLocal<>();

    @Inject(method = "applyBonemeal", at = @At("HEAD"))
    private static void buffsPotions$beforeBonemeal(
            ItemStack stack,
            Level level,
            BlockPos pos,
            @Nullable Player player,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (!(level instanceof ServerLevel server) || !(server.getBlockState(pos).getBlock() instanceof StemBlock)) {
            FRUIT.remove();
            return;
        }
        BlockPos[] existing = new BlockPos[4];
        int count = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            BlockState state = server.getBlockState(side);
            if (state.is(Blocks.MELON) || state.is(Blocks.PUMPKIN)) {
                existing[count++] = side.immutable();
            }
        }
        FRUIT.set(existing);
    }

    @Inject(
            method = "applyBonemeal",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/BonemealableBlock;performBonemeal(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
                    shift = At.Shift.AFTER
            )
    )
    private static void buffsPotions$afterBonemeal(
            ItemStack stack,
            Level level,
            BlockPos pos,
            @Nullable Player player,
            CallbackInfoReturnable<Boolean> callback
    ) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        GreenThumb.noteBonemeal(server, pos, FRUIT.get());
    }

    @Inject(method = "applyBonemeal", at = @At("RETURN"))
    private static void buffsPotions$clearFruit(
            ItemStack stack,
            Level level,
            BlockPos pos,
            @Nullable Player player,
            CallbackInfoReturnable<Boolean> callback
    ) {
        FRUIT.remove();
    }
}
