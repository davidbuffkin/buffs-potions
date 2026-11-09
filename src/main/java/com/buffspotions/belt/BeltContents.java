package com.buffspotions.belt;

import com.buffspotions.effect.Formation;
import com.buffspotions.effect.Incarnation;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.FlagonBeltItem;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

public final class BeltContents {
    public static final int BASE_SLOTS = 9;
    public static final int MAX_SLOTS = 18;
    public static final int BOTTLE_SLOTS = 3;
    public static final ItemContainerContents EMPTY_FIXED = ItemContainerContents.EMPTY;

    public static ItemContainerContents get(ItemStack belt) {
        if (belt.isEmpty()) {
            return EMPTY_FIXED;
        }
        return belt.getOrDefault(ModDataComponents.BELT_CONTENTS.get(), EMPTY_FIXED);
    }

    public static ItemStack get(ItemStack belt, int slot) {
        if (slot < 0 || slot >= MAX_SLOTS) {
            return ItemStack.EMPTY;
        }
        ItemContainerContents contents = get(belt);
        return slot < contents.getSlots() ? contents.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    public static void set(ItemStack belt, int slot, ItemStack value) {
        if (slot < 0 || slot >= MAX_SLOTS) {
            throw new IndexOutOfBoundsException("Belt slot " + slot);
        }
        NonNullList<ItemStack> items = copyToList(belt);
        items.set(slot, value.isEmpty() ? ItemStack.EMPTY : value.copyWithCount(1));
        belt.set(ModDataComponents.BELT_CONTENTS.get(), ItemContainerContents.fromItems(items));
    }

    public static boolean isAcceptedDose(ItemStack stack) {
        return isPotionDose(stack);
    }

    public static boolean accepts(ItemStack belt, ItemStack stack) {
        return accepts(null, belt, stack);
    }

    public static boolean accepts(@Nullable Player player, ItemStack belt, ItemStack stack) {
        if (stack.isEmpty() || stack.getItem() instanceof PotionBeltItem) {
            return false;
        }
        if (belt.getItem() instanceof FlagonBeltItem flagonBelt) {
            if (!(stack.getItem() instanceof FlagonItem flagon)) {
                return false;
            }
            if (!flagonBelt.throwsPotions(belt)) {
                return true;
            }
            FlagonContents contents = player == null
                    ? FlagonContents.of(stack)
                    : flagon.playerContents(player, stack);
            return allowsContents(belt, contents);
        }
        return isPotionDose(stack) || stack.is(Items.GLASS_BOTTLE);
    }

    /** Empty flagons are allowed. Honey is not, while the belt is throwing. Rain, sun, incarnation, and formation can be stored. */
    public static boolean allowsContents(ItemStack belt, FlagonContents contents) {
        if (!(belt.getItem() instanceof FlagonBeltItem flagonBelt) || !flagonBelt.throwsPotions(belt)) {
            return true;
        }
        if (contents.isEmpty()) {
            return true;
        }
        if (contents.isHoney()) {
            return false;
        }
        if (contents.isMilk()) {
            return true;
        }
        ItemStack dose = contents.dose();
        if (dose.get(DataComponents.POTION_CONTENTS) == null) {
            return false;
        }
        if (flagonBelt.hasThrowMode()
                && (BeltConsumptionService.isWeatherCalling(dose)
                || Incarnation.isIncarnation(dose)
                || Formation.isFormation(dose))) {
            return true;
        }
        return BeltConsumptionService.canThrow(dose, flagonBelt.lingers(belt));
    }

    public static int bottleSlotCount(ItemStack belt) {
        return belt.getItem() instanceof FlagonBeltItem flagonBelt && flagonBelt.throwsPotions(belt) ? BOTTLE_SLOTS : 0;
    }

    public static ItemStack bottle(ItemStack belt, int slot) {
        if (slot < 0 || slot >= BOTTLE_SLOTS) {
            return ItemStack.EMPTY;
        }
        ItemContainerContents bottles = bottles(belt);
        return slot < bottles.getSlots() ? bottles.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    public static void setBottle(ItemStack belt, int slot, ItemStack value) {
        if (slot < 0 || slot >= BOTTLE_SLOTS || bottleSlotCount(belt) == 0) {
            return;
        }
        NonNullList<ItemStack> items = copyBottles(belt);
        ItemStack stored = ItemStack.EMPTY;
        if (value.is(Items.GLASS_BOTTLE)) {
            stored = value.copy();
            stored.setCount(Math.min(stored.getCount(), stored.getMaxStackSize()));
        }
        items.set(slot, stored);
        belt.set(ModDataComponents.BELT_BOTTLES.get(), ItemContainerContents.fromItems(items));
    }

    public static boolean hasBottle(ItemStack belt) {
        for (int slot = 0; slot < bottleSlotCount(belt); slot++) {
            if (bottle(belt, slot).is(Items.GLASS_BOTTLE) && bottle(belt, slot).getCount() > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean takeBottle(ItemStack belt) {
        for (int slot = 0; slot < bottleSlotCount(belt); slot++) {
            ItemStack stored = bottle(belt, slot);
            if (stored.is(Items.GLASS_BOTTLE) && stored.getCount() > 0) {
                ItemStack left = stored.copy();
                left.shrink(1);
                setBottle(belt, slot, left);
                return true;
            }
        }
        return false;
    }

    /** Removes one glass bottle. The last bottle in the belt breaks out loud. */
    public static void consumeBottle(ServerPlayer player, ItemStack belt) {
        boolean last = bottleCount(belt) == 1;
        if (takeBottle(belt) && last) {
            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.GLASS_BREAK,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }

    public static int bottleCount(ItemStack belt) {
        int count = 0;
        for (int slot = 0; slot < bottleSlotCount(belt); slot++) {
            ItemStack stored = bottle(belt, slot);
            if (stored.is(Items.GLASS_BOTTLE)) {
                count += stored.getCount();
            }
        }
        return count;
    }

    private static boolean isPotionDose(ItemStack stack) {
        return !stack.isEmpty()
                && !(stack.getItem() instanceof PotionBeltItem)
                && (stack.getItem() instanceof PotionItem
                || stack.is(Items.HONEY_BOTTLE));
    }

    public static boolean isThrowableDose(ItemStack stack) {
        return stack.getItem() instanceof ThrowablePotionItem;
    }

    public static boolean isDrinkableDose(ItemStack stack) {
        return isAcceptedDose(stack) && !isThrowableDose(stack);
    }

    public static boolean isDrinkableDose(Player player, ItemStack belt, ItemStack stack) {
        return isDrinkableDose(player, belt, stack, false);
    }

    /** Gulp drinks whatever a flagon belt can drink, including while the belt is set to splash or linger. */
    public static boolean isDrinkableDose(Player player, ItemStack belt, ItemStack stack, boolean ignoreMode) {
        if (isThrowableDose(stack)) {
            return false;
        }
        if (!ignoreMode && !accepts(belt, stack)) {
            return false;
        }
        if (stack.getItem() instanceof FlagonItem flagon) {
            return flagon.hasDrinkableDose(player, stack);
        }
        return isDrinkableDose(stack);
    }

    public static boolean isConsumableCell(ItemStack stack) {
        return isAcceptedDose(stack);
    }

    public static boolean isConsumableCell(ItemStack belt, ItemStack stack) {
        if (belt.getItem() instanceof FlagonBeltItem) {
            return stack.getItem() instanceof FlagonItem;
        }
        return isDrinkableDose(stack) || isThrowableDose(stack);
    }

    public static boolean isResidue(ItemStack stack) {
        return stack.is(Items.GLASS_BOTTLE);
    }

    public static int activeSlot(ItemStack belt) {
        return Math.clamp(belt.getOrDefault(ModDataComponents.ACTIVE_BELT_SLOT.get(), 0), 0, MAX_SLOTS - 1);
    }

    public static void setActiveSlot(ItemStack belt, int slot) {
        belt.set(ModDataComponents.ACTIVE_BELT_SLOT.get(), Math.clamp(slot, 0, MAX_SLOTS - 1));
    }

    public static int firstConsumable(ItemStack belt, int visibleSlots) {
        for (int slot = 0; slot < Math.min(visibleSlots, MAX_SLOTS); slot++) {
            if (isConsumableCell(belt, get(belt, slot))) {
                return slot;
            }
        }
        return -1;
    }

    public static int scrollConsumable(ItemStack belt, int direction, int visibleSlots) {
        int count = Math.min(visibleSlots, MAX_SLOTS);
        int current = Math.min(activeSlot(belt), count - 1);
        int step = direction < 0 ? -1 : 1;
        for (int offset = 1; offset <= count; offset++) {
            int candidate = Math.floorMod(current + step * offset, count);
            if (isConsumableCell(belt, get(belt, candidate))) {
                return candidate;
            }
        }
        return current;
    }

    public static int scrollInRow(ItemStack belt, int direction, int rowStart, int rowLength) {
        int local = BeltContents.activeSlot(belt) - rowStart;
        if (local < 0 || local >= rowLength) {
            local = 0;
        }
        int step = direction < 0 ? -1 : 1;
        for (int offset = 1; offset <= rowLength; offset++) {
            int candidate = Math.floorMod(local + step * offset, rowLength);
            if (isConsumableCell(belt, get(belt, rowStart + candidate))) {
                return rowStart + candidate;
            }
        }
        return rowStart + local;
    }

    public static int nextInRow(ItemStack belt, int afterSlot, int rowStart, int rowLength) {
        int localAfter = afterSlot - rowStart;
        if (localAfter < 0 || localAfter >= rowLength) {
            localAfter = -1;
        }
        for (int offset = 1; offset <= rowLength; offset++) {
            int local = Math.floorMod(localAfter + offset, rowLength);
            if (isConsumableCell(belt, get(belt, rowStart + local))) {
                return rowStart + local;
            }
        }
        return -1;
    }

    /**
     * The slot auto-advance lands on. Flagon belts skip empty flagons and only stop on one that still has a dose.
     */
    public static int nextDrinkableInRow(Player player, ItemStack belt, int afterSlot, int rowStart, int rowLength) {
        int localAfter = afterSlot - rowStart;
        if (localAfter < 0 || localAfter >= rowLength) {
            localAfter = -1;
        }
        for (int offset = 1; offset <= rowLength; offset++) {
            int local = Math.floorMod(localAfter + offset, rowLength);
            if (isAutoSelectable(player, belt, get(belt, rowStart + local))) {
                return rowStart + local;
            }
        }
        return -1;
    }

    private static boolean isAutoSelectable(Player player, ItemStack belt, ItemStack stack) {
        if (belt.getItem() instanceof FlagonBeltItem) {
            return stack.getItem() instanceof FlagonItem flagon && flagon.hasDrinkableDose(player, stack);
        }
        return isConsumableCell(belt, stack);
    }

    public static int nextSelectable(ItemStack belt, int afterSlot, int visibleSlots) {
        int count = Math.min(visibleSlots, MAX_SLOTS);
        if (count <= 0) {
            return -1;
        }
        for (int offset = 1; offset <= count; offset++) {
            int candidate = Math.floorMod(afterSlot + offset, count);
            if (isConsumableCell(belt, get(belt, candidate))) {
                return candidate;
            }
        }
        return -1;
    }

    public static int lastOccupied(ItemStack belt, int visibleSlots) {
        for (int slot = Math.min(visibleSlots, MAX_SLOTS) - 1; slot >= 0; slot--) {
            if (!get(belt, slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public static ItemStack removeLastOccupied(ItemStack belt, int visibleSlots) {
        int slot = lastOccupied(belt, visibleSlots);
        if (slot < 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = get(belt, slot).copy();
        set(belt, slot, ItemStack.EMPTY);
        return removed;
    }

    /**
     * Every empty bottle, or else every empty flagon, as one stack. A creeper or dragon belt
     * keeps bottles in the bottle row and flagons in the dose slots.
     */
    public static ItemStack takeEmptyStack(ItemStack belt, int visibleSlots) {
        ItemStack bottles = takeStoredBottles(belt);
        if (!bottles.isEmpty()) {
            return bottles;
        }
        if (belt.getItem() instanceof FlagonBeltItem) {
            return takeMatching(belt, visibleSlots, BeltContents::isEmptyStoredFlagon);
        }
        return takeMatching(belt, visibleSlots, stack -> stack.is(Items.GLASS_BOTTLE));
    }

    private static ItemStack takeStoredBottles(ItemStack belt) {
        ItemStack gathered = ItemStack.EMPTY;
        for (int slot = 0; slot < bottleSlotCount(belt); slot++) {
            ItemStack stored = bottle(belt, slot);
            if (!stored.is(Items.GLASS_BOTTLE) || stored.isEmpty()) {
                continue;
            }
            if (gathered.isEmpty()) {
                int moved = Math.min(stored.getCount(), stored.getMaxStackSize());
                gathered = stored.copyWithCount(moved);
                shrinkBottle(belt, slot, stored, moved);
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(gathered, stored)) {
                continue;
            }
            int room = gathered.getMaxStackSize() - gathered.getCount();
            if (room <= 0) {
                break;
            }
            int moved = Math.min(room, stored.getCount());
            gathered.grow(moved);
            shrinkBottle(belt, slot, stored, moved);
        }
        return gathered;
    }

    private static void shrinkBottle(ItemStack belt, int slot, ItemStack stored, int moved) {
        if (moved >= stored.getCount()) {
            setBottle(belt, slot, ItemStack.EMPTY);
            return;
        }
        ItemStack left = stored.copy();
        left.shrink(moved);
        setBottle(belt, slot, left);
    }

    private static boolean isEmptyStoredFlagon(ItemStack stack) {
        return stack.is(ModItems.FLAGON.get()) && FlagonContents.of(stack).isEmpty();
    }

    private static ItemStack takeMatching(ItemStack belt, int visibleSlots, Predicate<ItemStack> match) {
        ItemStack gathered = ItemStack.EMPTY;
        int count = Math.min(visibleSlots, MAX_SLOTS);
        for (int slot = 0; slot < count; slot++) {
            ItemStack stored = get(belt, slot);
            if (!match.test(stored)) {
                continue;
            }
            if (gathered.isEmpty()) {
                int moved = Math.min(stored.getCount(), stored.getMaxStackSize());
                gathered = stored.copyWithCount(moved);
                shrinkStored(belt, slot, stored, moved);
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(gathered, stored)) {
                continue;
            }
            int room = gathered.getMaxStackSize() - gathered.getCount();
            if (room <= 0) {
                break;
            }
            int moved = Math.min(room, stored.getCount());
            gathered.grow(moved);
            shrinkStored(belt, slot, stored, moved);
        }
        return gathered;
    }

    private static void shrinkStored(ItemStack belt, int slot, ItemStack stored, int moved) {
        if (moved >= stored.getCount()) {
            set(belt, slot, ItemStack.EMPTY);
            return;
        }
        ItemStack left = stored.copy();
        left.shrink(moved);
        set(belt, slot, left);
    }

    public static List<ItemStack> copyOccupied(ItemStack belt, int visibleSlots) {
        ArrayList<ItemStack> copies = new ArrayList<>();
        for (int slot = 0; slot < Math.min(visibleSlots, MAX_SLOTS); slot++) {
            ItemStack stored = get(belt, slot);
            if (!stored.isEmpty()) {
                copies.add(stored.copy());
            }
        }
        return copies;
    }

    public static int insertFirst(ItemStack belt, ItemStack dose, int visibleSlots) {
        return insertFirst(belt, dose, visibleSlots, null);
    }

    public static int insertFirst(ItemStack belt, ItemStack dose, int visibleSlots, @Nullable Player player) {
        if (dose.is(Items.GLASS_BOTTLE) && bottleSlotCount(belt) > 0) {
            return insertBottle(belt, dose);
        }
        if (!accepts(player, belt, dose)) {
            return -1;
        }
        for (int slot = 0; slot < Math.min(visibleSlots, MAX_SLOTS); slot++) {
            if (get(belt, slot).isEmpty()) {
                set(belt, slot, dose);
                dose.shrink(1);
                return slot;
            }
        }
        return -1;
    }

    /** Glass bottles fill the three bottle slots up to a full stack. Flagons stay one per dose slot. */
    private static int insertBottle(ItemStack belt, ItemStack incoming) {
        if (!incoming.is(Items.GLASS_BOTTLE) || incoming.isEmpty()) {
            return -1;
        }
        int first = -1;
        for (int slot = 0; slot < BOTTLE_SLOTS && !incoming.isEmpty(); slot++) {
            ItemStack stored = bottle(belt, slot);
            if (!stored.is(Items.GLASS_BOTTLE) || stored.getCount() >= stored.getMaxStackSize()) {
                continue;
            }
            int moved = Math.min(stored.getMaxStackSize() - stored.getCount(), incoming.getCount());
            ItemStack grown = stored.copy();
            grown.grow(moved);
            setBottle(belt, slot, grown);
            incoming.shrink(moved);
            if (first < 0) {
                first = slot;
            }
        }
        for (int slot = 0; slot < BOTTLE_SLOTS && !incoming.isEmpty(); slot++) {
            if (!bottle(belt, slot).isEmpty()) {
                continue;
            }
            int moved = Math.min(incoming.getMaxStackSize(), incoming.getCount());
            setBottle(belt, slot, incoming.copyWithCount(moved));
            incoming.shrink(moved);
            if (first < 0) {
                first = slot;
            }
        }
        return first;
    }

    private static ItemContainerContents bottles(ItemStack belt) {
        if (belt.isEmpty()) {
            return EMPTY_FIXED;
        }
        return belt.getOrDefault(ModDataComponents.BELT_BOTTLES.get(), EMPTY_FIXED);
    }

    private static NonNullList<ItemStack> copyBottles(ItemStack belt) {
        NonNullList<ItemStack> items = NonNullList.withSize(BOTTLE_SLOTS, ItemStack.EMPTY);
        ItemContainerContents contents = bottles(belt);
        for (int slot = 0; slot < Math.min(contents.getSlots(), BOTTLE_SLOTS); slot++) {
            items.set(slot, contents.getStackInSlot(slot).copy());
        }
        return items;
    }

    private static NonNullList<ItemStack> copyToList(ItemStack belt) {
        NonNullList<ItemStack> items = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
        ItemContainerContents contents = get(belt);
        for (int slot = 0; slot < Math.min(contents.getSlots(), MAX_SLOTS); slot++) {
            items.set(slot, contents.getStackInSlot(slot).copy());
        }
        return items;
    }

    private BeltContents() {
    }
}
