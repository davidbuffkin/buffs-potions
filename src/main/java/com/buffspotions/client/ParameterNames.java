package com.buffspotions.client;

import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.effect.ParameterDose;
import com.buffspotions.effect.ParameterDoses;
import java.util.List;
import com.buffspotions.BuffsPotions;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.GatherEffectScreenTooltipsEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID, value = Dist.CLIENT)
public final class ParameterNames {
    private ParameterNames() {
    }

    public static Component join(Holder<MobEffect> effect, String key, boolean preferBlock) {
        List<ParameterDose> doses = doses(effect);
        if (doses == null || doses.isEmpty()) {
            return null;
        }
        if (doses.size() == 1) {
            return line(doses.get(0), key, preferBlock, false, 0);
        }
        MutableComponent joined = Component.empty();
        for (int i = 0; i < doses.size(); i++) {
            if (i > 0) {
                joined.append(", ");
            }
            joined.append(line(doses.get(i), key, preferBlock, false, 0));
        }
        return joined;
    }

    public static boolean several(Holder<MobEffect> effect) {
        List<ParameterDose> doses = doses(effect);
        return doses != null && doses.size() > 1;
    }

    @SubscribeEvent
    public static void tooltip(GatherEffectScreenTooltipsEvent event) {
        MobEffectInstance instance = event.getEffectInstance();
        List<ParameterDose> doses = doses(instance.getEffect());
        if (doses == null || doses.size() <= 1) {
            return;
        }
        String key = key(instance.getEffect());
        if (key == null) {
            return;
        }
        boolean preferBlock = !instance.getEffect().is(com.buffspotions.registry.ModEffects.ITEM_EATER)
                && !instance.getEffect().is(com.buffspotions.registry.ModEffects.ITEM_VOMITING);
        long now = Minecraft.getInstance().player.level().getGameTime();
        List<Component> lines = new ArrayList<>(doses.size());
        for (ParameterDose dose : doses) {
            lines.add(line(dose, key, preferBlock, true, now));
        }
        event.getTooltip().clear();
        event.getTooltip().addAll(lines);
    }

    private static List<ParameterDose> doses(Holder<MobEffect> effect) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        return ParameterDoses.matching(player, effect);
    }

    private static String key(Holder<MobEffect> effect) {
        if (effect.is(com.buffspotions.registry.ModEffects.BLOCK_SENSE)) {
            return "effect.buffs_potions.block_sense.named";
        }
        if (effect.is(com.buffspotions.registry.ModEffects.ITEM_EATER)) {
            return "effect.buffs_potions.item_eater.named";
        }
        if (effect.is(com.buffspotions.registry.ModEffects.ITEM_VOMITING)) {
            return "effect.buffs_potions.item_vomiting.named";
        }
        if (effect.is(com.buffspotions.registry.ModEffects.SEEKING)) {
            return "effect.buffs_potions.seeking.named";
        }
        if (effect.is(com.buffspotions.registry.ModEffects.DISGUISE)) {
            return "effect.buffs_potions.disguise.named";
        }
        return null;
    }

    private static Component line(ParameterDose dose, String key, boolean preferBlock, boolean several, long now) {
        if (key.equals("effect.buffs_potions.disguise.named") && dose.expiresAt() == ParameterDose.NEVER) {
            key = "effect.buffs_potions.disguise.boundless";
        }
        EssenceSubject subject = dose.subject();
        Component thing = key.equals("effect.buffs_potions.seeking.named")
                ? com.buffspotions.potion.EssenceNames.seekingSubject(subject)
                : preferBlock && subject.block().isPresent() ? subject.blockLabel() : subject.label();
        MutableComponent name = Component.translatable(key, thing);
        if (several && dose.amplifier() > 0 && dose.amplifier() <= 9) {
            name.append(" ").append(Component.translatable("enchantment.level." + (dose.amplifier() + 1)));
        }
        if (several) {
            if (dose.expiresAt() == ParameterDose.NEVER) {
                name.append(" (").append(Component.translatable("effect.duration.infinite")).append(")");
            } else {
                int seconds = (int) Math.max(0, (dose.expiresAt() - now) / 20);
                name.append(" (" + seconds / 60 + ":" + (seconds % 60 < 10 ? "0" : "") + (seconds % 60) + ")");
            }
        }
        return name;
    }
}
