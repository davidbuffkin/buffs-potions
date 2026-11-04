package com.buffspotions.mixin;

import com.buffspotions.advancement.TipJar;
import com.buffspotions.belt.BeltTipping;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractArrow.class)
abstract class AbstractArrowSplashMixin {
    @Inject(method = "onHitEntity", at = @At("RETURN"))
    private void buffsPotions$splashEntity(EntityHitResult hit, CallbackInfo callback) {
        BeltTipping.land((AbstractArrow) (Object) this, hit);
    }

    @Inject(method = "onHitBlock", at = @At("RETURN"))
    private void buffsPotions$splashBlock(BlockHitResult hit, CallbackInfo callback) {
        BeltTipping.land((AbstractArrow) (Object) this, hit);
    }

    @Inject(method = "doPostHurtEffects", at = @At("HEAD"))
    private void buffsPotions$tippedHit(LivingEntity target, CallbackInfo callback) {
        TipJar.note((AbstractArrow) (Object) this, target);
    }
}
