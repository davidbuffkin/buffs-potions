package com.buffspotions.advancement;

import com.buffspotions.registry.ModCriteria;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Five stacks of blocks, 320 in all, placed within five seconds. */
public final class StacksOnStacks {
    private static final int WINDOW_TICKS = 5 * 20;
    private static final int BLOCKS = 64 * 5;
    private static final Map<UUID, List<Placement>> PLACED = new HashMap<>();

    private StacksOnStacks() {
    }

    public static void note(ServerPlayer player, int count) {
        if (count <= 0) {
            return;
        }
        long now = player.level().getGameTime();
        List<Placement> placed = PLACED.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        placed.removeIf(entry -> now - entry.time > WINDOW_TICKS);
        placed.add(new Placement(now, count));
        int total = 0;
        for (Placement entry : placed) {
            total += entry.count;
        }
        if (total >= BLOCKS) {
            ModCriteria.STACKS_ON_STACKS.get().trigger(player);
            placed.clear();
        }
    }

    public static void forget(UUID player) {
        PLACED.remove(player);
    }

    public static void forgetAll() {
        PLACED.clear();
    }

    private record Placement(long time, int count) {
    }
}
