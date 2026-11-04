package com.buffspotions.potion;

import com.buffspotions.effect.ParameterDoses;
import com.buffspotions.effect.Stopper;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModEffects;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/** A drink that removes one effect, at any level and any duration. */
public final class Remedies {
    private static final String MARK = "remedy:";
    private static final ThreadLocal<Holder<MobEffect>> APPLYING = new ThreadLocal<>();

    private Remedies() {
    }

    public static ItemStack create(Holder<MobEffect> effect, Item container) {
        String id = effect.unwrapKey().map(key -> key.identifier().toString()).orElse("");
        ItemStack stack = new ItemStack(container);
        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(
                Optional.empty(),
                Optional.of(effect.value().getColor()),
                List.of(new MobEffectInstance(ModEffects.REMEDY, 1)),
                Optional.of(MARK + id)
        ));
        stack.set(ModDataComponents.REMEDY.get(), effect);
        return stack;
    }

    public static boolean isRemedy(ItemStack stack) {
        return effectOf(stack) != null;
    }

    public static Holder<MobEffect> effectOf(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Holder<MobEffect> direct = stack.get(ModDataComponents.REMEDY.get());
        if (direct != null) {
            return direct;
        }
        return fromContents(stack.get(DataComponents.POTION_CONTENTS));
    }

    public static Holder<MobEffect> effectOf(DataComponentGetter components) {
        if (components == null) {
            return null;
        }
        Holder<MobEffect> direct = components.get(ModDataComponents.REMEDY.get());
        if (direct != null) {
            return direct;
        }
        return fromContents(components.get(DataComponents.POTION_CONTENTS));
    }

    public static Holder<MobEffect> fromContents(PotionContents contents) {
        if (contents == null) {
            return null;
        }
        String name = contents.customName().orElse("");
        if (!name.startsWith(MARK)) {
            return null;
        }
        Identifier id = Identifier.tryParse(name.substring(MARK.length()));
        if (id == null) {
            return null;
        }
        return BuiltInRegistries.MOB_EFFECT.get(id).orElse(null);
    }

    public static void beginApply(ItemStack stack) {
        APPLYING.set(effectOf(stack));
    }

    public static void beginApply(PotionContents contents) {
        APPLYING.set(fromContents(contents));
    }

    public static Holder<MobEffect> applying() {
        return APPLYING.get();
    }

    public static void endApply() {
        APPLYING.remove();
    }

    public static void apply(LivingEntity entity, ItemStack stack) {
        Holder<MobEffect> effect = effectOf(stack);
        if (effect == null || Stopper.blocks(entity)) {
            return;
        }
        clear(entity, effect);
    }

    public static void clear(LivingEntity entity, Holder<MobEffect> effect) {
        if (entity instanceof Player player) {
            ParameterDoses.clear(player, effect);
        }
        entity.removeEffect(effect);
    }

    public static Component name(ItemStack stack) {
        Holder<MobEffect> effect = effectOf(stack);
        if (effect == null) {
            return null;
        }
        String key = switch (container(stack.getItem())) {
            case "splash" -> "item.buffs_potions.remedy.splash";
            case "lingering" -> "item.buffs_potions.remedy.lingering";
            case "arrow" -> "item.buffs_potions.remedy.arrow";
            default -> "item.buffs_potions.remedy";
        };
        return Component.translatable(key, label(effect));
    }

    public static Component flagonName(ItemStack dose) {
        Holder<MobEffect> effect = effectOf(dose);
        if (effect == null) {
            return null;
        }
        return Component.translatable("item.buffs_potions.remedy.flagon", label(effect));
    }

    public static boolean appendTooltip(DataComponentGetter components, java.util.function.Consumer<Component> lines) {
        Holder<MobEffect> effect = effectOf(components);
        if (effect == null) {
            return false;
        }
        lines.accept(Component.translatable("item.buffs_potions.remedy.clears", label(effect))
                .withStyle(effect.value().getCategory().getTooltipFormatting()));
        return true;
    }

    public static Component label(Holder<MobEffect> effect) {
        if (effect.unwrapKey().equals(ModEffects.BLOCK_STACKED.unwrapKey())) {
            return Component.translatable("effect.buffs_potions.stacking");
        }
        return Component.translatable(effect.value().getDescriptionId());
    }

    private static String container(Item item) {
        if (item == Items.SPLASH_POTION) {
            return "splash";
        }
        if (item == Items.LINGERING_POTION) {
            return "lingering";
        }
        if (item == Items.TIPPED_ARROW) {
            return "arrow";
        }
        return "potion";
    }
}
