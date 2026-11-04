package com.buffspotions.effect;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class WeatherCalls {
    public static final int DELAY_TICKS = 100;

    private static final List<Pending> PENDING = new ArrayList<>();

    private WeatherCalls() {
    }

    public static void scheduleRain(MinecraftServer server) {
        scheduleAt(server, true, server.getTickCount() + DELAY_TICKS);
    }

    public static void scheduleClear(MinecraftServer server) {
        scheduleAt(server, false, server.getTickCount() + DELAY_TICKS);
    }

    private static void scheduleAt(MinecraftServer server, boolean rain, int tick) {
        PENDING.add(new Pending(server, rain, tick));
    }

    public static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        Iterator<Pending> pending = PENDING.iterator();
        while (pending.hasNext()) {
            Pending call = pending.next();
            if (call.server != server || now < call.tick) {
                continue;
            }
            pending.remove();
            apply(server, call.rain);
        }
    }

    public static void forget(MinecraftServer server) {
        PENDING.removeIf(call -> call.server == server);
    }

    private static void apply(MinecraftServer server, boolean rain) {
        Level overworld = server.overworld();
        RandomSource random = overworld != null ? overworld.getRandom() : RandomSource.create();
        if (rain) {
            server.setWeatherParameters(0, ServerLevel.RAIN_DURATION.sample(random), true, false);
        } else {
            server.setWeatherParameters(ServerLevel.RAIN_DELAY.sample(random), 0, false, false);
        }
    }

    private record Pending(MinecraftServer server, boolean rain, int tick) {
    }
}
