package com.buffspotions.client;

import com.buffspotions.menu.BrewingStackMenu;
import com.buffspotions.mixin.client.RecipeCollectionAccess;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.stack.BrewingStackBlockEntity;
import com.buffspotions.stack.StackFlagons;
import com.buffspotions.stack.StackSearchCategory;
import com.buffspotions.stand.StandRecipeDisplay;
import com.buffspotions.stand.StandSubjects;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

public final class StackRecipeBookComponent extends RecipeBookComponent<BrewingStackMenu> {
    private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled"),
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled"),
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled_highlighted"),
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled_highlighted")
    );
    private static final Component FILTER_NAME = Component.translatable("gui.buffs_potions.recipebook.brewable");

    public StackRecipeBookComponent(BrewingStackMenu menu) {
        super(menu, List.of(
                new TabInfo(new ItemStack(Items.COMPASS), Optional.empty(), StackSearchCategory.STACK),
                new TabInfo(StackFlagons.filled(PotionContents.createItemStack(Items.POTION, Potions.HEALING)), Optional.empty(), ModRecipes.STACK_HELPFUL.get()),
                new TabInfo(StackFlagons.filled(PotionContents.createItemStack(Items.POTION, Potions.HARMING)), Optional.empty(), ModRecipes.STACK_HARMFUL.get()),
                new TabInfo(StackFlagons.filled(EyedBrewing.latentBottle()), Optional.empty(), ModRecipes.STACK_PARAMETER.get()),
                new TabInfo(StackFlagons.filled(PotionContents.createItemStack(Items.POTION, Potions.WATER)), Optional.empty(), ModRecipes.STACK_UTILITY.get())
        ));
    }

    @Override
    protected WidgetSprites getFilterButtonTextures() {
        return FILTER_SPRITES;
    }

    @Override
    protected boolean isCraftingSlot(Slot slot) {
        return slot.index <= 1 || slot.index == BrewingStackBlockEntity.SLOT_FUEL;
    }

    @Override
    protected Component getRecipeFilterName() {
        return FILTER_NAME;
    }

    @Override
    protected void fillGhostRecipe(GhostSlots ghostSlots, RecipeDisplay recipe, ContextMap context) {
        if (!(recipe instanceof StandRecipeDisplay stand)) {
            return;
        }
        List<ItemStack> flagons = stand.bottle().resolveForStacks(context);
        boolean owned = ownedFlagon(flagons, stand.anyPlayerEssence());
        boolean ingredient = matches(this.menu.getSlot(1).getItem(), stand.ingredient().resolveForStacks(context))
                || playerHas(stand.ingredient().resolveForStacks(context));
        if (!(owned && ingredient)) {
            if (!owned) {
                ItemStack current = this.menu.getSlot(0).getItem();
                if (!matchesFlagon(current, flagons, stand.anyPlayerEssence())) {
                    ghostSlots.setInput(this.menu.getSlot(0), context, stand.bottle());
                }
            }
            ghostIfMissing(ghostSlots, 1, stand.ingredient(), context);
        }
        Slot fuel = this.menu.getSlot(BrewingStackBlockEntity.SLOT_FUEL);
        if (fuel.getItem().isEmpty() && !playerHas(List.of(new ItemStack(Items.POPPED_CHORUS_FRUIT)))) {
            ghostSlots.setInput(fuel, context, new SlotDisplay.ItemSlotDisplay(Items.POPPED_CHORUS_FRUIT));
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

    @Override
    protected void selectMatchingRecipes(RecipeCollection collection, StackedItemContents stackedContents) {
        collection.selectRecipes(stackedContents, display -> display instanceof StandRecipeDisplay);
        correctCraftable(collection);
    }

    /**
     * Green when the player has the ingredient and any flagon of that fluid, full or not.
     * Popped chorus fruit is ignored. The queue is never part of the recipe.
     */
    private void correctCraftable(RecipeCollection collection) {
        List<RecipeDisplayEntry> recipes = collection.getRecipes();
        if (recipes.isEmpty() || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        boolean stack = false;
        for (RecipeDisplayEntry entry : recipes) {
            if (entry.display() instanceof StandRecipeDisplay) {
                stack = true;
                break;
            }
        }
        if (!stack) {
            return;
        }
        ContextMap context = SlotDisplayContext.fromLevel(this.minecraft.level);
        RecipeCollectionAccess access = (RecipeCollectionAccess) (Object) collection;
        Set<RecipeDisplayId> selected = access.buffsPotions$selected();
        Set<RecipeDisplayId> craftable = access.buffsPotions$craftable();
        for (RecipeDisplayEntry entry : recipes) {
            if (!(entry.display() instanceof StandRecipeDisplay display) || !selected.contains(entry.id())) {
                continue;
            }
            if (display.subjects() != StandSubjects.Kind.NONE && this.minecraft.player != null
                    && StandSubjects.matching(this.minecraft.player, display.subjects()).isEmpty()) {
                selected.remove(entry.id());
                craftable.remove(entry.id());
                continue;
            }
            boolean flagon = display.anyPlayerEssence()
                    ? hasPlayerEssence()
                    : hasSubjectFlagon(display, context);
            if (flagon && hasStack(display.ingredient().resolveForStacks(context))) {
                craftable.add(entry.id());
            } else {
                craftable.remove(entry.id());
            }
        }
    }

    private boolean ownedFlagon(List<ItemStack> options, boolean anyPlayer) {
        if (matchesFlagon(this.menu.getSlot(0).getItem(), options, anyPlayer)) {
            return true;
        }
        return playerHasFlagon(options, anyPlayer);
    }

    private boolean hasPlayerEssence() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        for (Slot slot : this.menu.slots) {
            if (StackFlagons.playerEssence(slot.getItem())) {
                return true;
            }
        }
        return StackFlagons.playerEssence(this.minecraft.player.getOffhandItem());
    }

    private boolean hasSubjectFlagon(StandRecipeDisplay display, ContextMap context) {
        if (display.subjects() == StandSubjects.Kind.NONE) {
            return hasFlagon(display.bottle().resolveForStacks(context), false);
        }
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        ItemStack sample = StandSubjects.resolve(display.bottle(), context);
        if (sample.isEmpty()) {
            return false;
        }
        for (Item subject : StandSubjects.matching(this.minecraft.player, display.subjects())) {
            if (hasFlagon(List.of(StandSubjects.retarget(sample, subject)), false)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFlagon(List<ItemStack> options, boolean anyPlayer) {
        for (Slot slot : this.menu.slots) {
            if (matchesFlagon(slot.getItem(), options, anyPlayer)) {
                return true;
            }
        }
        return playerHasFlagon(options, anyPlayer);
    }

    private boolean hasStack(List<ItemStack> options) {
        for (Slot slot : this.menu.slots) {
            if (matches(slot.getItem(), options)) {
                return true;
            }
        }
        return playerHas(options);
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

    private boolean playerHasFlagon(List<ItemStack> options, boolean anyPlayer) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        Inventory inventory = this.minecraft.player.getInventory();
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (matchesFlagon(stack, options, anyPlayer)) {
                return true;
            }
        }
        return matchesFlagon(inventory.getItem(Inventory.SLOT_OFFHAND), options, anyPlayer);
    }

    private static boolean matchesFlagon(ItemStack stack, List<ItemStack> options, boolean anyPlayer) {
        if (anyPlayer) {
            return StackFlagons.playerEssence(stack);
        }
        for (ItemStack option : options) {
            if (StackFlagons.sameFluid(stack, option)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matches(ItemStack stack, List<ItemStack> options) {
        for (ItemStack option : options) {
            if (StillStacks.satisfies(stack, option)) {
                return true;
            }
        }
        return false;
    }
}
