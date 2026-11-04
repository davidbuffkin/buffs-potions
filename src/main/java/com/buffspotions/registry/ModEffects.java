package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.BloomfootedEffect;
import com.buffspotions.effect.BuffMobEffect;
import com.buffspotions.effect.ColumnShiftEffect;
import com.buffspotions.effect.InstantConfusionEffect;
import com.buffspotions.effect.Formation;
import com.buffspotions.effect.InstantFormationEffect;
import com.buffspotions.effect.InstantIncarnationEffect;
import com.buffspotions.effect.MilkEffect;
import com.buffspotions.effect.ParameterizedEffect;
import com.buffspotions.effect.RemedyEffect;
import com.buffspotions.effect.SuspiciousBrewEffect;
import com.buffspotions.effect.WeatherCallEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, BuffsPotions.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> INSIGHTFUL = EFFECTS.register(
            "insightful",
            () -> new BuffMobEffect(MobEffectCategory.BENEFICIAL, 0x43F080)
    );

    public static final DeferredHolder<MobEffect, MobEffect> MILK = EFFECTS.register(
            "milk",
            () -> new MilkEffect(MobEffectCategory.NEUTRAL, 0xF4F7FB)
    );

    public static final DeferredHolder<MobEffect, MobEffect> INSTANT_CONFUSION = EFFECTS.register(
            "instant_confusion",
            () -> new InstantConfusionEffect(MobEffectCategory.HARMFUL, 0xC431DE)
    );

    public static final DeferredHolder<MobEffect, MobEffect> REACHING = EFFECTS.register(
            "reaching",
            () -> new BuffMobEffect(MobEffectCategory.BENEFICIAL, 0xFC6344)
                    .addAttributeModifier(
                            Attributes.BLOCK_INTERACTION_RANGE,
                            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "effect.reaching.block"),
                            2.0,
                            AttributeModifier.Operation.ADD_VALUE
                    )
                    .addAttributeModifier(
                            Attributes.ENTITY_INTERACTION_RANGE,
                            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "effect.reaching.entity"),
                            2.0,
                            AttributeModifier.Operation.ADD_VALUE
                    )
    );

    public static final DeferredHolder<MobEffect, MobEffect> FINESSE = EFFECTS.register(
            "finesse",
            () -> new BuffMobEffect(MobEffectCategory.BENEFICIAL, 0xFFF7B8)
    );

    public static final DeferredHolder<MobEffect, MobEffect> FUMBLING = EFFECTS.register(
            "fumbling",
            () -> new BuffMobEffect(MobEffectCategory.HARMFUL, 0x874E24)
    );

    public static final DeferredHolder<MobEffect, MobEffect> WIND_JUMPING = EFFECTS.register(
            "wind_jumping",
            () -> new BuffMobEffect(MobEffectCategory.BENEFICIAL, 0xC7E2FF)
    );

    public static final DeferredHolder<MobEffect, MobEffect> BLOOMFOOTED = EFFECTS.register(
            "bloomfooted",
            () -> new BloomfootedEffect(MobEffectCategory.BENEFICIAL, 0xFF96E1)
    );
    public static final DeferredHolder<MobEffect, MobEffect> GREEN_THUMB = EFFECTS.register(
            "green_thumb",
            () -> new BuffMobEffect(MobEffectCategory.BENEFICIAL, 0x1E7A32)
    );

    public static final DeferredHolder<MobEffect, MobEffect> INSTANT_RAIN = EFFECTS.register(
            "instant_rain",
            () -> new WeatherCallEffect(MobEffectCategory.NEUTRAL, 0x1711CF, true)
    );

    public static final DeferredHolder<MobEffect, MobEffect> INSTANT_SUN = EFFECTS.register(
            "instant_sun",
            () -> new WeatherCallEffect(MobEffectCategory.NEUTRAL, 0xFFE866, false)
    );

    public static final DeferredHolder<MobEffect, MobEffect> INSTANT_ASCENT = EFFECTS.register(
            "instant_ascent",
            () -> new ColumnShiftEffect(MobEffectCategory.NEUTRAL, 0x12B0FF, true)
    );

    public static final DeferredHolder<MobEffect, MobEffect> INSTANT_DESCENT = EFFECTS.register(
            "instant_descent",
            () -> new ColumnShiftEffect(MobEffectCategory.HARMFUL, 0x063852, false)
    );

    public static final DeferredHolder<MobEffect, MobEffect> QUIET = EFFECTS.register(
            "quiet",
            () -> new BuffMobEffect(MobEffectCategory.BENEFICIAL, 0xFEFFFA)
    );

    public static final DeferredHolder<MobEffect, MobEffect> RIOTOUS = EFFECTS.register(
            "riotous",
            () -> new BuffMobEffect(MobEffectCategory.HARMFUL, 0x962929)
    );

    public static final DeferredHolder<MobEffect, MobEffect> DISLOCATED = EFFECTS.register(
            "dislocated",
            () -> new BuffMobEffect(MobEffectCategory.NEUTRAL, 0x44078A)
    );

    public static final DeferredHolder<MobEffect, MobEffect> BLOCK_SENSE = EFFECTS.register(
            "block_sense",
            () -> new ParameterizedEffect(MobEffectCategory.BENEFICIAL, 0xFFD53D, ParameterizedEffect.Kind.SENSE)
    );

    public static final DeferredHolder<MobEffect, MobEffect> BLOCK_STACKED = EFFECTS.register(
            "block_stacked",
            () -> new BuffMobEffect(MobEffectCategory.BENEFICIAL, 0xC4350A)
    );

    public static final DeferredHolder<MobEffect, MobEffect> BLOCK_CRACKED = EFFECTS.register(
            "block_cracked",
            () -> new BuffMobEffect(MobEffectCategory.BENEFICIAL, 0x969696)
    );

    public static final DeferredHolder<MobEffect, MobEffect> ITEM_EATER = EFFECTS.register(
            "item_eater",
            () -> new ParameterizedEffect(MobEffectCategory.BENEFICIAL, 0x8F110D, ParameterizedEffect.Kind.EATER)
    );

    public static final DeferredHolder<MobEffect, MobEffect> INSTANT_INCARNATION = EFFECTS.register(
            "instant_incarnation",
            () -> new InstantIncarnationEffect(MobEffectCategory.NEUTRAL, 0x000000)
    );

    public static final DeferredHolder<MobEffect, MobEffect> INSTANT_FORMATION = EFFECTS.register(
            "instant_formation",
            () -> new InstantFormationEffect(MobEffectCategory.NEUTRAL, Formation.COLOR)
    );

    public static final DeferredHolder<MobEffect, MobEffect> ITEM_VOMITING = EFFECTS.register(
            "item_vomiting",
            () -> new ParameterizedEffect(MobEffectCategory.HARMFUL, 0xA2D950, ParameterizedEffect.Kind.VOMITING)
    );

    public static final DeferredHolder<MobEffect, MobEffect> SEEKING = EFFECTS.register(
            "seeking",
            () -> new ParameterizedEffect(MobEffectCategory.BENEFICIAL, 0x42FFD9, ParameterizedEffect.Kind.SEEKING)
    );

    public static final DeferredHolder<MobEffect, MobEffect> DISGUISE = EFFECTS.register(
            "disguise",
            () -> new ParameterizedEffect(MobEffectCategory.BENEFICIAL, 0xAD5F1F, ParameterizedEffect.Kind.DISGUISE)
    );

    public static final DeferredHolder<MobEffect, MobEffect> STOPPERED = EFFECTS.register(
            "stoppered",
            () -> new BuffMobEffect(MobEffectCategory.BENEFICIAL, 0xE0FFF9)
    );

    public static final DeferredHolder<MobEffect, MobEffect> REMEDY = EFFECTS.register(
            "remedy",
            () -> new RemedyEffect(MobEffectCategory.NEUTRAL, 0xFFFFFF)
    );

    public static final DeferredHolder<MobEffect, MobEffect> SUSPICIOUS_BREW = EFFECTS.register(
            "suspicious_brew",
            () -> new SuspiciousBrewEffect(MobEffectCategory.NEUTRAL, 0x882B2A)
    );

    public static void register(IEventBus bus) {
        EFFECTS.register(bus);
    }

    private ModEffects() {
    }
}
