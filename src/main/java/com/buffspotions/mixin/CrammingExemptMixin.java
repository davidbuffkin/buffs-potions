package com.buffspotions.mixin;

import com.buffspotions.registry.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
abstract class CrammingExemptMixin {
    @Redirect(
            method = "pushEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            )
    )
    private boolean buffsPotions$skipFlockCramming(LivingEntity self, ServerLevel level, DamageSource source, float amount) {
        if (self.hasData(ModAttachments.NO_CRAMMING)) {
            return false;
        }
        return self.hurtServer(level, source, amount);
    }
}
