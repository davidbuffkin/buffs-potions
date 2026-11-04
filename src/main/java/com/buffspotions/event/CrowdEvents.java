package com.buffspotions.event;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.CrowdTuning;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class CrowdEvents {
    @SubscribeEvent
    public static void onEntitySound(PlayLevelSoundEvent.AtEntity event) {
        scale(event, event.getEntity());
    }

    @SubscribeEvent
    public static void onPositionSound(PlayLevelSoundEvent.AtPosition event) {
        // Packet playback calls playSeededSound directly and does not set a source, so it is not scaled twice.
        // A source is set for sounds the player themselves plays and for blocks they use.
        scale(event, CrowdTuning.soundSource(), true);
    }

    private static void scale(PlayLevelSoundEvent event, Entity source) {
        scale(event, source, false);
    }

    private static void scale(PlayLevelSoundEvent event, Entity source, boolean allowClient) {
        if ((event.getLevel().isClientSide() && !allowClient) || !(source instanceof Player player)) {
            return;
        }
        float scale = CrowdTuning.volumeScale(player);
        if (scale != 1.0F) {
            event.setNewVolume(event.getNewVolume() * scale);
        }
    }

    @SubscribeEvent
    public static void onGameEvent(VanillaGameEvent event) {
        if (event.getCause() instanceof ServerPlayer player
                && CrowdTuning.ignoresCommotion(player, player.getRandom())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !CrowdTuning.riots(player)
                || player.tickCount % 10 != 0) {
            return;
        }
        AABB area = player.getBoundingBox().inflate(CrowdTuning.FLEE_DISTANCE, 4.0, CrowdTuning.FLEE_DISTANCE);
        for (PathfinderMob mob : player.level().getEntitiesOfClass(PathfinderMob.class, area, CrowdTuning::shouldFlee)) {
            Vec3 away = mob.position().subtract(player.position());
            if (away.lengthSqr() < 1.0E-4) {
                away = new Vec3(1.0, 0.0, 0.0);
            }
            Vec3 destination = away.normalize().scale(CrowdTuning.FLEE_DISTANCE).add(mob.position());
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, 1.35);
        }
    }

    private CrowdEvents() {
    }
}
