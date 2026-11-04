package com.buffspotions.mixin;

import com.buffspotions.item.AnchoredEnderFlagonItem;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleSetCreativeModeSlot", at = @At("HEAD"))
    private void buffsPotions$anchorCreativeFlagon(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo callback) {
        AnchoredEnderFlagonItem.bind(packet.itemStack(), this.player);
    }
}
