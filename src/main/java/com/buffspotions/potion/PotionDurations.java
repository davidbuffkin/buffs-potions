package com.buffspotions.potion;

import com.buffspotions.config.BuffsPotionsConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.alchemy.Potion;

public final class PotionDurations {
    private static final Set<String> VANILLA = Set.of(
            "night_vision",
            "invisibility",
            "leaping",
            "fire_resistance",
            "swiftness",
            "slowness",
            "water_breathing",
            "healing",
            "harming",
            "poison",
            "regeneration",
            "strength",
            "weakness",
            "luck",
            "slow_falling",
            "turtle_master",
            "wind_charged",
            "weaving",
            "oozing",
            "infested"
    );

    private PotionDurations() {
    }

    public static List<MobEffectInstance> rewrite(Potion potion, List<MobEffectInstance> effects) {
        if (!VANILLA.contains(potion.name())) {
            return effects;
        }
        int base = BuffsPotionsConfig.baseFor(potion.name());
        List<MobEffectInstance> next = new ArrayList<>(effects.size());
        boolean changed = false;
        for (MobEffectInstance effect : effects) {
            int duration = scaled(potion.name(), effect, base);
            int amplifier = amplifier(potion.name(), effect);
            if (duration == effect.getDuration() && amplifier == effect.getAmplifier()) {
                next.add(effect);
            } else {
                changed = true;
                next.add(new MobEffectInstance(
                        effect.getEffect(),
                        duration,
                        amplifier,
                        effect.isAmbient(),
                        effect.isVisible(),
                        effect.showIcon()
                ));
            }
        }
        return changed ? next : effects;
    }

    /** Vanilla strong slowness is level IV, and turtle master starts above level I. */
    private static int amplifier(String name, MobEffectInstance effect) {
        if ("slowness".equals(name) && effect.is(MobEffects.SLOWNESS) && effect.getAmplifier() == 3) {
            return 1;
        }
        if ("turtle_master".equals(name)) {
            return strongTurtle(effect) ? 1 : 0;
        }
        return effect.getAmplifier();
    }

    private static boolean strongTurtle(MobEffectInstance effect) {
        if (effect.getDuration() == 800) {
            return false;
        }
        if (effect.is(MobEffects.SLOWNESS)) {
            return effect.getAmplifier() >= 5;
        }
        return effect.getAmplifier() >= 3;
    }

    private static int scaled(String name, MobEffectInstance effect, int base) {
        int duration = effect.getDuration();
        if (duration <= 1) {
            return duration;
        }
        if ("turtle_master".equals(name)) {
            if (duration == 800) {
                return Math.max(1, base * 2);
            }
            boolean viscid = effect.getAmplifier() >= 5
                    || (effect.is(MobEffects.RESISTANCE) && effect.getAmplifier() >= 3);
            return viscid ? Math.max(1, base / 2) : base;
        }
        if (effect.getAmplifier() > 0) {
            return Math.max(1, base / 2);
        }
        if (extended(name, duration)) {
            return Math.max(1, base * 2);
        }
        return base;
    }

    private static boolean extended(String name, int duration) {
        return switch (name) {
            case "poison", "regeneration" -> duration == 1800;
            case "slowness", "weakness", "slow_falling" -> duration == 4800;
            default -> duration == 9600;
        };
    }
}
