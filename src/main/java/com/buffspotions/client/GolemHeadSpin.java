package com.buffspotions.client;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.Keyframe;
import net.minecraft.client.animation.KeyframeAnimations;

/** A flat full turn of the copper golem's head, ending where it began. */
public final class GolemHeadSpin {
    public static final float LENGTH_SECONDS = 0.5F;

    public static final AnimationDefinition ANIMATION = AnimationDefinition.Builder.withLength(LENGTH_SECONDS)
            .addAnimation(
                    "head",
                    new AnimationChannel(
                            AnimationChannel.Targets.ROTATION,
                            new Keyframe(0.0F, KeyframeAnimations.degreeVec(0.0F, 0.0F, 0.0F), AnimationChannel.Interpolations.LINEAR),
                            new Keyframe(LENGTH_SECONDS, KeyframeAnimations.degreeVec(0.0F, 360.0F, 0.0F), AnimationChannel.Interpolations.LINEAR)
                    )
            )
            .build();

    private GolemHeadSpin() {
    }
}
