package com.buffspotions.potion;

import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** The brewing registry for whichever side is currently running a mix. */
public final class ActiveBrewing {
    private static final ThreadLocal<PotionBrewing> FORCED = new ThreadLocal<>();
    private static Supplier<PotionBrewing> client = () -> null;

    private ActiveBrewing() {
    }

    /** Use this brewing registry for the duration of {@code action}, including on threads with no server. */
    public static void using(PotionBrewing brewing, Runnable action) {
        PotionBrewing previous = FORCED.get();
        FORCED.set(brewing);
        try {
            action.run();
        } finally {
            if (previous == null) {
                FORCED.remove();
            } else {
                FORCED.set(previous);
            }
        }
    }

    public static void bindClient(Supplier<PotionBrewing> supplier) {
        client = supplier;
    }

    public static PotionBrewing get() {
        PotionBrewing forced = FORCED.get();
        if (forced != null) {
            return forced;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.potionBrewing();
        }
        return client.get();
    }
}
