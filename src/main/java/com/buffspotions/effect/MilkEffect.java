package com.buffspotions.effect;

import com.buffspotions.registry.ModCriteria;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import org.jspecify.annotations.Nullable;

public class MilkEffect extends InstantaneousMobEffect {
    public MilkEffect(MobEffectCategory category, int color) {
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
        if (source instanceof ThrownSplashPotion
                && owner instanceof ServerPlayer player
                && target instanceof Mob
                && !target.getActiveEffects().isEmpty()) {
            ModCriteria.PASTEURIZATION.get().trigger(player);
        }
        target.removeAllEffects();
    }
}
