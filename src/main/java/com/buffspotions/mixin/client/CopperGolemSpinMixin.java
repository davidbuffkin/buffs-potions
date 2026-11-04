package com.buffspotions.mixin.client;

import com.buffspotions.client.GolemHeadSpin;
import com.buffspotions.client.GolemHeadSpinMark;
import com.buffspotions.item.BellowsItem;
import com.buffspotions.registry.ModAttachments;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemOxidationLevels;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Starts the head spin when a bellows golem stokes a machine. */
@Mixin(CopperGolem.class)
abstract class CopperGolemSpinMixin implements GolemHeadSpinMark {
    @Unique
    private final AnimationState buffsPotions$headSpin = new AnimationState();
    @Unique
    private int buffsPotions$spinWarmup;
    @Unique
    private long buffsPotions$lastSpin;

    @Override
    public AnimationState buffsPotions$headSpin() {
        return this.buffsPotions$headSpin;
    }

    @Inject(method = "setupAnimationStates", at = @At("HEAD"))
    private void buffsPotions$spinOnBellows(CallbackInfo callback) {
        CopperGolem golem = (CopperGolem) (Object) this;
        if (this.buffsPotions$headSpin.isStarted()
                && this.buffsPotions$headSpin.getTimeInMillis(golem.tickCount) > (long) (GolemHeadSpin.LENGTH_SECONDS * 1000.0F)) {
            this.buffsPotions$headSpin.stop();
        }
        if (golem.getState() != CopperGolemState.IDLE || !(golem.getMainHandItem().getItem() instanceof BellowsItem)) {
            return;
        }
        long spin = golem.getData(ModAttachments.GOLEM_HEAD_SPIN.get());
        if (this.buffsPotions$spinWarmup < 5) {
            this.buffsPotions$spinWarmup++;
            this.buffsPotions$lastSpin = spin;
            return;
        }
        if (spin == this.buffsPotions$lastSpin) {
            return;
        }
        this.buffsPotions$lastSpin = spin;
        this.buffsPotions$headSpin.start(golem.tickCount);
        golem.level().playLocalSound(
                golem.getX(),
                golem.getY(),
                golem.getZ(),
                CopperGolemOxidationLevels.getOxidationLevel(golem.getWeatherState()).spinHeadSound(),
                golem.getSoundSource(),
                1.0F,
                1.0F,
                false
        );
    }
}
