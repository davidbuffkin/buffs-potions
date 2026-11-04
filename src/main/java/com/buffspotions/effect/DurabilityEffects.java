package com.buffspotions.effect;

import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class DurabilityEffects {
    private DurabilityEffects() {
    }

    public static boolean affects(ItemStack stack) {
        return stack.isDamageableItem();
    }

    public static int damageMultiplier(int amplifier) {
        int level = Math.max(0, amplifier) + 1;
        if (level >= 30) {
            return Integer.MAX_VALUE;
        }
        int factor = BuffsPotionsConfig.fumblingFactor();
        int multiplied = 1;
        for (int step = 0; step < level; step++) {
            if (multiplied > Integer.MAX_VALUE / factor) {
                return Integer.MAX_VALUE;
            }
            multiplied *= factor;
        }
        return multiplied;
    }

    public static double skipChance(int amplifier) {
        int level = Math.max(0, amplifier) + 1;
        return 1.0 - Math.pow(BuffsPotionsConfig.finesseDurabilityChance(), level);
    }

    public static int scaleIncomingDamage(int amount, ItemStack stack, @Nullable LivingEntity entity) {
        if (amount <= 0 || entity == null || !affects(stack)) {
            return amount;
        }
        MobEffectInstance fumbling = entity.getEffect(ModEffects.FUMBLING);
        if (fumbling == null) {
            return amount;
        }
        int factor = damageMultiplier(fumbling.getAmplifier());
        if (amount > Integer.MAX_VALUE / factor) {
            return Integer.MAX_VALUE;
        }
        return amount * factor;
    }

    public static boolean skipDamage(int amount, ItemStack stack, @Nullable LivingEntity entity) {
        if (amount <= 0 || entity == null || !affects(stack)) {
            return false;
        }
        MobEffectInstance finesse = entity.getEffect(ModEffects.FINESSE);
        if (finesse == null) {
            return false;
        }
        return entity.getRandom().nextDouble() < skipChance(finesse.getAmplifier());
    }
}
