package com.buffspotions.client;

import com.buffspotions.recipebook.RecipeMystery;
import com.buffspotions.stand.StandRecipeDisplay;
import com.buffspotions.stand.StandSubjects;
import com.buffspotions.still.StillRecipeDisplay;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplay;

/** Client half of {@link RecipeMystery}: which book buttons stay, and what a search is allowed to match. */
public final class RecipeMysteryView {
    private RecipeMysteryView() {
    }

    /**
     * A cycling button with nothing known yet. Single unknown recipes stay, so they can show a question mark.
     */
    public static boolean omit(RecipeCollection collection, Player player, ContextMap context) {
        if (!RecipeMystery.ENABLED || player == null) {
            return false;
        }
        List<RecipeDisplayEntry> selected = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY);
        if (selected.isEmpty() || !cycle(selected)) {
            return false;
        }
        for (RecipeDisplayEntry entry : selected) {
            for (ItemStack stack : entry.resultItems(context)) {
                if (!stack.isEmpty() && !RecipeMystery.concealed(player, stack)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Whether this collection should remain while the search box holds {@code query}. */
    public static boolean matches(RecipeCollection collection, Player player, ContextMap context, String query) {
        if (!RecipeMystery.ENABLED || player == null || query == null || query.isEmpty()) {
            return true;
        }
        String needle = query.toLowerCase(Locale.ROOT);
        List<RecipeDisplayEntry> selected = collection.getSelectedRecipes(RecipeCollection.CraftableStatus.ANY);
        boolean cycling = cycle(selected);
        for (RecipeDisplayEntry entry : selected) {
            for (ItemStack stack : entry.resultItems(context)) {
                if (stack.isEmpty()) {
                    continue;
                }
                if (RecipeMystery.concealed(player, stack)) {
                    if (!cycling && mysteryQuery(ingredientName(entry, context), needle)) {
                        return true;
                    }
                    continue;
                }
                if (knownQuery(stack, needle)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static ItemStack ingredient(RecipeCollection collection, RecipeDisplayId id, ContextMap context) {
        for (RecipeDisplayEntry entry : collection.getRecipes()) {
            if (!entry.id().equals(id)) {
                continue;
            }
            ItemStack stack = first(ingredientSlot(entry), context);
            if (!stack.isEmpty()) {
                return stack;
            }
            return first(bottleSlot(entry), context);
        }
        return ItemStack.EMPTY;
    }

    private static boolean cycle(List<RecipeDisplayEntry> selected) {
        if (selected.size() > 1) {
            return true;
        }
        for (RecipeDisplayEntry entry : selected) {
            if (entry.display() instanceof StandRecipeDisplay stand && stand.subjects() != StandSubjects.Kind.NONE) {
                return true;
            }
        }
        return false;
    }

    private static boolean mysteryQuery(String ingredient, String needle) {
        String label = Component.translatable(RecipeMystery.LABEL).getString().toLowerCase(Locale.ROOT);
        return label.contains(needle) || ingredient.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean knownQuery(ItemStack stack, String needle) {
        com.buffspotions.flagon.FlagonContents flagon = com.buffspotions.flagon.FlagonContents.of(stack);
        ItemStack named = flagon.isEmpty() ? stack : flagon.dose();
        StringBuilder blob = new StringBuilder(stack.getHoverName().getString().toLowerCase(Locale.ROOT));
        blob.append(' ').append(named.getHoverName().getString().toLowerCase(Locale.ROOT));
        for (MobEffectInstance instance : effects(named)) {
            blob.append(' ')
                    .append(Component.translatable(instance.getEffect().value().getDescriptionId()).getString().toLowerCase(Locale.ROOT));
        }
        if (com.buffspotions.potion.Remedies.isRemedy(named)) {
            blob.append(' ').append(Component.translatable(
                    com.buffspotions.potion.Remedies.effectOf(named).value().getDescriptionId()
            ).getString().toLowerCase(Locale.ROOT));
        }
        return blob.toString().contains(needle);
    }

    private static Iterable<MobEffectInstance> effects(ItemStack stack) {
        var contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return contents == null ? List.of() : contents.getAllEffects();
    }

    private static String ingredientName(RecipeDisplayEntry entry, ContextMap context) {
        ItemStack stack = first(ingredientSlot(entry), context);
        return stack.isEmpty() ? "" : stack.getHoverName().getString();
    }

    private static SlotDisplay ingredientSlot(RecipeDisplayEntry entry) {
        if (entry.display() instanceof StillRecipeDisplay still) {
            return still.ingredient();
        }
        if (entry.display() instanceof StandRecipeDisplay stand) {
            return stand.ingredient();
        }
        return null;
    }

    private static SlotDisplay bottleSlot(RecipeDisplayEntry entry) {
        if (entry.display() instanceof StillRecipeDisplay still) {
            return still.bottle();
        }
        if (entry.display() instanceof StandRecipeDisplay stand) {
            return stand.bottle();
        }
        return null;
    }

    private static ItemStack first(SlotDisplay slot, ContextMap context) {
        if (slot == null) {
            return ItemStack.EMPTY;
        }
        List<ItemStack> stacks = slot.resolveForStacks(context);
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.getFirst().copy();
    }
}
