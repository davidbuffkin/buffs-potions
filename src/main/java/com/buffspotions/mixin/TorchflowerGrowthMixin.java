package com.buffspotions.mixin;

import com.buffspotions.effect.GreenThumb;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CropBlock.class)
abstract class TorchflowerGrowthMixin {
    @Inject(method = "growCrops", at = @At("RETURN"))
    private void buffsPotions$bloomedTorchflower(Level level, BlockPos pos, BlockState state, CallbackInfo callback) {
        if (level instanceof ServerLevel server) {
            GreenThumb.noteTorchflower(server, state, server.getBlockState(pos), pos);
        }
    }
}
