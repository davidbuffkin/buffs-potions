package com.buffspotions.mixin.client;

import com.buffspotions.client.RecipeMysteryView;
import com.buffspotions.recipebook.RecipeMystery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Search matches the text the button is allowed to show.
 * An unlearned effect matches "New Potion?" and its ingredient, not the effect name.
 */
@Mixin(RecipeBookComponent.class)
abstract class RecipeBookComponentMixin {
    @Shadow
    private EditBox searchBox;

    @Shadow
    private ClientRecipeBook book;

    @Shadow
    private RecipeBookPage recipeBookPage;

    @Shadow
    private RecipeBookTabButton selectedTab;

    @Inject(method = "updateCollections", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$visibleSearch(boolean resetPage, boolean isFiltering, CallbackInfo callback) {
        if (!RecipeMystery.ENABLED || this.searchBox == null || this.selectedTab == null || this.minecraft.player == null || this.minecraft.level == null) {
            return;
        }
        String query = this.searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return;
        }
        callback.cancel();
        List<RecipeCollection> collection = new ArrayList<>(this.book.getCollection(this.selectedTab.getCategory()));
        collection.removeIf(entry -> !entry.hasAnySelected());
        ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
        collection.removeIf(entry -> RecipeMysteryView.omit(entry, this.minecraft.player, context)
                || !RecipeMysteryView.matches(entry, this.minecraft.player, context, query));
        if (isFiltering) {
            collection.removeIf(entry -> !entry.hasCraftable());
        }
        this.recipeBookPage.updateCollections(collection, resetPage, isFiltering);
    }

    @Shadow
    protected net.minecraft.client.Minecraft minecraft;
}
