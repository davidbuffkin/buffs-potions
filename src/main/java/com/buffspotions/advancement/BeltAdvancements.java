package com.buffspotions.advancement;

import com.buffspotions.belt.BeltContents;
import com.buffspotions.belt.BeltEnchantments;
import com.buffspotions.belt.BeltSource;
import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.FlagonBeltItem;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModEffects;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Belt, reach, and air-step advancements that are not plain data triggers. */
public final class BeltAdvancements {
    private BeltAdvancements() {
    }

    public static void survey(ServerPlayer player) {
        FlagonContents ender = EnderFlagons.of(player);
        if (ender.doses() >= FlagonContents.MAX_DOSES && instantHealth(ender.dose())) {
            ModCriteria.ELIXIR_OF_LIFE.get().trigger(player);
        }
        ItemStack equipped = BeltSource.equipped(player);
        if (!equipped.isEmpty()) {
            surveyBelt(player, equipped);
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof PotionBeltItem) {
                surveyBelt(player, stack);
            }
        }
    }

    public static void onDrink(ServerPlayer player, ItemStack belt, ItemStack dose) {
        if (potionDose(dose)) {
            if (BeltEnchantments.level(belt, BeltEnchantments.PROTRACTION) > 0) {
                ModCriteria.PROTRACTED.get().trigger(player);
            }
            if (BeltEnchantments.level(belt, BeltEnchantments.AMPLIFICATION) > 0) {
                ModCriteria.AMPLIFIED.get().trigger(player);
            }
        }
        if (player.getData(ModAttachments.BELT_USE).gulp() && honey(dose)) {
            ModCriteria.WHOS_HUNGRY.get().trigger(player);
        }
    }

    public static void onEffectAdded(ServerPlayer player, MobEffectInstance added) {
        if (added != null && added.is(ModEffects.REACHING) && added.getAmplifier() >= 3) {
            ModCriteria.REACH_FOUR.get().trigger(player);
        }
        boolean air = has(player, added, ModEffects.WIND_JUMPING);
        boolean jump = has(player, added, MobEffects.JUMP_BOOST);
        boolean slow = has(player, added, MobEffects.SLOW_FALLING);
        if (air && jump && slow) {
            ModCriteria.AIR_BENDER.get().trigger(player);
        }
    }

    private static void surveyBelt(ServerPlayer player, ItemStack belt) {
        if (!(belt.getItem() instanceof PotionBeltItem)) {
            return;
        }
        int slots = BeltEnchantments.capacity(belt);
        if (belt.getItem() instanceof FlagonBeltItem) {
            if (BeltEnchantments.hasGulpReflex(belt) && fullHoneyFlagons(belt, slots) >= 9) {
                ModCriteria.SWEET_TOOTH.get().trigger(player);
            }
            return;
        }
        if (distinctPotions(belt, slots) >= 9) {
            ModCriteria.GLASS_MENAGERIE.get().trigger(player);
        }
    }

    private static int distinctPotions(ItemStack belt, int slots) {
        Set<String> kinds = new HashSet<>();
        for (int slot = 0; slot < slots; slot++) {
            String kind = potionKind(BeltContents.get(belt, slot));
            if (kind != null) {
                kinds.add(kind);
            }
        }
        return kinds.size();
    }

    private static int fullHoneyFlagons(ItemStack belt, int slots) {
        int count = 0;
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = BeltContents.get(belt, slot);
            if (!(stack.getItem() instanceof FlagonItem)) {
                continue;
            }
            FlagonContents contents = FlagonContents.of(stack);
            if (contents.isHoney() && contents.doses() >= FlagonContents.MAX_DOSES) {
                count++;
            }
        }
        return count;
    }

    /**
     * Empty bottles, water, and potions with no effects are ignored. Honey counts on its own.
     * Two potions match when they grant the same effects, whatever the level or duration.
     * A remedy is named for the effect it clears, so two remedies of the same effect count once.
     */
    private static String potionKind(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.GLASS_BOTTLE)) {
            return null;
        }
        if (stack.is(Items.HONEY_BOTTLE)) {
            return "honey";
        }
        if (!(stack.getItem() instanceof PotionItem)) {
            return null;
        }
        if (Remedies.isRemedy(stack)) {
            Holder<MobEffect> effect = Remedies.effectOf(stack);
            return effect == null ? null : "remedy:" + id(effect);
        }
        PotionContents contents = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (contents.is(Potions.WATER)) {
            return null;
        }
        Set<String> effects = new TreeSet<>();
        for (MobEffectInstance effect : contents.getAllEffects()) {
            effects.add(id(effect.getEffect()));
        }
        return effects.isEmpty() ? null : String.join("|", effects);
    }

    private static String id(Holder<?> holder) {
        return holder.unwrapKey().map(key -> key.identifier().toString()).orElse("unknown");
    }

    private static boolean potionDose(ItemStack dose) {
        if (dose.getItem() instanceof FlagonItem) {
            return FlagonContents.of(dose).dose().getItem() instanceof PotionItem;
        }
        return dose.getItem() instanceof PotionItem;
    }

    /** Any Instant Health level counts. A potion that also grants something else does not. */
    private static boolean instantHealth(ItemStack dose) {
        PotionContents contents = dose.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        boolean found = false;
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (!effect.is(MobEffects.INSTANT_HEALTH)) {
                return false;
            }
            found = true;
        }
        return found;
    }

    private static boolean honey(ItemStack dose) {
        if (dose.is(Items.HONEY_BOTTLE)) {
            return true;
        }
        if (dose.getItem() instanceof FlagonItem) {
            FlagonContents contents = FlagonContents.of(dose);
            return contents.isHoney() && contents.doses() > 0;
        }
        return false;
    }

    private static boolean has(ServerPlayer player, MobEffectInstance added, Holder<MobEffect> effect) {
        return (added != null && added.is(effect)) || player.hasEffect(effect);
    }
}
