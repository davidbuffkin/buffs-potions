package com.buffspotions.advancement;

import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModEffects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/** One splash of disguise has to catch a villager, a zombie, and an armor stand. */
public final class HalloweenSplash {
    private static final ThreadLocal<Hits> ACTIVE = new ThreadLocal<>();

    private HalloweenSplash() {
    }

    public static void begin(ThrownSplashPotion potion, ItemStack stack) {
        ACTIVE.remove();
        if (!disguise(stack)) {
            return;
        }
        Hits hits = new Hits();
        if (potion.getOwner() instanceof ServerPlayer player) {
            hits.owner = player;
        }
        ACTIVE.set(hits);
    }

    public static void note(LivingEntity entity, MobEffectInstance effect) {
        Hits hits = ACTIVE.get();
        if (hits == null || effect == null || !effect.is(ModEffects.DISGUISE)) {
            return;
        }
        if (entity instanceof Villager) {
            hits.villager = true;
        } else if (entity instanceof Zombie) {
            hits.zombie = true;
        } else if (entity instanceof ArmorStand) {
            hits.stand = true;
        }
    }

    public static void end() {
        Hits hits = ACTIVE.get();
        ACTIVE.remove();
        if (hits != null && hits.villager && hits.zombie && hits.stand && hits.owner != null) {
            ModCriteria.HALLOWEEN.get().trigger(hits.owner);
        }
    }

    private static boolean disguise(ItemStack stack) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.is(ModEffects.DISGUISE)) {
                return true;
            }
        }
        return false;
    }

    private static final class Hits {
        private boolean villager;
        private boolean zombie;
        private boolean stand;
        private ServerPlayer owner;
    }
}
