package com.buffspotions.mixin;

import com.buffspotions.bellows.BellowsBoosts;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
abstract class AbstractFurnaceBlockEntityMixin {
    private static final ThreadLocal<Boolean> EXTRA_STEP = ThreadLocal.withInitial(() -> false);

    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void buffsPotions$bellows(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            AbstractFurnaceBlockEntity entity,
            CallbackInfo callback
    ) {
        if (EXTRA_STEP.get() || !BellowsBoosts.isActive(level, pos)) {
            return;
        }
        EXTRA_STEP.set(true);
        try {
            if (level.getGameTime() % 10 == 0) {
                BellowsBoosts.trickle(level, pos);
            }
            AbstractFurnaceBlockEntity.serverTick(level, pos, level.getBlockState(pos), entity);
        } finally {
            EXTRA_STEP.set(false);
        }
    }
}
