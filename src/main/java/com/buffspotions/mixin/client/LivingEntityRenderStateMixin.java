package com.buffspotions.mixin.client;

import com.buffspotions.client.DisguiseStandMark;
import com.buffspotions.client.ShadowMark;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Rotations;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements ShadowMark, DisguiseStandMark {
    @Unique
    private boolean buffsPotions$shadow;
    @Unique
    private String buffsPotions$disguiseName = "";
    @Unique
    private boolean buffsPotions$stillArms;
    @Unique
    private Rotations @Nullable [] buffsPotions$standPose;

    @Override
    public boolean buffsPotions$isShadow() {
        return this.buffsPotions$shadow;
    }

    @Override
    public void buffsPotions$setShadow(boolean shadow) {
        this.buffsPotions$shadow = shadow;
    }

    @Override
    public String buffsPotions$disguiseName() {
        return this.buffsPotions$disguiseName;
    }

    @Override
    public void buffsPotions$setDisguiseName(String name) {
        this.buffsPotions$disguiseName = name == null ? "" : name;
    }

    @Override
    public boolean buffsPotions$stillArms() {
        return this.buffsPotions$stillArms;
    }

    @Override
    public void buffsPotions$setStillArms(boolean still) {
        this.buffsPotions$stillArms = still;
    }

    @Override
    public Rotations @Nullable [] buffsPotions$standPose() {
        return this.buffsPotions$standPose;
    }

    @Override
    public void buffsPotions$setStandPose(Rotations @Nullable [] pose) {
        this.buffsPotions$standPose = pose;
    }
}
