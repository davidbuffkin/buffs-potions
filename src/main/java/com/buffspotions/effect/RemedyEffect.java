package com.buffspotions.effect;

import com.buffspotions.potion.Remedies;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

/** Instant marker brewed into a remedy so splash and lingering clouds know what to clear. */
public class RemedyEffect extends InstantaneousMobEffect {
    public RemedyEffect(MobEffectCategory category, int color) {
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
        Holder<MobEffect> effect = Remedies.applying();
        if (effect == null || Stopper.blocks(target)) {
            return;
        }
        Remedies.clear(target, effect);
    }
}
