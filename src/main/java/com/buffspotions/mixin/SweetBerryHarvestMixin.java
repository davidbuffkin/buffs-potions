package com.buffspotions.mixin;

import com.buffspotions.effect.GreenThumb;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SweetBerryBushBlock.class)
abstract class SweetBerryHarvestMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"))
    private void buffsPotions$sweetBerries(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult,
            CallbackInfoReturnable<?> callback
    ) {
        if (player instanceof ServerPlayer server
                && level instanceof ServerLevel serverLevel
                && state.getValue(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE) {
            GreenThumb.onRightClickHarvest(server, serverLevel, pos, state);
        }
    }
}
