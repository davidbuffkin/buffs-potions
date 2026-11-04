package com.buffspotions.event;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.Visitation;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class VisitationEvents {
    @SubscribeEvent
    public static void onAdded(MobEffectEvent.Added event) {
        if (event.getOldEffectInstance() != null
                || !event.getEffectInstance().is(ModEffects.DISLOCATED)
                || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Visitation.depart(player, event.getEffectInstance().getAmplifier());
    }

    @SubscribeEvent
    public static void onRemove(MobEffectEvent.Remove event) {
        if (isDislocated(event.getEffect())) {
            finish(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onExpire(MobEffectEvent.Expired event) {
        MobEffectInstance effect = event.getEffectInstance();
        if (effect != null && isDislocated(effect.getEffect())) {
            finish(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Visitation.tick(player);
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Visitation.guard(event.getEntity());
    }

    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasData(ModAttachments.ENDER_SHADOW)) {
            return;
        }
        event.setCanceled(true);
        Visitation.redirectDamage(entity, event.getSource(), event.getOriginalAmount());
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (Visitation.suppressesKnockback(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity().hasData(ModAttachments.ENDER_SHADOW)) {
            event.setCanceled(true);
            event.getEntity().setHealth(event.getEntity().getMaxHealth());
            return;
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            Visitation.beforeOwnerDeath(player);
            Visitation.discard(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Visitation.hideShadow(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Visitation.restoreShadow(player);
        }
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) {
            return;
        }
        if (event.getOriginal() instanceof ServerPlayer original) {
            Visitation.discard(original);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            player.removeData(ModAttachments.DISLOCATION);
        }
    }

    private static void finish(LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player) || !player.hasData(ModAttachments.DISLOCATION)) {
            return;
        }
        if (player.isAlive()) {
            Visitation.recall(player);
        } else {
            Visitation.discard(player);
        }
    }

    private static boolean isDislocated(Holder<MobEffect> effect) {
        return effect.unwrapKey().equals(ModEffects.DISLOCATED.unwrapKey());
    }

    private VisitationEvents() {
    }
}
