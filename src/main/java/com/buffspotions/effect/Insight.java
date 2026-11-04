package com.buffspotions.effect;

import com.buffspotions.registry.ModEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.AABB;

/** Insight pulls experience orbs from the usual follow distance, plus the Item Eater step for each level. */
public final class Insight {
    /** Vanilla experience orbs follow a player from this many blocks. */
    static final int VANILLA_FOLLOW_BLOCKS = 8;

    private Insight() {
    }

    public static void attract(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            int reach = reach(player);
            if (reach <= 0 || !(player.level() instanceof ServerLevel level)) {
                continue;
            }
            AABB area = player.getBoundingBox().inflate(reach);
            for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area, entity -> !entity.isRemoved())) {
                ItemEating.pull(player, orb, reach);
            }
        }
    }

    /**
     * True while this orb is inside an Insight pull, so the usual short follow does not add a second force.
     */
    public static boolean pullsOrb(ExperienceOrb orb) {
        if (!(orb.level() instanceof ServerLevel level)) {
            return false;
        }
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            if (ItemEating.withinPull(player, orb, reach(player))) {
                return true;
            }
        }
        return false;
    }

    /** Level I is 11 blocks, and each higher level adds the same 3-block step. */
    static int reach(int amplifier) {
        return VANILLA_FOLLOW_BLOCKS + EyedTuning.attractDistance(amplifier);
    }

    private static int reach(ServerPlayer player) {
        MobEffectInstance insight = player.getEffect(ModEffects.INSIGHTFUL);
        if (insight == null) {
            return 0;
        }
        return reach(insight.getAmplifier());
    }
}
