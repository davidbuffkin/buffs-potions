package com.buffspotions.mixin;

import com.buffspotions.effect.Eyedentification;
import com.buffspotions.effect.Stopper;
import com.buffspotions.potion.Remedies;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AreaEffectCloud.class)
public abstract class AreaEffectCloudMixin {
    @Shadow
    private PotionContents potionContents;

    @Inject(method = "serverTick", at = @At("HEAD"))
    private void buffsPotions$beginCloud(ServerLevel serverLevel, CallbackInfo callback) {
        Stopper.begin();
        Remedies.beginApply(this.potionContents);
    }

    @Inject(method = "serverTick", at = @At("RETURN"))
    private void buffsPotions$endCloud(ServerLevel serverLevel, CallbackInfo callback) {
        Remedies.endApply();
        Stopper.end();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDD)V", at = @At("RETURN"))
    private void buffsPotions$captureSubject(Level level, double x, double y, double z, CallbackInfo callback) {
        Eyedentification.captureCloud((AreaEffectCloud) (Object) this);
    }
}
