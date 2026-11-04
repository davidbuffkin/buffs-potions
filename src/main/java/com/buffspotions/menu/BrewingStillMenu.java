package com.buffspotions.menu;

import com.buffspotions.BuffsPotions;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModMenus;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.still.BrewingStillBlockEntity;
import com.buffspotions.still.StillBrewing;
import com.buffspotions.still.StillDistillRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
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
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.core.component.DataComponents;
import org.jspecify.annotations.Nullable;

public final class BrewingStillMenu extends RecipeBookMenu {
    public static final int INGREDIENT_X = 79;
    public static final int INGREDIENT_Y = 17;
    public static final int BOTTLE_X = 79;
    public static final int BOTTLE_Y = 53;
    public static final int FUEL_X = 56;
    public static final int FUEL_Y = 53;
    public static final int FLAME_X = 56;
    public static final int FLAME_Y = 36;
    public static final int PROGRESS_X = 97;
    public static final int PROGRESS_Y = 16;

    private final Container still;
    private final ContainerData data;

    public BrewingStillMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf ignored) {
        this(containerId, inventory);
    }

    public BrewingStillMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(BrewingStillBlockEntity.SLOT_COUNT), new SimpleContainerData(3));
    }

    public BrewingStillMenu(int containerId, Inventory inventory, Container still, ContainerData data) {
        super(ModMenus.BREWING_STILL.get(), containerId);
        this.still = still;
        this.data = data;
        checkContainerSize(still, BrewingStillBlockEntity.SLOT_COUNT);
        checkContainerDataCount(data, 3);
        addSlot(new BottleSlot(still, BrewingStillBlockEntity.SLOT_BOTTLE, BOTTLE_X, BOTTLE_Y));
        addSlot(new IngredientSlot(still, BrewingStillBlockEntity.SLOT_INGREDIENT, INGREDIENT_X, INGREDIENT_Y));
        addSlot(new FuelSlot(still, BrewingStillBlockEntity.SLOT_FUEL, FUEL_X, FUEL_Y));
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    public int brewTime() {
        return data.get(0);
    }

    public boolean isLit() {
        return data.get(1) > 0;
    }

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents stackedContents) {
        for (int slot = 0; slot < BrewingStillBlockEntity.SLOT_COUNT; slot++) {
            stackedContents.accountStack(still.getItem(slot));
        }
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return ModRecipes.BREWING_STILL;
    }

    @Override
    public PostPlaceAction handlePlacement(
            boolean useMaxItems,
            boolean allowDroppingItemsToClear,
            RecipeHolder<?> recipe,
            ServerLevel level,
            Inventory inventory
    ) {
        if (!(recipe.value() instanceof StillDistillRecipe distill)) {
            return PostPlaceAction.NOTHING;
        }
        boolean missing = placeOne(inventory, BrewingStillBlockEntity.SLOT_BOTTLE, distill.bottle());
        missing |= placeOne(inventory, BrewingStillBlockEntity.SLOT_INGREDIENT, distill.ingredient());
        Slot fuel = getSlot(BrewingStillBlockEntity.SLOT_FUEL);
        if (fuel.getItem().isEmpty()) {
            missing |= placeOne(inventory, BrewingStillBlockEntity.SLOT_FUEL, Ingredient.of(Items.CHARCOAL));
        }
        return missing ? PostPlaceAction.PLACE_GHOST_RECIPE : PostPlaceAction.NOTHING;
    }

    /** @return true when the slot is still empty because the player does not have the item */
    private boolean placeOne(Inventory inventory, int index, Ingredient ingredient) {
        Slot slot = getSlot(index);
        ItemStack current = slot.getItem();
        if (!current.isEmpty() && ingredient.test(current)) {
            return false;
        }
        int found = findMatching(inventory, ingredient);
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

    private static int findMatching(Inventory inventory, Ingredient ingredient) {
        for (int index = 0; index < inventory.getContainerSize(); index++) {
            ItemStack stack = inventory.getItem(index);
            if (!stack.isEmpty() && ingredient.test(stack) && Inventory.isUsableForCrafting(stack)) {
                return index;
            }
        }
        return -1;
    }

    public float litProgress() {
        int duration = data.get(2);
        if (duration == 0) {
            return 0.0F;
        }
        return data.get(1) / (float) duration;
    }

    @Override
    public boolean stillValid(Player player) {
        return still.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = slot.getItem();
        ItemStack original = existing.copy();
        if (index < BrewingStillBlockEntity.SLOT_COUNT) {
            if (!moveItemStackTo(existing, BrewingStillBlockEntity.SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (existing.getItem() instanceof PotionItem && StillBrewing.isBottle(existing)) {
            if (!moveItemStackTo(existing, BrewingStillBlockEntity.SLOT_BOTTLE, BrewingStillBlockEntity.SLOT_BOTTLE + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (existing.is(Items.CHARCOAL)) {
            if (!moveItemStackTo(existing, BrewingStillBlockEntity.SLOT_FUEL, BrewingStillBlockEntity.SLOT_FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (StillBrewing.isIngredient(existing)) {
            if (!moveItemStackTo(existing, BrewingStillBlockEntity.SLOT_INGREDIENT, BrewingStillBlockEntity.SLOT_INGREDIENT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < BrewingStillBlockEntity.SLOT_COUNT + 27) {
            if (!moveItemStackTo(existing, BrewingStillBlockEntity.SLOT_COUNT + 27, slots.size(), false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(existing, BrewingStillBlockEntity.SLOT_COUNT, BrewingStillBlockEntity.SLOT_COUNT + 27, false)) {
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
        if (index == BrewingStillBlockEntity.SLOT_BOTTLE) {
            grantTaken(player, original);
        }
        slot.onTake(player, existing);
        return original;
    }

    private static void grantTaken(Player player, ItemStack stack) {
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        if (MundaneBrewing.isMundane(stack)) {
            ModCriteria.MUNDANE_STILL.get().trigger(server);
            if (stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.LUCK)) {
                ModCriteria.MUNDANE_LUCK.get().trigger(server);
            }
        }
        if (Remedies.isRemedy(stack)) {
            ModCriteria.REMEDY_STILL.get().trigger(server);
        }
        if (EyedBrewing.isEssence(stack)) {
            ModCriteria.ESSENCE_STILL.get().trigger(server);
        }
        if (EyedBrewing.isPlayerEssence(stack)) {
            ModCriteria.PLAYER_ESSENCE.get().trigger(server);
        }
    }

    private static final class BottleSlot extends Slot {
        private BottleSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !hasItem() && (stack.is(Items.GLASS_BOTTLE) || StillBrewing.isBottle(stack));
        }

        @Override
        public @Nullable Identifier getNoItemIcon() {
            return Identifier.withDefaultNamespace("container/slot/potion");
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            super.onTake(player, stack);
            grantTaken(player, stack);
        }
    }

    private static final class IngredientSlot extends Slot {
        private IngredientSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return StillBrewing.isIngredient(stack);
        }
    }

    private static final class FuelSlot extends Slot {
        private FuelSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.is(Items.CHARCOAL);
        }

        @Override
        public @Nullable Identifier getNoItemIcon() {
            return Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "container/slot/charcoal");
        }
    }
}
