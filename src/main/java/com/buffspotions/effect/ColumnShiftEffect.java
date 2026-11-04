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

public class ColumnShiftEffect extends InstantaneousMobEffect {
    private final boolean ascent;

    public ColumnShiftEffect(MobEffectCategory category, int color, boolean ascent) {
        super(category, color);
        this.ascent = ascent;
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
        if (!this.ascent
                && source instanceof ThrownSplashPotion
                && owner instanceof ServerPlayer player
                && mob instanceof Mob) {
            ModCriteria.BEGONE.get().trigger(player);
        }
        super.applyInstantaneousEffect(level, source, owner, mob, amplification, scale);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        if (this.ascent) {
            ColumnShift.ascend(level, mob, amplification);
        } else {
            ColumnShift.descend(level, mob, amplification);
        }
        return true;
    }
}
