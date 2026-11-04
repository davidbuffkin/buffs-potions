package com.buffspotions.mixin.client;

import com.buffspotions.client.RecipeMysteryView;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.potion.SuspiciousBrew;
import com.buffspotions.recipebook.RecipeMystery;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.stand.StandRecipeDisplay;
import com.buffspotions.still.StillRecipeDisplay;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Puts a few grouped buttons in a stable place on the stand and still books. */
@Mixin(RecipeBookPage.class)
abstract class RecipeBookPageMixin {
    @Inject(method = "updateCollections", at = @At("HEAD"))
    private void buffsPotions$order(List<RecipeCollection> recipeCollections, boolean resetPage, boolean isFiltering, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        ContextMap context = SlotDisplayContext.fromLevel(minecraft.level);
        if (recipeCollections.size() < 2) {
            if (RecipeMystery.ENABLED && minecraft.player != null) {
                recipeCollections.removeIf(collection -> RecipeMysteryView.omit(collection, minecraft.player, context));
            }
            return;
        }
        if (onlyCategory(recipeCollections, ModRecipes.STAND_UTILITY.get())) {
            moveBefore(
                    recipeCollections,
                    collection -> ingredientIs(collection, Items.GLOWSTONE_DUST, context),
                    collection -> ingredientIs(collection, Items.REDSTONE, context) && !plainMundane(collection, context)
            );
            moveFirst(recipeCollections, collection -> plainMundane(collection, context));
        }
        if (onlyCategory(recipeCollections, ModRecipes.OTHER.get())) {
            moveAfter(recipeCollections, collection -> remedy(collection, context), collection -> suspicious(collection, context));
            moveFirst(recipeCollections, collection -> plainMundane(collection, context));
        }
        if (onlyCategory(recipeCollections, ModRecipes.STACK_UTILITY.get())) {
            recipeCollections.sort(Comparator.comparingInt(collection -> stackUtilityRank(collection, context)));
            moveFirst(recipeCollections, collection -> plainMundane(collection, context));
        }
        if (RecipeMystery.ENABLED && minecraft.player != null) {
            recipeCollections.removeIf(collection -> RecipeMysteryView.omit(collection, minecraft.player, context));
        }
    }

    private static boolean onlyCategory(List<RecipeCollection> collections, RecipeBookCategory category) {
        boolean any = false;
        for (RecipeCollection collection : collections) {
            for (RecipeDisplayEntry entry : collection.getRecipes()) {
                any = true;
                if (entry.category() != category) {
                    return false;
                }
            }
        }
        return any;
    }

    private static void moveFirst(List<RecipeCollection> collections, Predicate<RecipeCollection> moved) {
        int movedIndex = index(collections, moved);
        if (movedIndex <= 0) {
            return;
        }
        collections.add(0, collections.remove(movedIndex));
    }

    private static void moveBefore(List<RecipeCollection> collections, Predicate<RecipeCollection> moved, Predicate<RecipeCollection> anchor) {
        int movedIndex = index(collections, moved);
        int anchorIndex = index(collections, anchor);
        if (movedIndex < 0 || anchorIndex < 0 || movedIndex == anchorIndex - 1) {
            return;
        }
        RecipeCollection collection = collections.remove(movedIndex);
        collections.add(index(collections, anchor), collection);
    }

    private static void moveAfter(List<RecipeCollection> collections, Predicate<RecipeCollection> moved, Predicate<RecipeCollection> anchor) {
        int movedIndex = index(collections, moved);
        int anchorIndex = index(collections, anchor);
        if (movedIndex < 0 || anchorIndex < 0 || movedIndex == anchorIndex + 1) {
            return;
        }
        RecipeCollection collection = collections.remove(movedIndex);
        collections.add(index(collections, anchor) + 1, collection);
    }

    private static int index(List<RecipeCollection> collections, Predicate<RecipeCollection> predicate) {
        for (int index = 0; index < collections.size(); index++) {
            if (predicate.test(collections.get(index))) {
                return index;
            }
        }
        return -1;
    }

    private static int stackUtilityRank(RecipeCollection collection, ContextMap context) {
        boolean glowstone = ingredientIs(collection, Items.GLOWSTONE_DUST, context);
        boolean redstone = ingredientIs(collection, Items.REDSTONE, context);
        boolean torchflower = ingredientIs(collection, Items.TORCHFLOWER, context);
        boolean pitcher = ingredientIs(collection, Items.PITCHER_PLANT, context);
        boolean wart = ingredientIs(collection, Items.NETHER_WART, context);
        if (wart && !glowstone && !redstone && !torchflower && !pitcher) {
            return 0;
        }
        if (glowstone) {
            return 1;
        }
        if (redstone) {
            return 2;
        }
        if (torchflower && pitcher) {
            return 5;
        }
        if (torchflower) {
            return 3;
        }
        if (pitcher) {
            return 4;
        }
        return 6;
    }

    private static boolean ingredientIs(RecipeCollection collection, Item item, ContextMap context) {
        for (RecipeDisplayEntry entry : collection.getRecipes()) {
            if (!(entry.display() instanceof StandRecipeDisplay stand)) {
                continue;
            }
            for (ItemStack stack : stand.ingredient().resolveForStacks(context)) {
                if (stack.is(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean plainMundane(RecipeCollection collection, ContextMap context) {
        return everyShownResult(collection, context, MundaneBrewing::isPlain);
    }

    private static boolean suspicious(RecipeCollection collection, ContextMap context) {
        return everyResult(collection, context, stack -> SuspiciousBrew.is(stack) && stack.is(Items.POTION));
    }

    private static boolean remedy(RecipeCollection collection, ContextMap context) {
        return everyResult(collection, context, stack -> stack.is(Items.POTION) && stack.has(ModDataComponents.REMEDY.get()));
    }

    private static boolean everyResult(RecipeCollection collection, ContextMap context, Predicate<ItemStack> result) {
        return everyShownResult(collection, context, result);
    }

    private static boolean everyShownResult(RecipeCollection collection, ContextMap context, Predicate<ItemStack> result) {
        boolean any = false;
        for (RecipeDisplayEntry entry : collection.getRecipes()) {
            boolean matched = false;
            for (ItemStack stack : shownResults(entry, context)) {
                if (result.test(stack)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                return false;
            }
            any = true;
        }
        return any;
    }

    private static List<ItemStack> shownResults(RecipeDisplayEntry entry, ContextMap context) {
        if (entry.display() instanceof StillRecipeDisplay still) {
            return still.result().resolveForStacks(context);
        }
        if (entry.display() instanceof StandRecipeDisplay stand) {
            return stand.result().resolveForStacks(context);
        }
        return List.of();
    }
}
