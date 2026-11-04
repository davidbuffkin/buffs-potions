package com.buffspotions.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientHandshakePacketListenerImpl;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Dev clients have no Mojang session. Direct connect would stop with "Invalid session"
 * even though a LAN join is allowed to continue. Offline dev mode follows the LAN path.
 */
@Mixin(ClientHandshakePacketListenerImpl.class)
abstract class DevSessionMixin {
    @Inject(method = "authenticateServer", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$offlineDevSession(String digest, CallbackInfoReturnable<Component> callback) {
        if (Minecraft.getInstance().isOfflineDeveloperMode()) {
            callback.setReturnValue(null);
        }
    }
}
