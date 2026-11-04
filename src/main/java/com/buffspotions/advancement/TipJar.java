package com.buffspotions.advancement;

import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModCriteria;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/** Five mobs, five different tipped-arrow effects, within ten seconds. */
public final class TipJar {
    private static final int WINDOW_TICKS = 10 * 20;
    private static final Map<UUID, List<Hit>> HITS = new HashMap<>();

    private TipJar() {
    }

    public static void note(AbstractArrow arrow, LivingEntity target) {
        if (!(arrow.getOwner() instanceof ServerPlayer player) || !(target instanceof Mob)) {
            return;
        }
        ItemStack tip = arrow.getData(ModAttachments.ARROW_TIP.get());
        String effects = effectKey(tip);
        if (effects == null) {
            return;
        }
        long now = player.level().getGameTime();
        List<Hit> hits = HITS.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        hits.removeIf(hit -> now - hit.time > WINDOW_TICKS);
        hits.add(new Hit(now, target.getUUID(), effects));
        Set<UUID> mobs = new HashSet<>();
        Set<String> kinds = new HashSet<>();
        for (Hit hit : hits) {
            mobs.add(hit.mob);
            kinds.add(hit.effects);
        }
        if (mobs.size() >= 5 && kinds.size() >= 5) {
            ModCriteria.TIP_JAR.get().trigger(player);
            hits.clear();
        }
    }

    public static void forget(UUID player) {
        HITS.remove(player);
    }

    public static void forgetAll() {
        HITS.clear();
    }

    /** Level and duration are ignored, so Instant Health I and II are one effect. */
    private static String effectKey(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (Remedies.isRemedy(stack)) {
            Holder<MobEffect> effect = Remedies.effectOf(stack);
            return effect == null ? null : "remedy:" + id(effect);
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Set<String> effects = new TreeSet<>();
        for (MobEffectInstance effect : contents.getAllEffects()) {
            effects.add(id(effect.getEffect()));
        }
        return effects.isEmpty() ? null : String.join("|", effects);
    }

    private static String id(Holder<?> holder) {
        return holder.unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
    }

    private record Hit(long time, UUID mob, String effects) {
    }
}
