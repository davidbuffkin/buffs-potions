package com.buffspotions.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

public class InstantFormationEffect extends InstantaneousMobEffect {
    public InstantFormationEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int remainingDuration, int amplification) {
        return false;
    }

    @Override
    public void applyInstantaneousEffect(
            ServerLevel level,
            @Nullable Entity source,
            @Nullable Entity owner,
            LivingEntity mob,
            int amplification,
            double scale
    ) {
        Formation.drink(level, mob);
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier) {
        if (mob.level() instanceof ServerLevel level) {
            Formation.drink(level, mob);
        }
    }
}
