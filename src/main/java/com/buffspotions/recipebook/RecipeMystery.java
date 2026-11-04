package com.buffspotions.recipebook;

import com.buffspotions.belt.BeltContents;
import com.buffspotions.equipment.BeltSlotAccess;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModItems;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.resources.ResourceKey;

/**
 * Hides brewing results until the player has held any potion of that effect.
 *
 * <p>Set {@link #ENABLED} to {@code false} to restore the previous recipe book:
 * every result icon, every cycling group, and the vanilla search index.
 */
public final class RecipeMystery {
    public static final boolean ENABLED = true;
    public static final String LABEL = "gui.buffs_potions.new_potion";

    private static final Map<ServerPlayer, Integer> STAMPS = new WeakHashMap<>();

    private RecipeMystery() {
    }

    public static void survey(ServerPlayer player) {
        if (!ENABLED) {
            return;
        }
        int stamp = player.getInventory().getTimesChanged();
        boolean seeded = player.getData(ModAttachments.KNOWN_EFFECTS_SEEDED);
        if (seeded && stamp == STAMPS.getOrDefault(player, Integer.MIN_VALUE) && player.tickCount % 20 != 0) {
            return;
        }
        STAMPS.put(player, stamp);
        Set<String> known = new HashSet<>(player.getData(ModAttachments.KNOWN_EFFECTS));
        if (!seeded) {
            seed(player, known);
            player.setData(ModAttachments.KNOWN_EFFECTS_SEEDED, true);
        }
        int before = known.size();
        absorb(known, player);
        if (known.size() != before || !seeded) {
            player.setData(ModAttachments.KNOWN_EFFECTS, known);
        }
    }

    /** True when the result should show as an ingredient and a question mark. */
    public static boolean concealed(Player player, ItemStack result) {
        if (!ENABLED || player == null) {
            return false;
        }
        return concealed(player.getData(ModAttachments.KNOWN_EFFECTS), result);
    }

    public static boolean concealed(Set<String> known, ItemStack result) {
        Set<String> effects = effectIds(result);
        if (effects.isEmpty()) {
            return false;
        }
        for (String effect : effects) {
            if (!known.contains(effect)) {
                return true;
            }
        }
        return false;
    }

    public static Set<String> effectIds(ItemStack stack) {
        Set<String> ids = new HashSet<>();
        if (stack == null || stack.isEmpty()) {
            return ids;
        }
        if (Remedies.isRemedy(stack)) {
            add(ids, Remedies.effectOf(stack));
            return ids;
        }
        ItemStack dose = stack;
        if (ModItems.fitsBrewingStack(stack)) {
            FlagonContents contents = FlagonContents.of(stack);
            if (!contents.isEmpty()) {
                dose = contents.dose();
            }
        }
        if (Remedies.isRemedy(dose)) {
            add(ids, Remedies.effectOf(dose));
            return ids;
        }
        PotionContents contents = dose.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return ids;
        }
        for (MobEffectInstance instance : contents.getAllEffects()) {
            if (instance.getEffect().is(ModEffects.REMEDY)) {
                continue;
            }
            add(ids, instance.getEffect());
        }
        return ids;
    }

    private static void seed(ServerPlayer player, Set<String> known) {
        RecipeManager recipes = player.level().getServer().getRecipeManager();
        ContextMap context = SlotDisplayContext.fromLevel(player.level());
        for (ResourceKey<Recipe<?>> id : player.getRecipeBook().pack().known()) {
            RecipeHolder<?> holder = recipes.byKey(id).orElse(null);
            if (holder == null) {
                continue;
            }
            for (RecipeDisplay display : holder.value().display()) {
                for (ItemStack stack : display.result().resolveForStacks(context)) {
                    known.addAll(effectIds(stack));
                }
            }
        }
    }

    private static void absorb(Set<String> known, ServerPlayer player) {
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            absorbStack(known, stack);
        }
        absorbStack(known, player.getOffhandItem());
        absorbStack(known, player.containerMenu.getCarried());
        absorbStack(known, BeltSlotAccess.getEquipped(player));
        absorbStack(known, player.getData(ModAttachments.ENDER_FLAGON).dose());
    }

    private static void absorbStack(Set<String> known, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        known.addAll(effectIds(stack));
        var contents = BeltContents.get(stack);
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            known.addAll(effectIds(contents.getStackInSlot(slot)));
        }
    }

    private static void add(Set<String> ids, Holder<MobEffect> effect) {
        if (effect == null) {
            return;
        }
        effect.unwrapKey().ifPresent(key -> ids.add(key.identifier().toString()));
    }
}
