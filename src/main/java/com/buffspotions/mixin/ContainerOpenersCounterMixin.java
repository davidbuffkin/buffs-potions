package com.buffspotions.mixin;

import com.buffspotions.effect.CrowdTuning;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ContainerOpenersCounter.class)
abstract class ContainerOpenersCounterMixin {
    @Inject(method = "incrementOpeners", at = @At("HEAD"))
    private void buffsPotions$beginOpen(
            LivingEntity entity, Level level, BlockPos pos, BlockState state, double range, CallbackInfo callback
    ) {
        begin(entity);
    }

    @Inject(method = "incrementOpeners", at = @At("RETURN"))
    private void buffsPotions$endOpen(
            LivingEntity entity, Level level, BlockPos pos, BlockState state, double range, CallbackInfo callback
    ) {
        end(entity);
    }

    @Inject(method = "decrementOpeners", at = @At("HEAD"))
    private void buffsPotions$beginClose(LivingEntity entity, Level level, BlockPos pos, BlockState state, CallbackInfo callback) {
        begin(entity);
    }

    @Inject(method = "decrementOpeners", at = @At("RETURN"))
    private void buffsPotions$endClose(LivingEntity entity, Level level, BlockPos pos, BlockState state, CallbackInfo callback) {
        end(entity);
    }

    private static void begin(LivingEntity entity) {
        if (entity instanceof Player player) {
            CrowdTuning.beginPlayerSound(player);
        }
    }

    private static void end(LivingEntity entity) {
        if (entity instanceof Player player) {
            CrowdTuning.endPlayerSound(player);
        }
    }
}
