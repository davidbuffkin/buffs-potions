package com.buffspotions.mixin.client;

import com.buffspotions.client.GolemHeadSpinMark;
import net.minecraft.client.renderer.entity.state.CopperGolemRenderState;
import net.minecraft.world.entity.AnimationState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CopperGolemRenderState.class)
public class CopperGolemSpinStateMixin implements GolemHeadSpinMark {
    @Unique
    private final AnimationState buffsPotions$headSpin = new AnimationState();

    @Override
    public AnimationState buffsPotions$headSpin() {
        return this.buffsPotions$headSpin;
    }
}
