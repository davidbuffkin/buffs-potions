package com.buffspotions.recipebook;

import com.buffspotions.registry.ModAttachments;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

/**
 * Fermented-spider-eye inversions stay out of the book until the player has obtained
 * the potion being inverted. Follows {@link RecipeMystery#ENABLED}.
 */
public final class InversionUnlock {
    private InversionUnlock() {
    }

    public interface Gated {
        ResourceKey<Recipe<?>> id();

        Set<String> requiresKnown();
    }

    /** Effects on {@code bottle} when {@code ingredient} is the inverting eye. Otherwise none. */
    public static Set<String> of(ItemStack ingredient, ItemStack bottle) {
        if (!RecipeMystery.ENABLED || ingredient == null || !ingredient.is(Items.FERMENTED_SPIDER_EYE)) {
            return Set.of();
        }
        Set<String> effects = RecipeMystery.effectIds(bottle);
        return effects.isEmpty() ? Set.of() : Set.copyOf(effects);
    }

    public static boolean allowed(Set<String> required, Set<String> known) {
        if (!RecipeMystery.ENABLED || required == null || required.isEmpty()) {
            return true;
        }
        return known.containsAll(required);
    }

    public static void revoke(ServerPlayer player, List<? extends Gated> entries, RecipeManager recipes) {
        if (!RecipeMystery.ENABLED) {
            return;
        }
        Set<String> known = player.getData(ModAttachments.KNOWN_EFFECTS);
        List<RecipeHolder<?>> remove = new ArrayList<>();
        for (Gated entry : entries) {
            if (allowed(entry.requiresKnown(), known) || !player.getRecipeBook().contains(entry.id())) {
                continue;
            }
            recipes.byKey(entry.id()).ifPresent(remove::add);
        }
        if (!remove.isEmpty()) {
            player.getRecipeBook().removeRecipes(remove, player);
        }
    }
}
