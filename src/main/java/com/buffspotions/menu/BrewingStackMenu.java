package com.buffspotions.menu;

import com.buffspotions.BuffsPotions;
import com.buffspotions.advancement.BrewedPotionGrant;
import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModMenus;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.stack.BrewingStackBlockEntity;
import com.buffspotions.stack.StackBrewing;
import com.buffspotions.stack.StackFlagons;
import com.buffspotions.stack.StackRecipeCatalog;
import com.buffspotions.stand.StandBrewRecipe;
import com.buffspotions.stand.StandRecipeDisplay;
import com.buffspotions.stand.StandSubjects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import org.jspecify.annotations.Nullable;

public final class BrewingStackMenu extends RecipeBookMenu {
    public static final int QUEUE_Y = 17;
    public static final int QUEUE_X2 = 13;
    public static final int QUEUE_X1 = 35;
    public static final int QUEUE_X0 = 57;
    public static final int INGREDIENT_X = 79;
    public static final int INGREDIENT_Y = 17;
    public static final int FLAGON_X = 79;
    public static final int FLAGON_Y = 53;
    public static final int FUEL_X = 116;
    public static final int FUEL_Y = 53;
    public static final int BUBBLE_X = 118;
    public static final int BUBBLE_Y = 20;
    public static final int PROGRESS_X = 97;
    public static final int PROGRESS_Y = 16;

    private final Container stack;
    private final ContainerData data;
    private final Player player;

    public BrewingStackMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf ignored) {
        this(containerId, inventory);
    }

    public BrewingStackMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(BrewingStackBlockEntity.SLOT_COUNT), new SimpleContainerData(1));
    }

    public BrewingStackMenu(int containerId, Inventory inventory, Container stack, ContainerData data) {
        super(ModMenus.BREWING_STACK.get(), containerId);
        this.stack = stack;
        this.data = data;
        this.player = inventory.player;
        checkContainerSize(stack, BrewingStackBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, 1);
        addSlot(new FlagonSlot(stack, BrewingStackBlockEntity.SLOT_FLAGON, FLAGON_X, FLAGON_Y));
        addSlot(new IngredientSlot(stack, BrewingStackBlockEntity.SLOT_INGREDIENT, INGREDIENT_X, INGREDIENT_Y, player));
        addSlot(new IngredientSlot(stack, BrewingStackBlockEntity.SLOT_QUEUE_0, QUEUE_X0, QUEUE_Y, player));
        addSlot(new IngredientSlot(stack, BrewingStackBlockEntity.SLOT_QUEUE_1, QUEUE_X1, QUEUE_Y, player));
        addSlot(new IngredientSlot(stack, BrewingStackBlockEntity.SLOT_QUEUE_2, QUEUE_X2, QUEUE_Y, player));
        addSlot(new FuelSlot(stack, BrewingStackBlockEntity.SLOT_FUEL, FUEL_X, FUEL_Y));
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public int brewTime() {
        return data.get(0);
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents stackedContents) {
        stackedContents.accountStack(stack.getItem(BrewingStackBlockEntity.SLOT_FLAGON));
        stackedContents.accountStack(stack.getItem(BrewingStackBlockEntity.SLOT_INGREDIENT));
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return ModRecipes.BREWING_STACK_BOOK;
    }

    @Override
    public PostPlaceAction handlePlacement(
            boolean useMaxItems,
            boolean allowDroppingItemsToClear,
            RecipeHolder<?> recipe,
            ServerLevel level,
            Inventory inventory
    ) {
        if (!(recipe.value() instanceof StandBrewRecipe brew) || !StackRecipeCatalog.owns(brew)) {
            return PostPlaceAction.NOTHING;
        }
        StandRecipeDisplay display = display(brew);
        if (display == null) {
            return PostPlaceAction.NOTHING;
        }
        ItemStack wanted = StandSubjects.resolve(display.bottle(), level);
        boolean missing = placeFlagon(inventory, level, wanted, brew.anyPlayerEssence());
        missing |= placeOne(inventory, BrewingStackBlockEntity.SLOT_INGREDIENT, brew.ingredient());
        missing |= placeFuel(inventory);
        if (enoughPlaced() && hasFuel()) {
            return PostPlaceAction.NOTHING;
        }
        return missing ? PostPlaceAction.PLACE_GHOST_RECIPE : PostPlaceAction.NOTHING;
    }

    /** Fills the flagon slot for one subject of a parameterized template. */
    public void placeSubject(ServerPlayer player, StandBrewRecipe brew, Item subject) {
        if (!StackRecipeCatalog.owns(brew)
                || brew.subjectKind() == StandSubjects.Kind.NONE
                || !StandSubjects.accepts(brew.subjectKind(), subject)
                || !StandSubjects.known(player, subject)) {
            return;
        }
        StandRecipeDisplay display = display(brew);
        if (display == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        ItemStack bottle = StandSubjects.retarget(StandSubjects.resolve(display.bottle(), level), subject);
        ItemStack result = StandSubjects.retarget(StandSubjects.resolve(display.result(), level), subject);
        if (bottle.isEmpty()) {
            return;
        }
        boolean missing = placeFlagon(player.getInventory(), level, bottle, false);
        missing |= placeOne(player.getInventory(), BrewingStackBlockEntity.SLOT_INGREDIENT, brew.ingredient());
        missing |= placeFuel(player.getInventory());
        if (!missing || (enoughPlaced() && hasFuel())) {
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

    private static StandRecipeDisplay display(StandBrewRecipe brew) {
        for (RecipeDisplay shown : brew.display()) {
            if (shown instanceof StandRecipeDisplay stand) {
                return stand;
            }
        }
        return null;
    }

    /** The flagon and the ingredient are enough. Queue slots stay empty. */
    private boolean enoughPlaced() {
        return !getSlot(BrewingStackBlockEntity.SLOT_FLAGON).getItem().isEmpty()
                && !getSlot(BrewingStackBlockEntity.SLOT_INGREDIENT).getItem().isEmpty();
    }

    private boolean hasFuel() {
        return !getSlot(BrewingStackBlockEntity.SLOT_FUEL).getItem().isEmpty();
    }

    /** Moves one popped chorus fruit into an empty fuel slot. @return true when that slot stays empty */
    private boolean placeFuel(Inventory inventory) {
        if (hasFuel()) {
            return false;
        }
        return placeOne(inventory, BrewingStackBlockEntity.SLOT_FUEL, Ingredient.of(Items.POPPED_CHORUS_FRUIT));
    }

    /** @return true when the flagon slot is still empty */
    private boolean placeFlagon(Inventory inventory, ServerLevel level, ItemStack wanted, boolean anyPlayerEssence) {
        Slot slot = getSlot(BrewingStackBlockEntity.SLOT_FLAGON);
        ItemStack current = slot.getItem();
        FlagonContents currentContents = live(level, current);
        boolean currentMatches = matches(currentContents, wanted, anyPlayerEssence);
        int found = findFullest(inventory, level, wanted, anyPlayerEssence);
        int foundDoses = found < 0 ? 0 : live(level, inventory.getItem(found)).doses();
        if (currentMatches && currentContents.doses() >= foundDoses) {
            return false;
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

    /** @return true when the ingredient slot is still empty */
    private boolean placeOne(Inventory inventory, int index, Ingredient ingredient) {
        Slot slot = getSlot(index);
        ItemStack current = slot.getItem();
        if (!current.isEmpty() && ingredient.test(current)) {
            return false;
        }
        int found = -1;
        for (int inventoryIndex = 0; inventoryIndex < inventory.getContainerSize(); inventoryIndex++) {
            ItemStack stack = inventory.getItem(inventoryIndex);
            if (!stack.isEmpty() && ingredient.test(stack) && Inventory.isUsableForCrafting(stack)) {
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

    private static int findFullest(Inventory inventory, ServerLevel level, ItemStack wanted, boolean anyPlayerEssence) {
        int best = -1;
        int bestDoses = 0;
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            ItemStack stack = inventory.getItem(index);
            if (stack.isEmpty() || !Inventory.isUsableForCrafting(stack)) {
                continue;
            }
            FlagonContents contents = live(level, stack);
            if (!matches(contents, wanted, anyPlayerEssence)) {
                continue;
            }
            if (contents.doses() > bestDoses) {
                best = index;
                bestDoses = contents.doses();
            }
        }
        return best;
    }

    private static boolean matches(FlagonContents contents, ItemStack wanted, boolean anyPlayerEssence) {
        if (anyPlayerEssence) {
            return StackFlagons.playerEssence(contents);
        }
        return StackFlagons.sameFluid(contents, wanted);
    }

    private static FlagonContents live(ServerLevel level, ItemStack stack) {
        if (!ModItems.fitsBrewingStack(stack)) {
            return FlagonContents.EMPTY;
        }
        return EnderFlagons.live(level, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return stack.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = slot.getItem();
        ItemStack original = existing.copy();
        PotionBrewing brewing = player.level().potionBrewing();
        boolean oneIngredient = false;
        if (index < BrewingStackBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(existing, BrewingStackBlockEntity.SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (ModItems.fitsBrewingStack(existing)) {
            if (!moveItemStackTo(existing, BrewingStackBlockEntity.SLOT_FLAGON, BrewingStackBlockEntity.SLOT_FLAGON + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (StackBrewing.isFuel(existing)) {
            if (!moveItemStackTo(existing, BrewingStackBlockEntity.SLOT_FUEL, BrewingStackBlockEntity.SLOT_FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (StackBrewing.isIngredient(brewing, existing)) {
            if (!moveOne(existing, BrewingStackBlockEntity.SLOT_INGREDIENT, BrewingStackBlockEntity.SLOT_QUEUE_2 + 1)) {
                return ItemStack.EMPTY;
            }
            oneIngredient = true;
        } else if (index < BrewingStackBlockEntity.SLOT_COUNT + 27) {
            if (!moveItemStackTo(existing, BrewingStackBlockEntity.SLOT_COUNT + 27, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(existing, BrewingStackBlockEntity.SLOT_COUNT, BrewingStackBlockEntity.SLOT_COUNT + 27, false)) {
            return ItemStack.EMPTY;
        }

        if (existing.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (existing.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (index == BrewingStackBlockEntity.SLOT_FLAGON) {
            BrewedPotionGrant.flagon(player, original);
        }
        slot.onTake(player, existing);
        // Shift-click repeats while the returned stack matches the source slot.
        // An ingredient move has to stop after one item, or it fills every queue slot.
        return oneIngredient ? ItemStack.EMPTY : original;
    }

    private boolean moveOne(ItemStack stack, int start, int end) {
        for (int index = start; index < end; index++) {
            Slot slot = slots.get(index);
            if (slot.hasItem() || !slot.mayPlace(stack)) {
                continue;
            }
            slot.setByPlayer(stack.copyWithCount(1));
            stack.shrink(1);
            return true;
        }
        return false;
    }

    private static final class FlagonSlot extends Slot {
        private FlagonSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !hasItem() && ModItems.fitsBrewingStack(stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public @Nullable Identifier getNoItemIcon() {
            return Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "container/slot/flagon");
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            BrewedPotionGrant.flagon(player, stack);
        }
    }

    private static final class IngredientSlot extends Slot {
        private final Player player;

        private IngredientSlot(Container container, int slot, int x, int y, Player player) {
            super(container, slot, x, y);
            this.player = player;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return StackBrewing.isIngredient(player.level().potionBrewing(), stack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class FuelSlot extends Slot {
        private FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return StackBrewing.isFuel(stack);
        }

        @Override
        public int getMaxStackSize() {
            return BrewingStackBlockEntity.FUEL_MAX;
        }

        @Override
        public @Nullable Identifier getNoItemIcon() {
            return Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "container/slot/popped_chorus_fruit");
        }
    }
}
