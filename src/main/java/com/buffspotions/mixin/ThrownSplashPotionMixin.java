package com.buffspotions.mixin;

import com.buffspotions.advancement.HalloweenSplash;
import com.buffspotions.effect.DisguiseArmorStands;
import com.buffspotions.effect.Eyedentification;
import com.buffspotions.effect.Formation;
import com.buffspotions.effect.Incarnation;
import com.buffspotions.effect.Stopper;
import com.buffspotions.potion.Remedies;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownSplashPotion.class)
public abstract class ThrownSplashPotionMixin {
    @Inject(method = "onHitAsPotion", at = @At("HEAD"))
    private void buffsPotions$beginSplash(ServerLevel level, ItemStack potionItem, HitResult hitResult, CallbackInfo callback) {
        HalloweenSplash.begin((ThrownSplashPotion) (Object) this, potionItem);
        Stopper.begin();
        Remedies.beginApply(potionItem);
        Eyedentification.beginApply(potionItem);
        if (Incarnation.isIncarnation(potionItem)) {
            Incarnation.suppress();
            Vec3 position = hitResult.getLocation();
            if (hitResult instanceof BlockHitResult blockHit) {
                Direction face = blockHit.getDirection();
                position = Vec3.atBottomCenterOf(blockHit.getBlockPos().relative(face));
            }
            Incarnation.spawn(level, Eyedentification.applying(), position, ((ThrownSplashPotion) (Object) this).getYRot());
        }
        if (Formation.isFormation(potionItem)) {
            Formation.suppress();
            ThrownSplashPotion potion = (ThrownSplashPotion) (Object) this;
            Formation.splash(level, hitResult, potion.getOwner());
        }
        DisguiseArmorStands.splash(level, (ThrownSplashPotion) (Object) this, potionItem, hitResult);
    }

    @Inject(method = "onHitAsPotion", at = @At("RETURN"))
    private void buffsPotions$endSplash(ServerLevel level, ItemStack potionItem, HitResult hitResult, CallbackInfo callback) {
        Eyedentification.endApply();
        Remedies.endApply();
        Stopper.end();
        HalloweenSplash.end();
    }
}
