package com.buffspotions.mixin;

import com.buffspotions.effect.CrowdTuning;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameEventDispatcher.class)
abstract class GameEventDispatcherMixin {
    @Unique
    private static final ThreadLocal<GameEvent.Context> buffsPotions$context = new ThreadLocal<>();

    @Inject(method = "post", at = @At("HEAD"))
    private void buffsPotions$capture(net.minecraft.core.Holder<GameEvent> event, net.minecraft.world.phys.Vec3 position, GameEvent.Context context, CallbackInfo callback) {
        buffsPotions$context.set(context);
    }

    @Inject(method = "post", at = @At("RETURN"))
    private void buffsPotions$clear(net.minecraft.core.Holder<GameEvent> event, net.minecraft.world.phys.Vec3 position, GameEvent.Context context, CallbackInfo callback) {
        buffsPotions$context.remove();
    }

    @Redirect(
            method = "post",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/gameevent/GameEvent;notificationRadius()I"
            )
    )
    private int buffsPotions$riotRadius(GameEvent event) {
        int radius = event.notificationRadius();
        GameEvent.Context context = buffsPotions$context.get();
        if (context != null && context.sourceEntity() instanceof ServerPlayer player) {
            return CrowdTuning.sensorRadius(player, radius);
        }
        return radius;
    }
}
