package com.buffspotions.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Rotations;
import net.minecraft.util.Mth;

/** Copies an armor stand's limb rotations onto a player model. */
public final class StandPose {
    private StandPose() {
    }

    public static void apply(
            HumanoidModel<?> model,
            Rotations head,
            Rotations body,
            Rotations leftArm,
            Rotations rightArm,
            Rotations leftLeg,
            Rotations rightLeg
    ) {
        set(model.head, head);
        set(model.body, body);
        set(model.leftArm, leftArm);
        set(model.rightArm, rightArm);
        set(model.leftLeg, leftLeg);
        set(model.rightLeg, rightLeg);
    }

    /**
     * The player idle sway, with the constant outward bias removed so it swings
     * equally to either side of whatever rotation the arms already have.
     */
    public static void bobArms(HumanoidModel<?> model, float ageInTicks) {
        bob(model.rightArm, ageInTicks, 1.0F);
        bob(model.leftArm, ageInTicks, -1.0F);
    }

    private static void set(ModelPart part, Rotations rotations) {
        part.xRot = (float) Math.toRadians(rotations.x());
        part.yRot = (float) Math.toRadians(rotations.y());
        part.zRot = (float) Math.toRadians(rotations.z());
    }

    private static void bob(ModelPart part, float ageInTicks, float scale) {
        part.zRot += scale * Mth.cos(ageInTicks * 0.09F) * 0.05F;
        part.xRot += scale * Mth.sin(ageInTicks * 0.067F) * 0.05F;
    }
}
