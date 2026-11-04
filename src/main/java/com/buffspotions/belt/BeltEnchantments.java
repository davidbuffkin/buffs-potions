package com.buffspotions.belt;

import com.buffspotions.BuffsPotions;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.item.LingeringFlagonBeltItem;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.registry.ModDataComponents;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jspecify.annotations.Nullable;

public final class BeltEnchantments {
    /** Refresh a belt effect once fewer than three seconds remain. Quick Quaff waits until under one second. */
    public static final int GULP_REMAINING_TICKS = 3 * 20;
    public static final int QUICK_GULP_REMAINING_TICKS = 20;
    public static final ResourceKey<Enchantment> PROTRACTION = key("protraction");
    public static final ResourceKey<Enchantment> AMPLIFICATION = key("amplification");
    public static final ResourceKey<Enchantment> QUICK_QUAFF = key("quick_quaff");
    public static final ResourceKey<Enchantment> GAUGE = key("gauge");
    public static final ResourceKey<Enchantment> GULP_REFLEX = key("gulp_reflex");
    public static final ResourceKey<Enchantment> TIPPING = key("tipping");
    public static final float TIPPED_ARROW_DURATION_SCALE = 0.125F;

    public static int level(ItemStack belt, ResourceKey<Enchantment> enchantment) {
        if (belt.isEmpty()) {
            return 0;
        }
        ItemEnchantments enchantments = belt.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            if (entry.getKey().is(enchantment)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    public static float durationMultiplier(ItemStack belt) {
        return durationMultiplierForLevel(level(belt, PROTRACTION));
    }

    public static float durationMultiplierForLevel(int level) {
        if (level <= 0) {
            return 1.0F;
        }
        return 1.0F + Math.min(level, 3) / 3.0F;
    }

    public static int amplifierBonus(ItemStack belt) {
        return level(belt, AMPLIFICATION) > 0 ? 1 : 0;
    }

    public static boolean hasQuickQuaff(ItemStack belt) {
        return level(belt, QUICK_QUAFF) > 0;
    }

    public static boolean hasGauge(ItemStack belt) {
        return level(belt, GAUGE) > 0;
    }

    public static boolean hasGulpReflex(ItemStack belt) {
        return level(belt, GULP_REFLEX) > 0;
    }

    public static boolean hasTipping(ItemStack belt) {
        return belt.getItem() instanceof LingeringFlagonBeltItem && level(belt, TIPPING) > 0;
    }

    public static void strip(ItemStack belt, ResourceKey<Enchantment> enchantment) {
        ItemEnchantments current = belt.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        mutable.removeIf(holder -> holder.is(enchantment));
        belt.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    /**
     * Bakes protraction into durations and amplification into amplifiers. The caller still applies
     * the item's own duration scale (splash 1, lingering cloud 0.25, tipped arrow 0.125).
     */
    public static PotionContents modifiedContents(PotionContents contents, ItemStack belt) {
        float durationMultiplier = durationMultiplier(belt);
        int amplifierBonus = amplifierBonus(belt);
        if (contents == null || (durationMultiplier == 1.0F && amplifierBonus <= 0)) {
            return contents == null ? PotionContents.EMPTY : contents;
        }
        List<MobEffectInstance> effects = new ArrayList<>();
        for (MobEffectInstance effect : contents.getAllEffects()) {
            int bonus = amplifierBonus > 0 && hasStrongForm(effect.getEffect()) ? amplifierBonus : 0;
            int amplifier = Math.min(MobEffectInstance.MAX_AMPLIFIER, effect.getAmplifier() + bonus);
            int duration = effect.getDuration();
            if (!effect.getEffect().value().isInstantaneous() && durationMultiplier != 1.0F && duration > 0) {
                duration = (int) Math.min(Integer.MAX_VALUE, Math.max(1, Math.round(duration * (double) durationMultiplier)));
            }
            effects.add(new MobEffectInstance(
                    effect.getEffect(),
                    duration,
                    amplifier,
                    effect.isAmbient(),
                    effect.isVisible(),
                    effect.showIcon()
            ));
        }
        Optional<String> name = contents.customName().or(() -> contents.potion().map(potion -> potion.value().name()));
        return new PotionContents(Optional.empty(), Optional.of(contents.getColor()), effects, name);
    }

    public static void copyDoseMarkers(ItemStack dose, ItemStack projectile) {
        var essence = dose.get(ModDataComponents.ESSENCE.get());
        if (essence != null) {
            projectile.set(ModDataComponents.ESSENCE.get(), essence);
        }
        var remedy = dose.get(ModDataComponents.REMEDY.get());
        if (remedy != null) {
            projectile.set(ModDataComponents.REMEDY.get(), remedy);
        }
        if (dose.has(ModDataComponents.MUNDANE.get())) {
            projectile.set(ModDataComponents.MUNDANE.get(), dose.get(ModDataComponents.MUNDANE.get()));
        }
    }

    public static int capacity(ItemStack belt) {
        return hasGauge(belt) ? BeltContents.MAX_SLOTS : BeltContents.BASE_SLOTS;
    }

    public static int row(ItemStack belt) {
        if (!hasGauge(belt)) {
            return 0;
        }
        return Math.clamp(belt.getOrDefault(ModDataComponents.BELT_ROW.get(), 0), 0, 1);
    }

    public static int rowStart(ItemStack belt) {
        return row(belt) * BeltContents.BASE_SLOTS;
    }

    public static int displayedActive(ItemStack belt) {
        int active = BeltContents.activeSlot(belt);
        int origin = rowStart(belt);
        if (active >= origin && active < origin + BeltContents.BASE_SLOTS) {
            return active;
        }
        return origin + Math.floorMod(active, BeltContents.BASE_SLOTS);
    }

    public static ItemStack modifierSource(Player player, ItemStack consumed) {
        ItemStack explicit = BeltDrinkContext.explicitSource();
        if (!explicit.isEmpty()) {
            return explicit;
        }
        if (consumed.getItem() instanceof PotionBeltItem) {
            return ItemStack.EMPTY;
        }
        return BeltSource.equipped(player);
    }

    public static void applyModified(Player player, PotionContents contents, ItemStack stack, float durationMultiplier, int amplifierBonus) {
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        float scale = stack.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F) * durationMultiplier;
        contents.forEachEffect(effect -> {
            int bonus = amplifierBonus > 0 && hasStrongForm(effect.getEffect()) ? amplifierBonus : 0;
            int amplifier = Math.min(MobEffectInstance.MAX_AMPLIFIER, effect.getAmplifier() + bonus);
            if (effect.getEffect().value().isInstantaneous()) {
                effect.getEffect().value().applyInstantaneousEffect(serverLevel, player, player, player, amplifier, 1.0);
            } else {
                player.addEffect(new MobEffectInstance(
                        effect.getEffect(),
                        effect.getDuration(),
                        amplifier,
                        effect.isAmbient(),
                        effect.isVisible(),
                        effect.showIcon()
                ));
            }
        }, scale);
    }

    private static Set<Holder<MobEffect>> strongEffects;

    /** Night vision and fire resistance have no level II, so amplification leaves them alone. */
    private static boolean hasStrongForm(Holder<MobEffect> effect) {
        if (strongEffects == null) {
            Set<Holder<MobEffect>> found = new HashSet<>();
            BuiltInRegistries.POTION.listElements().forEach(potion -> {
                for (MobEffectInstance instance : potion.value().getEffects()) {
                    if (instance.getAmplifier() > 0) {
                        found.add(instance.getEffect());
                    }
                }
            });
            strongEffects = found;
        }
        return strongEffects.contains(effect);
    }

    public static Optional<Integer> gulpSlot(Player player, ItemStack belt) {
        if (!hasGulpReflex(belt) || player.isUsingItem() || player.getCooldowns().isOnCooldown(belt)) {
            return Optional.empty();
        }
        int limit = capacity(belt);
        int lead = gulpLead(belt);
        for (int slot = 0; slot < limit; slot++) {
            if (shouldAutoDrink(player, BeltContents.get(belt, slot), lead)) {
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public static boolean hasRoomForHoney(int foodLevel) {
        return foodLevel <= 20 - 6;
    }

    public static boolean hasRoomForHoney(Player player) {
        return hasRoomForHoney(player.getFoodData().getFoodLevel());
    }

    public static int gulpLead(ItemStack belt) {
        return hasQuickQuaff(belt) ? QUICK_GULP_REMAINING_TICKS : GULP_REMAINING_TICKS;
    }

    public static boolean shouldAutoDrink(Player player, ItemStack dose) {
        return shouldAutoDrink(player, dose, GULP_REMAINING_TICKS);
    }

    public static boolean shouldAutoDrink(Player player, ItemStack dose, int leadTicks) {
        ItemStack effectItem = dose;
        if (dose.getItem() instanceof FlagonItem flagon) {
            FlagonContents stored = flagon.playerContents(player, dose);
            if (stored.isHoney()) {
                return hasRoomForHoney(player);
            }
            if (stored.isEmpty() || stored.isMilk()) {
                return false;
            }
            effectItem = stored.dose();
        }
        if (effectItem.is(Items.HONEY_BOTTLE)) {
            return hasRoomForHoney(player);
        }
        if (!(effectItem.getItem() instanceof PotionItem) || effectItem.getItem() instanceof ThrowablePotionItem) {
            return false;
        }
        PotionContents contents = effectItem.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().value().isInstantaneous()) {
                continue;
            }
            if (effectNeedsRefresh(effect, player.getEffect(effect.getEffect()), leadTicks)) {
                return true;
            }
        }
        return false;
    }

    public static boolean effectNeedsRefresh(MobEffectInstance effect, @Nullable MobEffectInstance active) {
        return effectNeedsRefresh(effect, active, GULP_REMAINING_TICKS);
    }

    public static boolean effectNeedsRefresh(MobEffectInstance effect, @Nullable MobEffectInstance active, int leadTicks) {
        if (effect.getEffect().value().isInstantaneous()) {
            return false;
        }
        if (active == null) {
            return true;
        }
        return !active.isInfiniteDuration() && active.getDuration() < leadTicks;
    }

    /** A gulp belt in the main inventory still has a night vision dose, so the view can stay steady. */
    public static boolean steadiesNightVision(Player player) {
        for (int slot = 9; slot < 36; slot++) {
            ItemStack belt = player.getInventory().getItem(slot);
            if (hasGulpReflex(belt) && containsNightVision(player, belt)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsNightVision(Player player, ItemStack belt) {
        if (!(belt.getItem() instanceof PotionBeltItem)) {
            return false;
        }
        int limit = capacity(belt);
        for (int slot = 0; slot < limit; slot++) {
            if (isNightVisionDose(player, BeltContents.get(belt, slot))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNightVisionDose(Player player, ItemStack stored) {
        if (stored.isEmpty()) {
            return false;
        }
        ItemStack effectItem = stored;
        if (stored.getItem() instanceof FlagonItem flagon) {
            FlagonContents contents = flagon.playerContents(player, stored);
            if (contents.doses() <= 0) {
                return false;
            }
            effectItem = contents.dose();
        }
        PotionContents potion = effectItem.get(DataComponents.POTION_CONTENTS);
        if (potion == null) {
            return false;
        }
        for (MobEffectInstance effect : potion.getAllEffects()) {
            if (effect.is(MobEffects.NIGHT_VISION)) {
                return true;
            }
        }
        return false;
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, path));
    }

    private BeltEnchantments() {
    }
}
