package com.buffspotions.entity;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EphemeralTestServerProvider.class)
class ArmorStandPoseTest {
    @Test
    void sneakCyclesAndRedstoneSelectsTheBedrockPoses(MinecraftServer server) {
        assertEquals(13, ArmorStandPoses.count());
        ArmorStand.ArmorStandPose fresh = new ArmorStand.ArmorStandPose(
                ArmorStand.DEFAULT_HEAD_POSE,
                ArmorStand.DEFAULT_BODY_POSE,
                ArmorStand.DEFAULT_LEFT_ARM_POSE,
                ArmorStand.DEFAULT_RIGHT_ARM_POSE,
                ArmorStand.DEFAULT_LEFT_LEG_POSE,
                ArmorStand.DEFAULT_RIGHT_LEG_POSE
        );
        assertEquals(0, ArmorStandPoses.indexOf(fresh));
        assertEquals(0, ArmorStandPoses.indexOf(ArmorStandPoses.forSignal(0)));
        assertEquals(0, ArmorStandPoses.indexOf(ArmorStandPoses.forSignal(1)));
        assertEquals(1, ArmorStandPoses.indexOf(ArmorStandPoses.forSignal(2)));
        assertEquals(12, ArmorStandPoses.indexOf(ArmorStandPoses.forSignal(13)));
        assertEquals(12, ArmorStandPoses.indexOf(ArmorStandPoses.forSignal(15)));

        ArmorStand.ArmorStandPose posed = fresh;
        for (int step = 0; step < ArmorStandPoses.count(); step++) {
            posed = ArmorStandPoses.next(posed);
        }
        assertEquals(0, ArmorStandPoses.indexOf(posed));
        assertEquals(1, ArmorStandPoses.indexOf(ArmorStandPoses.next(fresh)));
    }
}
