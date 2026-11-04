package com.buffspotions.effect;

import com.buffspotions.potion.SuspiciousBrew;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/** Marker on suspicious brew so drinking, splashing, and lingering all roll an effect. */
public class SuspiciousBrewEffect extends InstantaneousMobEffect {
    public SuspiciousBrewEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public void applyInstantaneousEffect(
            ServerLevel level,
            @Nullable Entity source,
            @Nullable Entity owner,
            LivingEntity target,
            int amplification,
            double scale
    ) {
        SuspiciousBrew.apply(target, source, scale);
    }
}
