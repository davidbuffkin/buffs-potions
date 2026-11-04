package com.buffspotions.effect;

import com.buffspotions.registry.ModEffects;
import net.minecraft.world.entity.LivingEntity;

/** Thrown potions, lingering clouds, and tipped arrows cannot affect a stoppered living entity. */
public final class Stopper {
    private static final ThreadLocal<Boolean> PROJECTILE = ThreadLocal.withInitial(() -> false);

    private Stopper() {
    }

    public static void begin() {
        PROJECTILE.set(true);
    }

    public static void end() {
        PROJECTILE.set(false);
    }

    public static boolean blocks(LivingEntity entity) {
        return PROJECTILE.get() && entity.hasEffect(ModEffects.STOPPERED);
    }
}
