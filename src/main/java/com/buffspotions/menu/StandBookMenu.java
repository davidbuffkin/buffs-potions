package com.buffspotions.menu;

import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.registry.ModMenus;
import com.buffspotions.stand.StandBrewRecipe;
import com.buffspotions.stand.StandRecipeDisplay;
import com.buffspotions.stand.StandSubjects;
import com.buffspotions.still.StillStacks;
import java.util.Optional;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.event.EventHooks;

/**
 * The vanilla brewing stand slots, plus recipe-book placement.
 * Three bottle slots are filled left to right with as many matching bottles as the player has.
 */
public final class StandBookMenu extends RecipeBookMenu {
    private static final Identifier EMPTY_SLOT_FUEL = Identifier.withDefaultNamespace("container/slot/brewing_fuel");
    private static final Identifier EMPTY_SLOT_POTION = Identifier.withDefaultNamespace("container/slot/potion");
    private static final int BOTTLE_SLOTS = 3;
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;

    private final Container brewingStand;
    private final ContainerData brewingStandData;

    public StandBookMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf ignored) {
        this(containerId, inventory, new SimpleContainer(5), new SimpleContainerData(2));
    }

    public StandBookMenu(int containerId, Inventory inventory, Container brewingStand, ContainerData brewingStandData) {
        super(ModMenus.BREWING_STAND_BOOK.get(), containerId);
        checkContainerSize(brewingStand, 5);
        checkContainerDataCount(brewingStandData, 2);
        this.brewingStand = brewingStand;
        this.brewingStandData = brewingStandData;
        PotionBrewing brewing = inventory.player.level().potionBrewing();
        addSlot(new PotionSlot(brewing, brewingStand, 0, 56, 51));
        addSlot(new PotionSlot(brewing, brewingStand, 1, 79, 58));
        addSlot(new PotionSlot(brewing, brewingStand, 2, 102, 51));
        addSlot(new IngredientSlot(brewing, brewingStand, INGREDIENT_SLOT, 79, 17));
        addSlot(new FuelSlot(brewingStand, FUEL_SLOT, 17, 17));
        addDataSlots(brewingStandData);
        addStandardInventorySlots(inventory, 8, 84);
    }

    public int getFuel() {
        return brewingStandData.get(1);
    }

    public int getBrewingTicks() {
        return brewingStandData.get(0);
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents stackedContents) {
        for (int slot = 0; slot < 5; slot++) {
            stackedContents.accountStack(brewingStand.getItem(slot));
        }
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return com.buffspotions.registry.ModRecipes.BREWING_STAND_BOOK;
    }

    @Override
    public PostPlaceAction handlePlacement(
            boolean useMaxItems,
            boolean allowDroppingItemsToClear,
            RecipeHolder<?> recipe,
            ServerLevel level,
            Inventory inventory
    ) {
        if (!(recipe.value() instanceof StandBrewRecipe brew) || com.buffspotions.stack.StackRecipeCatalog.owns(brew)) {
            return PostPlaceAction.NOTHING;
        }
        boolean missing = false;
        for (int slot = 0; slot < BOTTLE_SLOTS; slot++) {
            missing |= placeBottle(inventory, slot, brew);
        }
        missing |= placeOne(inventory, INGREDIENT_SLOT, brew.ingredient(), false);
        if (enoughPlaced()) {
            return PostPlaceAction.NOTHING;
        }
        return missing ? PostPlaceAction.PLACE_GHOST_RECIPE : PostPlaceAction.NOTHING;
    }

    /**
     * Fills the stand for one subject of a parameterized template.
     * Sends the ghost itself, because the vanilla ghost uses the template's sample subject.
     */
    public void placeSubject(ServerPlayer player, StandBrewRecipe brew, Item subject) {
        if (com.buffspotions.stack.StackRecipeCatalog.owns(brew)
                || brew.subjectKind() == StandSubjects.Kind.NONE
                || !StandSubjects.accepts(brew.subjectKind(), subject)
                || !StandSubjects.known(player, subject)) {
            return;
        }
        StandRecipeDisplay display = standDisplay(brew);
        if (display == null) {
            return;
        }
        ItemStack bottle = StandSubjects.retarget(StandSubjects.resolve(display.bottle(), player.level()), subject);
        ItemStack result = StandSubjects.retarget(StandSubjects.resolve(display.result(), player.level()), subject);
        if (bottle.isEmpty()) {
            return;
        }
        Inventory inventory = player.getInventory();
        boolean missing = false;
        for (int slot = 0; slot < BOTTLE_SLOTS; slot++) {
            missing |= placeWant(inventory, slot, bottle);
        }
        missing |= placeOne(inventory, INGREDIENT_SLOT, brew.ingredient(), false);
        if (!missing || enoughPlaced()) {
            return;
        }
        SlotDisplay resultDisplay = result.isEmpty()
                ? display.result()
                : new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(result.copyWithCount(1)));
        StandRecipeDisplay ghost = new StandRecipeDisplay(
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(bottle.copyWithCount(1))),
                display.ingredient(),
                resultDisplay,
                display.craftingStation(),
                false,
                StandSubjects.Kind.NONE.ordinal()
        );
        player.connection.send(new ClientboundPlaceGhostRecipePacket(containerId, ghost));
    }

    private static StandRecipeDisplay standDisplay(StandBrewRecipe brew) {
        for (RecipeDisplay display : brew.display()) {
            if (display instanceof StandRecipeDisplay stand) {
                return stand;
            }
        }
        return null;
    }

    /** One bottle and the ingredient are enough. Empty bottle slots stay empty, without a ghost. */
    private boolean enoughPlaced() {
        boolean bottle = false;
        for (int slot = 0; slot < BOTTLE_SLOTS; slot++) {
            if (!getSlot(slot).getItem().isEmpty()) {
                bottle = true;
                break;
            }
        }
        return bottle && !getSlot(INGREDIENT_SLOT).getItem().isEmpty();
    }

    /** @return true when this bottle slot is still empty */
    private boolean placeBottle(Inventory inventory, int index, StandBrewRecipe brew) {
        if (brew.anyPlayerEssence()) {
            return placeOne(inventory, index, null, true);
        }
        return placeOne(inventory, index, brew.bottle(), false);
    }

    /**
     * @return true when the slot is still empty because the player does not have the item
     */
    private boolean placeOne(Inventory inventory, int index, Ingredient ingredient, boolean anyPlayerEssence) {
        Slot slot = getSlot(index);
        ItemStack current = slot.getItem();
        if (!current.isEmpty() && matches(current, ingredient, anyPlayerEssence)) {
            return false;
        }
        int found = findMatching(inventory, ingredient, anyPlayerEssence);
        ItemStack taken = found < 0 ? ItemStack.EMPTY : inventory.removeItem(found, 1);
        if (!current.isEmpty()) {
            ItemStack returning = current.copy();
            slot.set(ItemStack.EMPTY);
            inventory.placeItemBackInInventory(returning, false);
        }
        if (taken.isEmpty()) {
            return true;
        }
        slot.set(taken);
        return false;
    }

    /** @return true when the slot is still empty because the player does not have this bottle */
    private boolean placeWant(Inventory inventory, int index, ItemStack want) {
        Slot slot = getSlot(index);
        ItemStack current = slot.getItem();
        if (!current.isEmpty() && StillStacks.satisfies(current, want)) {
            return false;
        }
        int found = -1;
        for (int inventoryIndex = 0; inventoryIndex < inventory.getContainerSize(); inventoryIndex++) {
            ItemStack stack = inventory.getItem(inventoryIndex);
            if (!stack.isEmpty() && StillStacks.satisfies(stack, want) && Inventory.isUsableForCrafting(stack)) {
                found = inventoryIndex;
                break;
            }
        }
        ItemStack taken = found < 0 ? ItemStack.EMPTY : inventory.removeItem(found, 1);
        if (!current.isEmpty()) {
            ItemStack returning = current.copy();
            slot.set(ItemStack.EMPTY);
            inventory.placeItemBackInInventory(returning, false);
        }
        if (taken.isEmpty()) {
            return true;
        }
        slot.set(taken);
        return false;
    }

    private static int findMatching(Inventory inventory, Ingredient ingredient, boolean anyPlayerEssence) {
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            ItemStack stack = inventory.getItem(index);
            if (!stack.isEmpty() && matches(stack, ingredient, anyPlayerEssence) && Inventory.isUsableForCrafting(stack)) {
                return index;
            }
        }
        return -1;
    }

    private static boolean matches(ItemStack stack, Ingredient ingredient, boolean anyPlayerEssence) {
        if (anyPlayerEssence) {
            return EyedBrewing.isPlayerEssence(stack);
        }
        return ingredient != null && ingredient.test(stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return brewingStand.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack clicked = stack.copy();
        PotionBrewing brewing = player.level().potionBrewing();
        if (slotIndex >= 0 && slotIndex <= 4) {
            if (!moveItemStackTo(stack, 5, 41, true)) {
                return ItemStack.EMPTY;
            }
            slot.onQuickCraft(stack, clicked);
        } else if (stack.getItem() instanceof PotionItem && brewing.isInput(stack)) {
            if (!moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }
        } else if (stack.is(ItemTags.BREWING_FUEL)) {
            if (!moveItemStackTo(stack, 4, 5, false)) {
                return ItemStack.EMPTY;
            }
        } else if (this.slots.get(INGREDIENT_SLOT).mayPlace(stack)) {
            if (!moveItemStackTo(stack, 3, 4, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= 5 && slotIndex < 32) {
            if (!moveItemStackTo(stack, 32, 41, false)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= 32 && slotIndex < 41) {
            if (!moveItemStackTo(stack, 5, 32, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 5, 41, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == clicked.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, clicked);
        return clicked;
    }

    private static final class PotionSlot extends Slot {
        private final PotionBrewing potionBrewing;

        private PotionSlot(PotionBrewing potionBrewing, Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.potionBrewing = potionBrewing;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return potionBrewing.isInput(stack) || stack.is(Items.GLASS_BOTTLE);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void onTake(Player player, ItemStack carried) {
            Optional<Holder<Potion>> potion = carried.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
            if (potion.isPresent() && player instanceof ServerPlayer serverPlayer) {
                EventHooks.onPlayerBrewedPotion(player, carried);
                CriteriaTriggers.BREWED_POTION.trigger(serverPlayer, potion.get());
            }
            super.onTake(player, carried);
        }

        @Override
        public Identifier getNoItemIcon() {
            return EMPTY_SLOT_POTION;
        }
    }

    private static final class IngredientSlot extends Slot {
        private final PotionBrewing potionBrewing;

        private IngredientSlot(PotionBrewing potionBrewing, Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.potionBrewing = potionBrewing;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return potionBrewing.isIngredient(stack);
        }
    }

    private static final class FuelSlot extends Slot {
        private FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(ItemTags.BREWING_FUEL);
        }

        @Override
        public Identifier getNoItemIcon() {
            return EMPTY_SLOT_FUEL;
        }
    }
}
