package com.buffspotions.mixin;

import com.buffspotions.bellows.BellowsBoosts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlockEntity.class)
abstract class BrewingStandBlockEntityMixin {
    private static final ThreadLocal<Boolean> EXTRA_STEP = ThreadLocal.withInitial(() -> false);

    @Unique
    private boolean buffsPotions$wasBrewing;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void buffsPotions$rememberBrewing(
            Level level,
            BlockPos pos,
            BlockState state,
            BrewingStandBlockEntity entity,
            CallbackInfo callback
    ) {
        if (EXTRA_STEP.get()) {
            return;
        }
        ((BrewingStandBlockEntityMixin) (Object) entity).buffsPotions$wasBrewing =
                ((BrewingStandBellowsAccess) entity).buffsPotions$brewTime() > 0;
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void buffsPotions$bellows(
            Level level,
            BlockPos pos,
            BlockState state,
            BrewingStandBlockEntity entity,
            CallbackInfo callback
    ) {
        if (!EXTRA_STEP.get() && BellowsBoosts.isActive(level, pos)) {
            EXTRA_STEP.set(true);
            try {
                if (level instanceof ServerLevel server && server.getGameTime() % 10 == 0) {
                    BellowsBoosts.trickle(server, pos);
                }
                BrewingStandBlockEntity.serverTick(level, pos, level.getBlockState(pos), entity);
            } finally {
                EXTRA_STEP.set(false);
            }
        }
        if (EXTRA_STEP.get()) {
            return;
        }
        boolean brewing = ((BrewingStandBellowsAccess) entity).buffsPotions$brewTime() > 0;
        if (brewing == ((BrewingStandBlockEntityMixin) (Object) entity).buffsPotions$wasBrewing
                || !(level instanceof ServerLevel server)) {
            return;
        }
        server.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }
}
