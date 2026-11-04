package com.buffspotions.client;

import com.buffspotions.menu.StandBookMenu;
import com.buffspotions.mixin.client.RecipeCollectionAccess;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.stand.StandRecipeDisplay;
import com.buffspotions.stand.StandSearchCategory;
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

public final class StandRecipeBookComponent extends RecipeBookComponent<StandBookMenu> {
    private static final WidgetSprites FILTER_SPRITES = new WidgetSprites(
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled"),
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled"),
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_enabled_highlighted"),
            Identifier.withDefaultNamespace("recipe_book/furnace_filter_disabled_highlighted")
    );
    private static final Component FILTER_NAME = Component.translatable("gui.buffs_potions.recipebook.brewable");

    public StandRecipeBookComponent(StandBookMenu menu) {
        super(menu, List.of(
                new TabInfo(new ItemStack(Items.COMPASS), Optional.empty(), StandSearchCategory.STAND),
                new TabInfo(
                        PotionContents.createItemStack(Items.POTION, Potions.HEALING),
                        Optional.empty(),
                        ModRecipes.STAND_HELPFUL.get()
                ),
                new TabInfo(
                        PotionContents.createItemStack(Items.POTION, Potions.HARMING),
                        Optional.empty(),
                        ModRecipes.STAND_HARMFUL.get()
                ),
                new TabInfo(
                        com.buffspotions.potion.EyedBrewing.latentBottle(),
                        Optional.empty(),
                        ModRecipes.STAND_PARAMETER.get()
                ),
                new TabInfo(
                        PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WATER),
                        Optional.empty(),
                        ModRecipes.STAND_UTILITY.get()
                )
        ));
    }

    @Override
    protected WidgetSprites getFilterButtonTextures() {
        return FILTER_SPRITES;
    }

    @Override
    protected boolean isCraftingSlot(Slot slot) {
        return slot.index <= 4;
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
        List<ItemStack> bottles = stand.bottle().resolveForStacks(context);
        int owned = ownedBottles(bottles, stand.anyPlayerEssence());
        boolean ingredient = matches(this.menu.getSlot(3).getItem(), stand.ingredient().resolveForStacks(context))
                || playerHas(stand.ingredient().resolveForStacks(context));
        if (owned >= 1 && ingredient) {
            return;
        }
        if (owned < 3) {
            for (int slot = 0; slot < 3; slot++) {
                ItemStack current = this.menu.getSlot(slot).getItem();
                if (matchesBottle(current, bottles, stand.anyPlayerEssence())) {
                    continue;
                }
                ghostSlots.setInput(this.menu.getSlot(slot), context, stand.bottle());
            }
        }
        ghostIfMissing(ghostSlots, 3, stand.ingredient(), context);
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
     * A stand recipe is highlighted when the player has the ingredient and at least one bottle.
     * Blaze powder is ignored. Three bottles are ideal when the recipe is placed, not required to show green.
     */
    private void correctCraftable(RecipeCollection collection) {
        List<RecipeDisplayEntry> recipes = collection.getRecipes();
        if (recipes.isEmpty() || this.minecraft == null || this.minecraft.level == null) {
            return;
        }
        boolean stand = false;
        for (RecipeDisplayEntry entry : recipes) {
            if (entry.display() instanceof StandRecipeDisplay) {
                stand = true;
                break;
            }
        }
        if (!stand) {
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
            boolean bottle = display.anyPlayerEssence()
                    ? hasPlayerEssence()
                    : hasSubjectBottle(display, context);
            if (bottle && hasStack(display.ingredient().resolveForStacks(context))) {
                craftable.add(entry.id());
            } else {
                craftable.remove(entry.id());
            }
        }
    }

    private int ownedBottles(List<ItemStack> options, boolean anyPlayer) {
        int count = 0;
        if (this.minecraft == null || this.minecraft.player == null) {
            return 0;
        }
        for (int slot = 0; slot < 3; slot++) {
            ItemStack stack = this.menu.getSlot(slot).getItem();
            if (matchesBottle(stack, options, anyPlayer)) {
                count += stack.getCount();
            }
        }
        Inventory inventory = this.minecraft.player.getInventory();
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (matchesBottle(stack, options, anyPlayer)) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = inventory.getItem(Inventory.SLOT_OFFHAND);
        if (matchesBottle(offhand, options, anyPlayer)) {
            count += offhand.getCount();
        }
        return count;
    }

    private boolean hasPlayerEssence() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        for (Slot slot : this.menu.slots) {
            if (EyedBrewing.isPlayerEssence(slot.getItem())) {
                return true;
            }
        }
        Inventory inventory = this.minecraft.player.getInventory();
        for (ItemStack stack : inventory.getNonEquipmentItems()) {
            if (EyedBrewing.isPlayerEssence(stack)) {
                return true;
            }
        }
        return EyedBrewing.isPlayerEssence(inventory.getItem(Inventory.SLOT_OFFHAND));
    }

    private boolean hasSubjectBottle(StandRecipeDisplay display, ContextMap context) {
        if (display.subjects() == StandSubjects.Kind.NONE) {
            return hasStack(display.bottle().resolveForStacks(context));
        }
        if (this.minecraft == null || this.minecraft.player == null) {
            return false;
        }
        ItemStack sample = StandSubjects.resolve(display.bottle(), context);
        if (sample.isEmpty()) {
            return false;
        }
        for (Item subject : StandSubjects.matching(this.minecraft.player, display.subjects())) {
            if (hasStack(List.of(StandSubjects.retarget(sample, subject)))) {
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

    private static boolean matchesBottle(ItemStack stack, List<ItemStack> options, boolean anyPlayer) {
        if (anyPlayer) {
            return EyedBrewing.isPlayerEssence(stack);
        }
        return matches(stack, options);
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
