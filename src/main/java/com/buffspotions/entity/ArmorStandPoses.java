package com.buffspotions.entity;

import com.buffspotions.BuffsPotions;
import com.buffspotions.registry.ModAttachments;
import net.minecraft.core.Rotations;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Bedrock's thirteen armor-stand poses, written as the limb rotations Java already syncs.
 * Sneak-use cycles them. A change in redstone strength selects one.
 */
@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class ArmorStandPoses {
    private static final ArmorStand.ArmorStandPose[] POSES = {
            pose(0, 0, 0, 0, 0, 0, -10, 0, -10, -15, 0, 10, -1, 0, -1, 1, 0, 1),
            pose(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            pose(15, 0, 0, 0, 0, 2, -30, 15, 15, -60, -20, -10, -1, 0, -1, 1, 0, 1),
            pose(-5, 0, 0, 0, 0, 2, 10, 0, -5, -60, 20, -10, -3, -3, -3, 3, 3, 3),
            pose(-15, 0, 0, 0, 0, -2, 20, 0, -10, -110, 50, 0, 5, -3, -3, -5, 3, 3),
            pose(-15, 0, 0, 0, 0, 0, -110, 35, 0, -110, -35, 0, 5, -3, -3, -5, 3, 3),
            pose(-15, 0, 0, 0, 0, 0, -110, -35, 0, -110, 35, 0, 5, -3, -3, -5, 3, 3),
            pose(0, 0, 0, 0, 0, 0, 10, 0, -5, -70, -40, 0, -1, 0, -1, 1, 0, 1),
            pose(16, 20, 0, 0, 0, 0, 4, 8, 237, 246, 0, 89, -14, -18, -16, 8, 20, 4),
            pose(-10, 0, -5, 0, 0, 0, -105, 0, 0, -100, 0, 0, 7, 0, 0, -46, 0, 0),
            pose(-5, 18, 0, 0, 22, 0, 8, 0, -114, 0, 84, 111, -111, 55, 0, 0, 23, -13),
            pose(-10, -20, 0, 0, -18, 0, 0, 0, -112, 8, 90, 111, 0, 0, 13, -119, -42, 0),
            pose(-4, 67, 0, 0, 8, 0, 16, 32, -8, -99, 63, 0, 0, -75, -8, 4, 63, 8)
    };

    private ArmorStandPoses() {
    }

    public static int count() {
        return POSES.length;
    }

    /** Strength 0 and 1 are the default pose. 2 is the next pose, and 13 or more is the last. */
    public static ArmorStand.ArmorStandPose forSignal(int signal) {
        if (signal <= 1) {
            return POSES[0];
        }
        return POSES[Math.min(signal - 1, POSES.length - 1)];
    }

    public static ArmorStand.ArmorStandPose next(ArmorStand.ArmorStandPose current) {
        return POSES[Math.floorMod(indexOf(current) + 1, POSES.length)];
    }

    public static int indexOf(ArmorStand.ArmorStandPose pose) {
        for (int index = 0; index < POSES.length; index++) {
            if (same(POSES[index], pose)) {
                return index;
            }
        }
        return -1;
    }

    public static void cycle(ArmorStand stand) {
        stand.setArmorStandPose(next(stand.getArmorStandPose()));
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ArmorStand stand) || stand.level().isClientSide()) {
            return;
        }
        int signal = stand.level().getBestNeighborSignal(stand.blockPosition());
        int last = stand.getData(ModAttachments.ARMOR_POSE_SIGNAL.get());
        if (last < 0) {
            stand.setData(ModAttachments.ARMOR_POSE_SIGNAL.get(), signal);
            return;
        }
        if (signal == last) {
            return;
        }
        stand.setData(ModAttachments.ARMOR_POSE_SIGNAL.get(), signal);
        stand.setArmorStandPose(forSignal(signal));
    }

    private static boolean same(ArmorStand.ArmorStandPose left, ArmorStand.ArmorStandPose right) {
        return same(left.head(), right.head())
                && same(left.body(), right.body())
                && same(left.leftArm(), right.leftArm())
                && same(left.rightArm(), right.rightArm())
                && same(left.leftLeg(), right.leftLeg())
                && same(left.rightLeg(), right.rightLeg());
    }

    private static boolean same(Rotations left, Rotations right) {
        return left.x() == right.x() && left.y() == right.y() && left.z() == right.z();
    }

    private static ArmorStand.ArmorStandPose pose(
            float headX, float headY, float headZ,
            float bodyX, float bodyY, float bodyZ,
            float leftArmX, float leftArmY, float leftArmZ,
            float rightArmX, float rightArmY, float rightArmZ,
            float leftLegX, float leftLegY, float leftLegZ,
            float rightLegX, float rightLegY, float rightLegZ
    ) {
        return new ArmorStand.ArmorStandPose(
                new Rotations(headX, headY, headZ),
                new Rotations(bodyX, bodyY, bodyZ),
                new Rotations(leftArmX, leftArmY, leftArmZ),
                new Rotations(rightArmX, rightArmY, rightArmZ),
                new Rotations(leftLegX, leftLegY, leftLegZ),
                new Rotations(rightLegX, rightLegY, rightLegZ)
        );
    }
}
