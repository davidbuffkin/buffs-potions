package com.buffspotions.effect;

import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Parameterized effects share one vanilla {@link MobEffectInstance} per type.
 * The subject, amplifier, and expiry of each dose live here so diamond sense and iron sense can both be active.
 */
public final class ParameterDoses {
    private static final List<Holder<MobEffect>> KINDS = List.of(
            ModEffects.BLOCK_SENSE,
            ModEffects.ITEM_EATER,
            ModEffects.ITEM_VOMITING,
            ModEffects.SEEKING,
            ModEffects.DISGUISE
    );

    private ParameterDoses() {
    }

    public static List<ParameterDose> merge(List<ParameterDose> current, ParameterDose incoming) {
        List<ParameterDose> next = new ArrayList<>(current.size() + 1);
        boolean found = false;
        for (ParameterDose dose : current) {
            if (!found && dose.effect().equals(incoming.effect()) && sameSubject(dose.subject(), incoming.subject())) {
                next.add(new ParameterDose(
                        incoming.effect(),
                        incoming.subject(),
                        Math.max(dose.amplifier(), incoming.amplifier()),
                        Math.max(dose.expiresAt(), incoming.expiresAt())
                ));
                found = true;
            } else {
                next.add(dose);
            }
        }
        if (!found) {
            next.add(incoming);
        }
        return List.copyOf(next);
    }

    public static void add(Player player, Holder<MobEffect> effect, EssenceSubject subject, int duration, int amplifier) {
        if (player.level().isClientSide() || subject == null) {
            return;
        }
        long expires = duration < 0
                ? ParameterDose.NEVER
                : player.level().getGameTime() + Math.max(1, duration);
        List<ParameterDose> current = new ArrayList<>(stored(player));
        if (!hasKind(current, effect)) {
            current.addAll(legacy(player, effect));
        }
        String id = effectId(effect);
        if (effect.unwrapKey().equals(ModEffects.SEEKING.unwrapKey())
                || effect.unwrapKey().equals(ModEffects.DISGUISE.unwrapKey())) {
            current.removeIf(dose -> dose.effect().equals(id) && !sameSubject(dose.subject(), subject));
        }
        player.setData(
                ModAttachments.PARAMETER_DOSES.get(),
                merge(current, new ParameterDose(id, subject, Math.max(0, amplifier), expires))
        );
        mirror(player, effect, subject);
        if (effect.unwrapKey().equals(ModEffects.SEEKING.unwrapKey())) {
            Seeking.begin(player);
        }
    }

    public static boolean has(Player player, Holder<MobEffect> effect, EssenceSubject subject) {
        String id = effectId(effect);
        for (ParameterDose dose : matching(player, effect)) {
            if (dose.effect().equals(id) && sameSubject(dose.subject(), subject)) {
                return true;
            }
        }
        return false;
    }

    public static List<ParameterDose> matching(Player player, Holder<MobEffect> effect) {
        List<ParameterDose> stored = ofKind(stored(player), effect);
        if (!stored.isEmpty()) {
            return stored;
        }
        return legacy(player, effect);
    }

    public static int amplifierForItem(Player player, Holder<MobEffect> effect, ItemStack stack) {
        int best = -1;
        for (ParameterDose dose : matching(player, effect)) {
            if (dose.subject().item().isPresent()
                    && stack.is(dose.subject().item().get())
                    && dose.amplifier() > best) {
                best = dose.amplifier();
            }
        }
        return best;
    }

    public static int amplifierForBlock(Player player, Holder<MobEffect> effect, BlockState state) {
        int best = -1;
        for (ParameterDose dose : matching(player, effect)) {
            if (dose.subject().block().isPresent()
                    && state.is(dose.subject().block().get())
                    && dose.amplifier() > best) {
                best = dose.amplifier();
            }
        }
        return best;
    }

    public static boolean alive(Player player, Holder<MobEffect> effect) {
        long now = player.level().getGameTime();
        for (ParameterDose dose : ofKind(stored(player), effect)) {
            if (dose.expiresAt() > now) {
                return true;
            }
        }
        return false;
    }

    public static void tick(MinecraftServer server) {
        for (Player player : server.getPlayerList().getPlayers()) {
            adoptLegacy(player);
            dropExpired(player);
            for (Holder<MobEffect> effect : KINDS) {
                List<ParameterDose> doses = ofKind(stored(player), effect);
                if (doses.isEmpty()) {
                    if (player.hasEffect(effect)) {
                        player.removeEffect(effect);
                    }
                    if (effect.is(ModEffects.DISGUISE) && !player.getData(ModAttachments.DISGUISE.get()).isEmpty()) {
                        player.setData(ModAttachments.DISGUISE.get(), "");
                    }
                    continue;
                }
                reconcile(player, effect, doses);
            }
        }
    }

    public static void clear(Player player, Holder<MobEffect> effect) {
        if (player.level().isClientSide()) {
            return;
        }
        String id = effectId(effect);
        List<ParameterDose> next = new ArrayList<>();
        for (ParameterDose dose : stored(player)) {
            if (!dose.effect().equals(id)) {
                next.add(dose);
            }
        }
        player.setData(ModAttachments.PARAMETER_DOSES.get(), List.copyOf(next));
        if (effect.unwrapKey().equals(ModEffects.BLOCK_SENSE.unwrapKey())) {
            player.removeData(ModAttachments.BLOCK_SENSE);
        } else if (effect.unwrapKey().equals(ModEffects.ITEM_EATER.unwrapKey())) {
            player.removeData(ModAttachments.ITEM_EATER);
        } else if (effect.unwrapKey().equals(ModEffects.ITEM_VOMITING.unwrapKey())) {
            player.removeData(ModAttachments.ITEM_VOMITING);
        } else if (effect.unwrapKey().equals(ModEffects.SEEKING.unwrapKey())) {
            player.removeData(ModAttachments.SEEKING);
        } else if (effect.unwrapKey().equals(ModEffects.DISGUISE.unwrapKey())) {
            player.setData(ModAttachments.DISGUISE.get(), "");
        }
    }

    public static boolean sameSubject(EssenceSubject left, EssenceSubject right) {
        if (left.playerName().isPresent() || right.playerName().isPresent()) {
            return left.playerName().isPresent()
                    && right.playerName().isPresent()
                    && left.playerName().get().equalsIgnoreCase(right.playerName().get());
        }
        if (left.entity().isPresent() || right.entity().isPresent()) {
            return left.entity().flatMap(Holder::unwrapKey).equals(right.entity().flatMap(Holder::unwrapKey));
        }
        return itemKey(left).equals(itemKey(right));
    }

    public static String effectId(Holder<MobEffect> effect) {
        return effect.unwrapKey().map(key -> key.identifier().toString()).orElse("");
    }

    private static void adoptLegacy(Player player) {
        List<ParameterDose> current = new ArrayList<>(stored(player));
        boolean changed = false;
        for (Holder<MobEffect> effect : KINDS) {
            if (hasKind(current, effect)) {
                continue;
            }
            List<ParameterDose> inherited = legacy(player, effect);
            if (!inherited.isEmpty()) {
                current.addAll(inherited);
                changed = true;
            }
        }
        if (changed) {
            player.setData(ModAttachments.PARAMETER_DOSES.get(), List.copyOf(current));
        }
    }

    private static void dropExpired(Player player) {
        long now = player.level().getGameTime();
        List<ParameterDose> current = stored(player);
        List<ParameterDose> next = null;
        for (ParameterDose dose : current) {
            if (dose.expiresAt() <= now) {
                if (next == null) {
                    next = new ArrayList<>();
                    for (ParameterDose kept : current) {
                        if (kept == dose) {
                            break;
                        }
                        next.add(kept);
                    }
                }
                continue;
            }
            if (next != null) {
                next.add(dose);
            }
        }
        if (next != null) {
            player.setData(ModAttachments.PARAMETER_DOSES.get(), List.copyOf(next));
        }
    }

    private static void reconcile(Player player, Holder<MobEffect> effect, List<ParameterDose> doses) {
        long now = player.level().getGameTime();
        boolean infinite = false;
        int remaining = 1;
        int strongest = 0;
        for (ParameterDose dose : doses) {
            if (dose.expiresAt() == ParameterDose.NEVER) {
                infinite = true;
            } else {
                remaining = Math.max(remaining, (int) Math.min(Integer.MAX_VALUE, dose.expiresAt() - now));
            }
            strongest = Math.max(strongest, dose.amplifier());
        }
        int shown = doses.size() > 1 ? 0 : strongest;
        int duration = infinite ? MobEffectInstance.INFINITE_DURATION : remaining;
        MobEffectInstance current = player.getEffect(effect);
        boolean visible = current == null || current.isVisible();
        boolean ambient = current != null && current.isAmbient();
        boolean refresh = current == null || current.getAmplifier() != shown;
        if (!refresh) {
            refresh = infinite
                    ? !current.isInfiniteDuration()
                    : !current.isInfiniteDuration() && current.getDuration() + 5 < remaining;
        }
        if (refresh) {
            player.forceAddEffect(new MobEffectInstance(effect, duration, shown, ambient, visible), null);
        }
    }

    private static List<ParameterDose> legacy(Player player, Holder<MobEffect> effect) {
        if (!player.hasEffect(effect)) {
            return List.of();
        }
        MobEffectInstance instance = player.getEffect(effect);
        if (instance == null) {
            return List.of();
        }
        EssenceSubject subject = legacySubject(player, effect);
        if (subject == null) {
            return List.of();
        }
        long expires = instance.isInfiniteDuration()
                ? ParameterDose.NEVER
                : player.level().getGameTime() + Math.max(1, instance.getDuration());
        return List.of(new ParameterDose(effectId(effect), subject, instance.getAmplifier(), expires));
    }

    private static EssenceSubject legacySubject(Player player, Holder<MobEffect> effect) {
        if (effect.unwrapKey().equals(ModEffects.BLOCK_SENSE.unwrapKey())) {
            return blockSubject(player, ModAttachments.BLOCK_SENSE.get());
        }
        if (effect.unwrapKey().equals(ModEffects.ITEM_EATER.unwrapKey())) {
            return itemSubject(player, ModAttachments.ITEM_EATER.get());
        }
        if (effect.unwrapKey().equals(ModEffects.ITEM_VOMITING.unwrapKey())) {
            return itemSubject(player, ModAttachments.ITEM_VOMITING.get());
        }
        if (effect.unwrapKey().equals(ModEffects.DISGUISE.unwrapKey())) {
            String name = player.getData(ModAttachments.DISGUISE.get());
            if (name != null && !name.isBlank()) {
                return EssenceSubject.ofPlayer(name);
            }
        }
        if (effect.unwrapKey().equals(ModEffects.SEEKING.unwrapKey()) && player.hasData(ModAttachments.SEEKING.get())) {
            EssenceSubject subject = player.getData(ModAttachments.SEEKING.get());
            if (subject.item().isPresent() || subject.playerName().isPresent()) {
                return subject;
            }
        }
        return null;
    }

    private static EssenceSubject blockSubject(Player player, net.neoforged.neoforge.attachment.AttachmentType<TrackedBlock> type) {
        if (!player.hasData(type)) {
            return null;
        }
        TrackedBlock tracked = player.getData(type);
        if (tracked.block().isEmpty()) {
            return null;
        }
        Item item = tracked.block().get().value().asItem();
        if (item == Items.AIR) {
            return null;
        }
        return EssenceSubject.ofItem(item.builtInRegistryHolder());
    }

    private static EssenceSubject itemSubject(Player player, net.neoforged.neoforge.attachment.AttachmentType<TrackedItem> type) {
        if (!player.hasData(type)) {
            return null;
        }
        TrackedItem tracked = player.getData(type);
        if (tracked.item().isEmpty()) {
            return null;
        }
        return EssenceSubject.ofItem(tracked.item().get());
    }

    private static List<ParameterDose> stored(Player player) {
        return player.getData(ModAttachments.PARAMETER_DOSES.get());
    }

    private static List<ParameterDose> ofKind(List<ParameterDose> doses, Holder<MobEffect> effect) {
        String id = effectId(effect);
        List<ParameterDose> matched = null;
        for (ParameterDose dose : doses) {
            if (!dose.effect().equals(id)) {
                continue;
            }
            if (matched == null) {
                matched = new ArrayList<>();
            }
            matched.add(dose);
        }
        return matched == null ? List.of() : matched;
    }

    private static boolean hasKind(List<ParameterDose> doses, Holder<MobEffect> effect) {
        String id = effectId(effect);
        for (ParameterDose dose : doses) {
            if (dose.effect().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static void mirror(Player player, Holder<MobEffect> effect, EssenceSubject subject) {
        if (subject.block().isPresent() && effect.unwrapKey().equals(ModEffects.BLOCK_SENSE.unwrapKey())) {
            player.setData(ModAttachments.BLOCK_SENSE.get(), TrackedBlock.of(subject.block().get()));
        } else if (subject.item().isPresent() && effect.unwrapKey().equals(ModEffects.ITEM_EATER.unwrapKey())) {
            player.setData(ModAttachments.ITEM_EATER.get(), TrackedItem.of(subject.item().get()));
        } else if (subject.item().isPresent() && effect.unwrapKey().equals(ModEffects.ITEM_VOMITING.unwrapKey())) {
            player.setData(ModAttachments.ITEM_VOMITING.get(), TrackedItem.of(subject.item().get()));
        } else if (effect.unwrapKey().equals(ModEffects.SEEKING.unwrapKey())) {
            player.setData(ModAttachments.SEEKING.get(), subject);
        } else if (effect.unwrapKey().equals(ModEffects.DISGUISE.unwrapKey()) && subject.playerName().isPresent()) {
            player.setData(ModAttachments.DISGUISE.get(), subject.playerName().get());
        }
    }

    private static String itemKey(EssenceSubject subject) {
        return subject.item().flatMap(holder -> holder.unwrapKey().map(key -> key.identifier().toString())).orElse("");
    }
}
