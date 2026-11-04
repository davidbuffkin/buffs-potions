package com.buffspotions.mixin;

import com.buffspotions.effect.Eyedentification;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownLingeringPotion.class)
public abstract class ThrownLingeringPotionMixin {
    @Inject(method = "onHitAsPotion", at = @At("HEAD"))
    private void buffsPotions$pendingCloud(ServerLevel level, ItemStack potionItem, HitResult hitResult, CallbackInfo callback) {
        Eyedentification.pendingCloud(potionItem);
    }

    @Inject(method = "onHitAsPotion", at = @At("RETURN"))
    private void buffsPotions$clearCloud(ServerLevel level, ItemStack potionItem, HitResult hitResult, CallbackInfo callback) {
        Eyedentification.clearPendingCloud();
    }
}
