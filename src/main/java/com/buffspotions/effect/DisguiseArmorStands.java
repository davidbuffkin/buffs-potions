package com.buffspotions.effect;

import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;

/**
 * Armor stands refuse potions. Disguise, a disguise remedy, and milk are the exceptions.
 */
public final class DisguiseArmorStands {
    private DisguiseArmorStands() {
    }

    public static void splash(ServerLevel level, ThrownSplashPotion potion, ItemStack stack, HitResult hitResult) {
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        MobEffectInstance disguise = null;
        boolean milk = false;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.is(ModEffects.DISGUISE)) {
                disguise = effect;
            } else if (effect.is(ModEffects.MILK)) {
                milk = true;
            }
        }
        Holder<MobEffect> remedy = Remedies.applying();
        boolean disguiseRemedy = remedy != null && remedy.is(ModEffects.DISGUISE);
        EssenceSubject subject = Eyedentification.applying();
        String name = subject == null ? null : subject.playerName().orElse(null);
        boolean namedDisguise = disguise != null && name != null;
        if (!namedDisguise && !milk && !disguiseRemedy) {
            return;
        }
        float durationScale = stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
        AABB potionAabb = potion.getBoundingBox().move(hitResult.getLocation().subtract(potion.position()));
        AABB effectAabb = potionAabb.inflate(4.0, 2.0, 4.0);
        float margin = ProjectileUtil.computeMargin(potion);
        for (ArmorStand stand : level.getEntitiesOfClass(ArmorStand.class, effectAabb)) {
            double distance = potionAabb.distanceToSqr(stand.getBoundingBox().inflate(margin));
            if (distance >= 16.0) {
                continue;
            }
            double scale = 1.0 - Math.sqrt(distance) / 4.0;
            if (milk) {
                ModEffects.MILK.value().applyInstantaneousEffect(level, potion, potion.getOwner(), stand, 0, scale);
                clear(stand);
            }
            if (disguiseRemedy) {
                ModEffects.REMEDY.value().applyInstantaneousEffect(level, potion, potion.getOwner(), stand, 0, scale);
                clear(stand);
            }
            if (!namedDisguise) {
                continue;
            }
            int duration = disguise.mapDuration(ticks -> (int) (scale * ticks * durationScale + 0.5));
            if (duration >= 0 && duration < 20) {
                continue;
            }
            MobEffectInstance applied = new MobEffectInstance(
                    disguise.getEffect(),
                    MobEffectInstance.INFINITE_DURATION,
                    disguise.getAmplifier(),
                    disguise.isAmbient(),
                    disguise.isVisible()
            );
            if (stand.addEffect(applied, potion)) {
                stand.setData(ModAttachments.DISGUISE.get(), name);
            }
        }
    }

    public static void clear(LivingEntity entity) {
        entity.setData(ModAttachments.DISGUISE.get(), "");
    }
}
