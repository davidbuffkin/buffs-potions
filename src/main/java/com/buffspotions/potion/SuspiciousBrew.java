package com.buffspotions.potion;

import com.buffspotions.effect.Stopper;
import com.buffspotions.mixin.AreaEffectCloudAccess;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModPotions;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.brewing.IBrewingRecipe;
import org.jspecify.annotations.Nullable;

/** Beetroot brewed into water. A flower pot can turn it into that flower's mundane potion. */
public final class SuspiciousBrew {
    /** Average color of the beetroot item texture. */
    public static final int COLOR = 0x882B2A;
    public static final int DRINK_TICKS = 10 * 20;

    private SuspiciousBrew() {
    }

    public static boolean is(ItemStack stack) {
        return stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.SUSPICIOUS_BREW);
    }

    public static Component unknownEffect() {
        return Component.translatable("effect.buffs_potions.unknown").withStyle(ChatFormatting.GRAY);
    }

    /**
     * Rolls one lasting effect. A drink lasts {@link #DRINK_TICKS}. A splash shortens that by
     * distance, and a lingering cloud uses the cloud's duration scale.
     */
    public static void apply(LivingEntity entity, @Nullable Entity source, double scale) {
        if (entity.level().isClientSide() || Stopper.blocks(entity)) {
            return;
        }
        int duration = duration(source, scale);
        if (duration <= 20) {
            return;
        }
        List<Holder<MobEffect>> choices = drinkable();
        if (choices.isEmpty()) {
            return;
        }
        Holder<MobEffect> effect = choices.get(entity.getRandom().nextInt(choices.size()));
        entity.addEffect(new MobEffectInstance(effect, duration), source);
    }

    private static int duration(@Nullable Entity source, double scale) {
        float durationScale;
        if (source instanceof AreaEffectCloud cloud) {
            durationScale = ((AreaEffectCloudAccess) cloud).buffsPotions$potionDurationScale();
        } else if (source instanceof AbstractThrownPotion thrown) {
            float itemScale = thrown.getItem().getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
            durationScale = (float) (scale * itemScale);
        } else {
            durationScale = 1.0F;
        }
        return Mth.floor(DRINK_TICKS * durationScale + 0.5);
    }

    private static ItemStack colored(Item item) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.of(ModPotions.SUSPICIOUS_BREW),
                Optional.of(COLOR),
                List.of(),
                Optional.empty()
        ));
        return stack;
    }

    private static List<Holder<MobEffect>> drinkable() {
        List<Holder<MobEffect>> effects = new ArrayList<>();
        BuiltInRegistries.MOB_EFFECT.listElements().forEach(effect -> {
            if (!effect.value().isInstantaneous() && !excluded(effect)) {
                effects.add(effect);
            }
        });
        return effects;
    }

    private static boolean excluded(Holder<MobEffect> effect) {
        return needsSubject(effect)
                || effect.unwrapKey().equals(ModEffects.QUIET.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.RIOTOUS.unwrapKey());
    }

    private static boolean needsSubject(Holder<MobEffect> effect) {
        return effect.unwrapKey().equals(ModEffects.BLOCK_SENSE.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.ITEM_EATER.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.INSTANT_INCARNATION.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.INSTANT_FORMATION.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.ITEM_VOMITING.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.SEEKING.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.MILK.unwrapKey());
    }

    public static final class Recipe implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            return input.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER);
        }

        @Override
        public boolean isIngredient(ItemStack stack) {
            return stack.is(Items.BEETROOT);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            if (!isInput(input) || !isIngredient(ingredient)) {
                return ItemStack.EMPTY;
            }
            return colored(input.getItem());
        }
    }
}
