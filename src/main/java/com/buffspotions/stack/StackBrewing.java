package com.buffspotions.stack;

import com.buffspotions.BuffsPotions;
import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * Brews one dose for the brewing stack. Torchflower and pitcher plant perform the
 * second thicken or extend. Every other ingredient uses the ordinary brewing recipes.
 */
public final class StackBrewing {
    private StackBrewing() {
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.is(Items.POPPED_CHORUS_FRUIT);
    }

    public static boolean isIngredient(PotionBrewing brewing, ItemStack stack) {
        return stack.is(Items.TORCHFLOWER)
                || stack.is(Items.PITCHER_PLANT)
                || stack.is(Items.GUNPOWDER)
                || stack.is(Items.DRAGON_BREATH)
                || brewing.isIngredient(stack);
    }

    public static ItemStack mix(PotionBrewing brewing, ItemStack dose, ItemStack ingredient) {
        if (dose.isEmpty() || ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (ingredient.is(Items.TORCHFLOWER)) {
            return upgrade(dose, Upgrade.TORCHFLOWER);
        }
        if (ingredient.is(Items.PITCHER_PLANT)) {
            return upgrade(dose, Upgrade.PITCHER);
        }
        if (!brewing.hasMix(dose, ingredient)) {
            return ItemStack.EMPTY;
        }
        ItemStack mixed = brewing.mix(ingredient, dose.copy());
        // A flagon cannot hold a splash or lingering potion.
        if (!(mixed.getItem() instanceof PotionItem) || mixed.getItem() instanceof ThrowablePotionItem) {
            return ItemStack.EMPTY;
        }
        return preserve(dose, mixed);
    }

    private static ItemStack preserve(ItemStack from, ItemStack to) {
        var essence = from.get(ModDataComponents.ESSENCE.get());
        if (essence != null && to.get(ModDataComponents.ESSENCE.get()) == null) {
            to.set(ModDataComponents.ESSENCE.get(), essence);
        }
        if (MundaneBrewing.isMundane(from) && !MundaneBrewing.isMundane(to)) {
            to.set(ModDataComponents.MUNDANE.get(), true);
        }
        return to;
    }

    private static ItemStack upgrade(ItemStack dose, Upgrade upgrade) {
        if (Remedies.isRemedy(dose)) {
            return ItemStack.EMPTY;
        }
        PotionContents contents = dose.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return ItemStack.EMPTY;
        }
        String name = contents.customName().or(() -> contents.potion().map(potion -> potion.value().name())).orElse("");
        if (name.isEmpty()) {
            return ItemStack.EMPTY;
        }
        List<MobEffectInstance> effects = new ArrayList<>();
        contents.getAllEffects().forEach(effects::add);
        if (effects.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Form form = form(contents, name, effects.getFirst());
        Form next = switch (upgrade) {
            case TORCHFLOWER -> switch (form) {
                case STRONG -> Form.LEVEL_THREE;
                case EXTENDED -> Form.CLARIFIED;
                default -> null;
            };
            case PITCHER -> switch (form) {
                case EXTENDED -> Form.DOUBLE_EXTENDED;
                case STRONG -> Form.CLARIFIED;
                default -> null;
            };
        };
        if (next == null) {
            return ItemStack.EMPTY;
        }
        int base = BuffsPotionsConfig.baseFor(name);
        boolean raise = next == Form.LEVEL_THREE || (next == Form.CLARIFIED && form == Form.EXTENDED);
        if (raise && !supportsHigherPotency(name)) {
            return ItemStack.EMPTY;
        }
        int duration = switch (next) {
            case LEVEL_THREE -> Math.max(1, base / 4);
            case DOUBLE_EXTENDED -> Math.max(1, base * 4);
            case CLARIFIED -> Math.max(1, (int) Math.round(base * 1.5));
            default -> base;
        };
        List<MobEffectInstance> brewed = new ArrayList<>(effects.size());
        boolean changed = false;
        for (MobEffectInstance effect : effects) {
            int ticks = effect.getDuration() <= 1 ? effect.getDuration() : duration;
            int amplifier = effect.getAmplifier();
            if (raise) {
                amplifier++;
            }
            if (ticks != effect.getDuration() || amplifier != effect.getAmplifier()) {
                changed = true;
            }
            brewed.add(new MobEffectInstance(
                    effect.getEffect(),
                    ticks,
                    amplifier,
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }
        if (!changed) {
            return ItemStack.EMPTY;
        }
        ItemStack result = dose.copy();
        result.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.empty(),
                contents.customColor(),
                brewed,
                Optional.of(name)
        ));
        return result;
    }

    private static Form form(PotionContents contents, String name, MobEffectInstance sample) {
        String path = contents.potion().flatMap(Holder::unwrapKey).map(key -> key.identifier().getPath()).orElse("");
        if (path.startsWith("long_")) {
            return Form.EXTENDED;
        }
        if (path.startsWith("strong_")) {
            return Form.STRONG;
        }
        if (path.isEmpty()) {
            int base = BuffsPotionsConfig.baseFor(name);
            int duration = sample.getDuration();
            int amplifier = sample.getAmplifier();
            if (duration <= 1) {
                return amplifier > 0 ? Form.STRONG : Form.BASE;
            }
            if (near(duration, Math.max(1, base * 4))) {
                return Form.DOUBLE_EXTENDED;
            }
            if (near(duration, Math.max(1, (int) Math.round(base * 1.5))) && amplifier > 0) {
                return Form.CLARIFIED;
            }
            if (near(duration, Math.max(1, base / 4)) && amplifier >= 2) {
                return Form.LEVEL_THREE;
            }
            if (near(duration, Math.max(1, base * 2)) && amplifier == 0) {
                return Form.EXTENDED;
            }
            if (near(duration, Math.max(1, base / 2)) && amplifier > 0) {
                return Form.STRONG;
            }
        }
        return Form.BASE;
    }

    private static boolean near(int actual, int expected) {
        return Math.abs(actual - expected) <= 1;
    }

    /** Glowstone can make a level II of this potion, so the stack may thicken it further. */
    private static boolean supportsHigherPotency(String name) {
        String base = name;
        if (base.startsWith("long_")) {
            base = base.substring("long_".length());
        } else if (base.startsWith("strong_")) {
            base = base.substring("strong_".length());
        }
        return BuiltInRegistries.POTION.containsKey(Identifier.withDefaultNamespace("strong_" + base))
                || BuiltInRegistries.POTION.containsKey(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "strong_" + base));
    }

    private enum Upgrade {
        TORCHFLOWER,
        PITCHER
    }

    private enum Form {
        BASE,
        EXTENDED,
        STRONG,
        LEVEL_THREE,
        DOUBLE_EXTENDED,
        CLARIFIED
    }
}
