package com.buffspotions.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.InstantaneousMobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;

public class WeatherCallEffect extends InstantaneousMobEffect {
    private final boolean rain;

    public WeatherCallEffect(MobEffectCategory category, int color, boolean rain) {
        super(category, color);
        this.rain = rain;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        if (this.rain) {
            WeatherCalls.scheduleRain(level.getServer());
        } else {
            WeatherCalls.scheduleClear(level.getServer());
        }
        if (mob instanceof ServerPlayer) {
            mob.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100));
            LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
            if (bolt != null) {
                bolt.snapTo(mob.getX(), mob.getY(), mob.getZ());
                level.addFreshEntity(bolt);
            }
        }
        return true;
    }
}
