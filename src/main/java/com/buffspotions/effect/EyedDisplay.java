package com.buffspotions.effect;

import com.buffspotions.registry.ModEffects;
import net.minecraft.world.entity.EntityType;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;

public final class EyedDisplay {
    public static final ThreadLocal<EssenceSubject> TOOLTIP = new ThreadLocal<>();
    /** True while a tooltip is describing an infinite disguise dose. */
    public static final ThreadLocal<Boolean> BOUNDLESS = new ThreadLocal<>();
    public static volatile Supplier<Component> SENSE = () -> null;
    public static volatile Supplier<Component> EATER = () -> null;
    public static volatile Supplier<Component> VOMITING = () -> null;
    public static volatile Supplier<Component> SEEKING = () -> null;
    public static volatile Supplier<Component> DISGUISE = () -> null;

    private EyedDisplay() {
    }

    public static MutableComponent effectLine(Holder<MobEffect> effect, int amplifier) {
        EssenceSubject subject = TOOLTIP.get();
        if (subject == null) {
            return null;
        }
        String key = null;
        Component name = null;
        if (subject.block().isPresent()) {
            if (effect.unwrapKey().equals(ModEffects.BLOCK_SENSE.unwrapKey())) {
                key = "effect.buffs_potions.block_sense.named";
            } else if (effect.unwrapKey().equals(ModEffects.INSTANT_FORMATION.unwrapKey())) {
                key = "effect.buffs_potions.instant_formation.named";
            }
            name = subject.blockLabel();
        }
        if (key == null && subject.item().isPresent() && effect.unwrapKey().equals(ModEffects.ITEM_EATER.unwrapKey())) {
            key = "effect.buffs_potions.item_eater.named";
            name = subject.label();
        }
        if (key == null && subject.item().isPresent() && effect.unwrapKey().equals(ModEffects.ITEM_VOMITING.unwrapKey())) {
            key = "effect.buffs_potions.item_vomiting.named";
            name = subject.label();
        }
        if (key == null && effect.unwrapKey().equals(ModEffects.SEEKING.unwrapKey())
                && (subject.item().isPresent() || subject.playerName().isPresent())) {
            key = "effect.buffs_potions.seeking.named";
            name = com.buffspotions.potion.EssenceNames.seekingSubject(subject);
        }
        if (key == null && effect.unwrapKey().equals(ModEffects.DISGUISE.unwrapKey()) && subject.playerName().isPresent()) {
            key = Boolean.TRUE.equals(BOUNDLESS.get())
                    ? "effect.buffs_potions.disguise.boundless"
                    : "effect.buffs_potions.disguise.named";
            name = subject.label();
        }
        if (key == null && effect.unwrapKey().equals(ModEffects.INSTANT_INCARNATION.unwrapKey())) {
            key = "effect.buffs_potions.instant_incarnation.named";
            name = subject.entity().map(holder -> holder.value().getDescription())
                    .or(() -> subject.item().flatMap(MobDrops::type).map(EntityType::getDescription))
                    .orElseGet(() -> Component.translatable("effect.buffs_potions.instant_incarnation.unknown_mob"));
        }
        if (key == null) {
            return null;
        }
        MutableComponent line = Component.translatable(key, name);
        if (amplifier > 0) {
            line = Component.translatable(
                    "potion.withAmplifier",
                    line,
                    Component.translatable("potion.potency." + amplifier)
            );
        }
        return line;
    }
}
