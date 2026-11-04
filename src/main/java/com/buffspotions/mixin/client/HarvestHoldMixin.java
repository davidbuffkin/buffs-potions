package com.buffspotions.mixin.client;

import com.buffspotions.effect.GreenThumb;
import com.buffspotions.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A fresh click can break an immature harvest crop. Holding the button only continues the crop that click started.
 */
@Mixin(MultiPlayerGameMode.class)
abstract class HarvestHoldMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private BlockPos destroyBlockPos;

    @Shadow
    private ItemStack destroyingItem;

    @Shadow
    private boolean isDestroying;

    @Shadow
    private int destroyDelay;

    @Shadow
    public abstract void stopDestroyBlock();

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$spareImmatureCrops(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> callback) {
        if (this.destroyDelay > 0) {
            return;
        }
        LocalPlayer player = this.minecraft.player;
        if (player == null || !player.hasEffect(ModEffects.GREEN_THUMB)) {
            return;
        }
        BlockState state = this.minecraft.level.getBlockState(pos);
        if (!GreenThumb.immatureSeedCrop(state)) {
            return;
        }
        boolean finishingThisClick = this.isDestroying
                && pos.equals(this.destroyBlockPos)
                && !this.destroyingItem.shouldCauseBlockBreakReset(player.getMainHandItem());
        if (player.getAbilities().instabuild || !finishingThisClick) {
            this.stopDestroyBlock();
            callback.setReturnValue(false);
        }
    }
}
