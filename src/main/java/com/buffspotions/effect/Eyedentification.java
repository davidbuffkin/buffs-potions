package com.buffspotions.effect;

import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

public final class Eyedentification {
    private static final ThreadLocal<EssenceSubject> APPLYING = new ThreadLocal<>();
    private static final ThreadLocal<EssenceSubject> PENDING_CLOUD = new ThreadLocal<>();

    private Eyedentification() {
    }

    public static void beginApply(ItemStack stack) {
        beginSubject(stack.get(ModDataComponents.ESSENCE.get()));
    }

    public static void beginSubject(EssenceSubject subject) {
        if (subject != null) {
            APPLYING.set(subject);
        }
    }

    public static EssenceSubject applying() {
        return APPLYING.get();
    }

    public static void endApply() {
        APPLYING.remove();
        Incarnation.clearAt();
        Incarnation.clearSuppress();
        Formation.clear();
    }

    public static void pendingCloud(ItemStack stack) {
        EssenceSubject subject = stack.get(ModDataComponents.ESSENCE.get());
        if (subject != null) {
            PENDING_CLOUD.set(subject);
        }
    }

    public static void captureCloud(AreaEffectCloud cloud) {
        EssenceSubject subject = PENDING_CLOUD.get();
        if (subject == null) {
            return;
        }
        PENDING_CLOUD.remove();
        cloud.setData(ModAttachments.LINGERING_SUBJECT.get(), subject);
    }

    public static void clearPendingCloud() {
        PENDING_CLOUD.remove();
    }

    public static void onAdded(MobEffectEvent.Added event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof Zombie zombie) {
            if (!zombie.isBaby()) {
                rememberDisguise(zombie, event);
            }
            return;
        }
        if (event.getEntity() instanceof Villager villager) {
            if (!villager.isBaby()) {
                rememberDisguise(villager, event);
            }
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        EssenceSubject subject = subjectOf(event);
        if (subject == null) {
            return;
        }
        MobEffectInstance effect = event.getEffectInstance();
        if (!parameterized(effect.getEffect())) {
            return;
        }
        if (effect.is(ModEffects.BLOCK_SENSE) && subject.block().isEmpty()) {
            return;
        }
        if ((effect.is(ModEffects.ITEM_EATER) || effect.is(ModEffects.ITEM_VOMITING)) && subject.item().isEmpty()) {
            return;
        }
        if (effect.is(ModEffects.SEEKING) && subject.item().isEmpty() && subject.playerName().isEmpty()) {
            return;
        }
        if (effect.is(ModEffects.DISGUISE) && subject.playerName().isEmpty()) {
            return;
        }
        ParameterDoses.add(player, effect.getEffect(), subject, effect.getDuration(), effect.getAmplifier());
    }

    private static void rememberDisguise(LivingEntity entity, MobEffectEvent.Added event) {
        if (!event.getEffectInstance().is(ModEffects.DISGUISE)) {
            return;
        }
        EssenceSubject subject = subjectOf(event);
        if (subject == null || subject.playerName().isEmpty()) {
            return;
        }
        entity.setData(ModAttachments.DISGUISE.get(), subject.playerName().get());
    }

    private static EssenceSubject subjectOf(MobEffectEvent.Added event) {
        EssenceSubject subject = APPLYING.get();
        if (subject != null) {
            return subject;
        }
        Entity source = event.getEffectSource();
        if (source instanceof AreaEffectCloud cloud && cloud.hasData(ModAttachments.LINGERING_SUBJECT.get())) {
            return cloud.getData(ModAttachments.LINGERING_SUBJECT.get());
        }
        return null;
    }

    public static void onCleared(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        if (player.level().isClientSide()) {
            return;
        }
        if (parameterized(effect)) {
            ParameterDoses.clear(player, effect);
        }
        forget(player, effect);
    }

    public static void onExpired(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        if (player.level().isClientSide() || player.hasEffect(effect)) {
            return;
        }
        if (ParameterDoses.alive(player, effect)) {
            return;
        }
        ParameterDoses.clear(player, effect);
        forget(player, effect);
    }

    private static boolean parameterized(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        return effect.is(ModEffects.BLOCK_SENSE)
                || effect.is(ModEffects.ITEM_EATER)
                || effect.is(ModEffects.ITEM_VOMITING)
                || effect.is(ModEffects.SEEKING)
                || effect.is(ModEffects.DISGUISE);
    }

    private static void forget(Player player, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect) {
        if (effect.unwrapKey().equals(ModEffects.BLOCK_STACKED.unwrapKey())) {
            BlockStacking.forget(player);
        } else if (effect.unwrapKey().equals(ModEffects.BLOCK_CRACKED.unwrapKey())) {
            BlockCracking.forget(player);
        } else if (effect.unwrapKey().equals(ModEffects.ITEM_VOMITING.unwrapKey())) {
            ItemVomiting.forget(player);
        } else if (effect.unwrapKey().equals(ModEffects.SEEKING.unwrapKey())) {
            Seeking.forget(player);
        }
    }
}
