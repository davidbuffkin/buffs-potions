package com.buffspotions.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The vanilla stand keeps brew time off the client. Send it with the block entity so particles can see a live brew. */
@Mixin(BlockEntity.class)
abstract class BrewingStandSyncMixin {
    @Inject(method = "getUpdateTag", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$standTag(HolderLookup.Provider registries, CallbackInfoReturnable<CompoundTag> callback) {
        if ((Object) this instanceof BrewingStandBlockEntity) {
            callback.setReturnValue(((BlockEntity) (Object) this).saveCustomOnly(registries));
        }
    }

    @Inject(method = "getUpdatePacket", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$standPacket(CallbackInfoReturnable<Packet<ClientGamePacketListener>> callback) {
        if ((Object) this instanceof BrewingStandBlockEntity stand) {
            callback.setReturnValue(ClientboundBlockEntityDataPacket.create(stand));
        }
    }
}
