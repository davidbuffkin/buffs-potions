package com.buffspotions.mixin.client;

import com.buffspotions.client.SkinMemory;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Placed and held player heads use this world's saved skin before resolving the profile. */
@Mixin(PlayerSkinRenderCache.class)
abstract class PlayerSkinRenderCacheMixin {
    @Inject(method = "getOrDefault", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$cachedSkin(ResolvableProfile profile, CallbackInfoReturnable<PlayerSkinRenderCache.RenderInfo> callback) {
        PlayerSkin skin = SkinMemory.skin(profile);
        if (skin == null) {
            return;
        }
        callback.setReturnValue(((PlayerSkinRenderCache) (Object) this).new RenderInfo(
                profile.partialProfile(),
                skin,
                profile.skinPatch()
        ));
    }
}
