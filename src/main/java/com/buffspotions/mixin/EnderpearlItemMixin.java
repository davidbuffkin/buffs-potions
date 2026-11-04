package com.buffspotions.mixin;

import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderpearlItem.class)
abstract class EnderpearlItemMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void buffsPotions$perfectTiming(
            Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback
    ) {
        if (level.isClientSide() || !(player instanceof ServerPlayer server)) {
            return;
        }
        MobEffectInstance dislocated = server.getEffect(ModEffects.DISLOCATED);
        if (dislocated != null && dislocated.getDuration() > 0 && dislocated.getDuration() < 60) {
            ModCriteria.PERFECT_TIMING.get().trigger(server);
        }
    }
}
