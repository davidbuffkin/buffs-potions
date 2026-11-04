package com.buffspotions.mixin.client;

import com.buffspotions.client.BlockSenseClient;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelChunk.class)
abstract class LevelChunkSenseMixin {
    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void buffsPotions$dirtyBlockSense(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> callback) {
        if (callback.getReturnValue() != null && ((LevelChunk) (Object) this).getLevel().isClientSide()) {
            BlockSenseClient.markChanged(pos);
        }
    }
}
