package com.buffspotions.client;

import com.buffspotions.menu.BrewingStillMenu;
import com.buffspotions.mixin.client.RecipeCollectionAccess;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.still.BrewingStillBlockEntity;
import com.buffspotions.still.StillRecipeDisplay;
import com.buffspotions.still.StillSearchCategory;
import com.buffspotions.still.StillStacks;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.GhostSlots;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

public final class StillRecipeBookComponent extends RecipeBookComponent<BrewingStillMenu> {
    private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled"),
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled"),
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled_highlighted"),
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled_highlighted")
    );
    private static final Component FILTER_NAME = Component.translatable("gui.buffs_potions.recipebook.distillable");

    public StillRecipeBookComponent(BrewingStillMenu menu) {
        super(menu, List.of(
                new TabInfo(new ItemStack(Items.COMPASS), Optional.empty(), StillSearchCategory.STILL),
                new TabInfo(
                        PotionContents.createItemStack(Items.POTION, Potions.HEALING),
                        Optional.empty(),
                        ModRecipes.MUNDANE.get()
                ),
                new TabInfo(
                        PotionContents.createItemStack(Items.POTION, Potions.HARMING),
                        Optional.empty(),
                        ModRecipes.HARMFUL.get()
                ),
                new TabInfo(
                        Remedies.create(ModEffects.GREEN_THUMB, Items.SPLASH_POTION),
                        Optional.empty(),
                        ModRecipes.OTHER.get()
                )
        ));
    }

    @Override
    protected WidgetSprites getFilterButtonTextures() {
        return FILTER_SPRITES;
    }

    @Override
    protected boolean isCraftingSlot(Slot slot) {
        return slot.index <= BrewingStillBlockEntity.SLOT_FUEL;
    }

    @Override
    protected void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay recipe, ContextMap context) {
        if (!(recipe instanceof StillRecipeDisplay still)) {
            return;
        }
        ghostIfMissing(ghostSlots, BrewingStillBlockEntity.SLOT_BOTTLE, still.bottle(), context);
        ghostIfMissing(ghostSlots, BrewingStillBlockEntity.SLOT_INGREDIENT, still.ingredient(), context);
        Slot fuel = this.menu.getSlot(BrewingStillBlockEntity.SLOT_FUEL);
        if (fuel.getItem().isEmpty() && !playerHas(still.fuel().resolveForStacks(context))) {
            ghostSlots.setInput(fuel, context, still.fuel());
        }
    }

    private void ghostIfMissing(GhostSlots ghostSlots, int index, SlotDisplay display, ContextMap context) {
        List<ItemStack> options = display.resolveForStacks(context);
        Slot slot = this.menu.getSlot(index);
        if (matches(slot.getItem(), options) || playerHas(options)) {
            return;
        }
        ghostSlots.setInput(slot, context, display);
    }

    private boolean playerHas(List<ItemStack> options) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        Inventory inventory = this.minecraft.player.getInventory();
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (matches(stack, options)) {
                return true;
            }
        }
        return matches(inventory.getItem(Inventory.SLOT_OFFHAND), options);
    }

    private static boolean matches(ItemStack stack, List<ItemStack> options) {
        for (ItemStack option : options) {
            if (StillStacks.satisfies(stack, option)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Component getRecipeFilterName() {
        return FILTER_NAME;
    }

    @Override
    protected void selectMatchingRecipes(RecipeCollection collection, StackedItemContents stackedContents) {
        collection.selectRecipes(stackedContents, display -> display instanceof StillRecipeDisplay);
        correctCraftable(collection);
    }

    /**
     * Item counts treat every potion as the same bottle. A still recipe is highlighted only when
     * the player has both the bottle and the ingredient.
     */
    private void correctCraftable(RecipeCollection collection) {
        List<RecipeDisplayEntry> recipes = collection.getRecipes();
        if (recipes.isEmpty() || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        boolean still = false;
        for (RecipeDisplayEntry entry : recipes) {
            if (entry.display() instanceof StillRecipeDisplay) {
                still = true;
                break;
            }
        }
        if (!still) {
            return;
        }
        ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
        RecipeCollectionAccess access = (RecipeCollectionAccess) (Object) collection;
        Set<RecipeDisplayId> selected = access.buffsPotions$selected();
        Set<RecipeDisplayId> craftable = access.buffsPotions$craftable();
        for (RecipeDisplayEntry entry : recipes) {
            if (!(entry.display() instanceof StillRecipeDisplay display) || !selected.contains(entry.id())) {
                continue;
            }
            if (essenceHidden(display, context)) {
                selected.remove(entry.id());
                craftable.remove(entry.id());
                continue;
            }
            if (hasStack(display.bottle().resolveForStacks(context))
                    && hasStack(display.ingredient().resolveForStacks(context))) {
                craftable.add(entry.id());
            } else {
                craftable.remove(entry.id());
            }
        }
    }

    /** The essence cycle is visible only after a latent eyedentification potion has been carried. */
    private boolean essenceHidden(StillRecipeDisplay display, ContextMap context) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        if (this.minecraft.player.getData(ModAttachments.HELD_LATENT)) {
            return false;
        }
        for (ItemStack stack : display.result().resolveForStacks(context)) {
            if (EyedBrewing.isEssence(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasStack(List<ItemStack> options) {
        for (Slot slot : this.menu.slots) {
            if (matches(slot.getItem(), options)) {
                return true;
            }
        }
        return playerHas(options);
    }
}
