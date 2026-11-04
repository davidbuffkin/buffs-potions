package com.buffspotions.client;

import net.minecraft.core.Rotations;
import org.jspecify.annotations.Nullable;

/** Disguise subject copied onto a render state for this frame. Empty when the entity is drawn as itself. */
public interface DisguiseStandMark {
    String buffsPotions$disguiseName();

    void buffsPotions$setDisguiseName(String name);

    boolean buffsPotions$stillArms();

    void buffsPotions$setStillArms(boolean still);

    /** Head, body, left arm, right arm, left leg, right leg. Null unless this frame is an armor stand. */
    Rotations @Nullable [] buffsPotions$standPose();

    void buffsPotions$setStandPose(Rotations @Nullable [] pose);
}
