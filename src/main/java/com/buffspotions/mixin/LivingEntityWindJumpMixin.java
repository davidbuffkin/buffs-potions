package com.buffspotions.mixin;

import com.buffspotions.effect.AirStep;
import com.buffspotions.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class LivingEntityWindJumpMixin {
    @Redirect(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;onGround()Z",
                    ordinal = 3
            )
    )
    private boolean buffsPotions$windJump(LivingEntity entity) {
        return AirStep.allow(entity);
    }

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void buffsPotions$resetAirJumps(CallbackInfo callback) {
        AirStep.tick((LivingEntity) (Object) this);
    }

    @Inject(method = "jumpFromGround", at = @At("HEAD"))
    private void buffsPotions$countAirJump(CallbackInfo callback) {
        AirStep.onJump((LivingEntity) (Object) this);
    }

    /**
     * A hit while air-stepping was using the grounded knockback formula once downward
     * speed started, which dumped the hop straight at the floor.
     */
    @Inject(method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V", at = @At("RETURN"))
    private void buffsPotions$keepAirHop(CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (entity.onGround() || !entity.hasEffect(ModEffects.WIND_JUMPING)) {
            return;
        }
        Vec3 movement = entity.getDeltaMovement();
        if (movement.y < -0.1) {
            entity.setDeltaMovement(movement.x, -0.05, movement.z);
        }
    }
}
