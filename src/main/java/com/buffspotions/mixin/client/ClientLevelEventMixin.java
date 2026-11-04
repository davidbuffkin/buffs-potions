package com.buffspotions.mixin.client;

import com.buffspotions.effect.CrowdTuning;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
abstract class ClientLevelEventMixin {
    @Inject(method = "levelEvent", at = @At("HEAD"))
    private void buffsPotions$beginBreak(Entity source, int type, BlockPos pos, int data, CallbackInfo callback) {
        if (type == 2001 && source instanceof Player player) {
            CrowdTuning.beginPlayerSound(player);
        }
    }

    @Inject(method = "levelEvent", at = @At("RETURN"))
    private void buffsPotions$endBreak(Entity source, int type, BlockPos pos, int data, CallbackInfo callback) {
        if (type == 2001 && source instanceof Player player) {
            CrowdTuning.endPlayerSound(player);
        }
    }
}
