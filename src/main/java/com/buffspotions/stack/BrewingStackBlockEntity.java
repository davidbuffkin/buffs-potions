package com.buffspotions.stack;

import com.buffspotions.bellows.BellowsBoosts;
import com.buffspotions.bellows.BellowsTarget;
import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.menu.BrewingStackMenu;
import com.buffspotions.registry.ModBlockEntities;
import com.buffspotions.registry.ModDataComponents;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.event.EventHooks;
import org.jspecify.annotations.Nullable;

public final class BrewingStackBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer, BellowsTarget {
    public static final int SLOT_FLAGON = 0;
    public static final int SLOT_INGREDIENT = 1;
    public static final int SLOT_QUEUE_0 = 2;
    public static final int SLOT_QUEUE_1 = 3;
    public static final int SLOT_QUEUE_2 = 4;
    public static final int SLOT_FUEL = 5;
    public static final int FUEL_MAX = 64;
    public static final int SLOT_COUNT = 6;
    public static final int BREW_TIME = 400;

    private static final int[] SLOTS_DOWN = {SLOT_FLAGON, SLOT_INGREDIENT, SLOT_QUEUE_0, SLOT_QUEUE_1, SLOT_QUEUE_2};
    private static final int[] SLOTS_UP = {SLOT_INGREDIENT, SLOT_QUEUE_0, SLOT_QUEUE_1, SLOT_QUEUE_2};
    private static final int[] SLOTS_SIDE = {SLOT_FLAGON, SLOT_FUEL};
    private static final List<Integer> QUEUE = List.of(SLOT_QUEUE_0, SLOT_QUEUE_1, SLOT_QUEUE_2);

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
    int brewTime;
    private ItemStack brewingDose = ItemStack.EMPTY;
    private ItemStack brewingIngredient = ItemStack.EMPTY;
    private ItemStack syncedFlagon = ItemStack.EMPTY;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return index == 0 ? brewTime : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                brewTime = value;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    };

    public BrewingStackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BREWING_STACK.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BrewingStackBlockEntity stack) {
        stack.refreshAnchoredFlagon();
        stack.tickBrew(level.potionBrewing(), level);
        if (BellowsBoosts.isActive(level, pos)) {
            stack.tickBrew(level.potionBrewing(), level);
            if (level instanceof net.minecraft.server.level.ServerLevel server && server.getGameTime() % 10 == 0) {
                BellowsBoosts.trickle(server, pos);
            }
        }
    }

    @Override
    public boolean isWorking() {
        return brewTime > 0;
    }

    public boolean tickBrew(PotionBrewing brewing, @Nullable Level level) {
        int previous = brewTime;
        if (canBrew(brewing)) {
            if (brewTime > 0 && brewingDose.isEmpty()) {
                rememberMix();
            } else if (brewTime > 0 && !sameMix()) {
                brewTime = 0;
            }
            if (brewTime == 0) {
                rememberMix();
            }
            brewTime++;
            if (brewTime >= BREW_TIME) {
                brewTime = 0;
                finishBrew(brewing, level);
            }
        } else if (brewTime != 0 || !brewingDose.isEmpty()) {
            brewTime = 0;
            brewingDose = ItemStack.EMPTY;
            brewingIngredient = ItemStack.EMPTY;
        }
        if (brewTime != previous) {
            setChanged();
            if ((previous > 0) != (brewTime > 0) && level != null) {
                BlockState state = getBlockState();
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
        return brewTime > 0;
    }

    private void refreshAnchoredFlagon() {
        ItemStack flagon = items.get(SLOT_FLAGON);
        if (!(level instanceof ServerLevel server) || EnderFlagons.anchor(flagon) == null) {
            return;
        }
        FlagonContents live = EnderFlagons.live(server, flagon);
        if (!FlagonContents.of(flagon).sameFill(live)) {
            flagon.set(ModDataComponents.FLAGON_CONTENTS.get(), live);
            setChanged();
        }
    }

    private FlagonContents flagonContents() {
        ItemStack flagon = items.get(SLOT_FLAGON);
        if (level instanceof ServerLevel server) {
            return EnderFlagons.live(server, flagon);
        }
        return FlagonContents.of(flagon);
    }

    private void writeFlagon(ItemStack flagon, FlagonContents contents) {
        if (level instanceof ServerLevel server && EnderFlagons.anchor(flagon) != null) {
            EnderFlagons.store(server, flagon, contents);
            return;
        }
        flagon.set(ModDataComponents.FLAGON_CONTENTS.get(), contents);
    }

    private boolean canBrew(PotionBrewing brewing) {
        FlagonContents contents = flagonContents();
        ItemStack ingredient = items.get(SLOT_INGREDIENT);
        return !contents.isEmpty()
                && !contents.isHoney()
                && StackBrewing.isFuel(items.get(SLOT_FUEL))
                && !ingredient.isEmpty()
                && !StackBrewing.mix(brewing, contents.dose(), ingredient).isEmpty();
    }

    private boolean sameMix() {
        FlagonContents contents = flagonContents();
        return !contents.isEmpty()
                && ItemStack.isSameItemSameComponents(contents.dose(), brewingDose)
                && ItemStack.isSameItemSameComponents(items.get(SLOT_INGREDIENT), brewingIngredient);
    }

    private void rememberMix() {
        FlagonContents contents = flagonContents();
        ItemStack ingredient = items.get(SLOT_INGREDIENT);
        brewingDose = contents.dose().copy();
        brewingIngredient = ingredient.copyWithCount(1);
    }

    private void finishBrew(PotionBrewing brewing, @Nullable Level level) {
        ItemStack flagon = items.get(SLOT_FLAGON);
        FlagonContents contents = flagonContents();
        ItemStack ingredient = items.get(SLOT_INGREDIENT);
        NonNullList<ItemStack> stacks = NonNullList.withSize(5, ItemStack.EMPTY);
        stacks.set(0, contents.dose().copy());
        stacks.set(3, ingredient.copy());
        if (EventHooks.onPotionAttemptBrew(stacks)) {
            writeFlagon(flagon, new FlagonContents(stacks.get(0), contents.doses()));
            items.set(SLOT_INGREDIENT, stacks.get(3));
        } else {
            ItemStack brewed = StackBrewing.mix(brewing, stacks.get(0), stacks.get(3));
            if (!brewed.isEmpty()) {
                stacks.set(0, brewed);
                EventHooks.onPotionBrewed(stacks);
                writeFlagon(flagon, new FlagonContents(stacks.get(0), contents.doses()));
                spendIngredient(level);
                spendFuel(level);
                if (level != null) {
                    level.levelEvent(1035, worldPosition, 0);
                }
            }
        }
        brewingDose = ItemStack.EMPTY;
        brewingIngredient = ItemStack.EMPTY;
        setChanged();
    }

    private void spendIngredient(@Nullable Level level) {
        ItemStack ingredient = items.get(SLOT_INGREDIENT);
        ItemStackTemplate remainder = ingredient.getCraftingRemainder();
        ingredient.shrink(1);
        if (remainder != null) {
            if (ingredient.isEmpty()) {
                ingredient = remainder.create();
            } else if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder.create());
            }
        }
        items.set(SLOT_INGREDIENT, ingredient);
        if (ingredient.isEmpty()) {
            shiftQueue();
        }
    }

    private void shiftQueue() {
        if (!items.get(SLOT_INGREDIENT).isEmpty()) {
            return;
        }
        for (int slot : QUEUE) {
            ItemStack queued = items.get(slot);
            if (!queued.isEmpty()) {
                items.set(SLOT_INGREDIENT, queued);
                items.set(slot, ItemStack.EMPTY);
                compactQueue();
                return;
            }
        }
    }

    private void compactQueue() {
        int write = 0;
        ItemStack[] kept = new ItemStack[QUEUE.size()];
        for (int slot : QUEUE) {
            ItemStack queued = items.get(slot);
            if (!queued.isEmpty()) {
                kept[write++] = queued;
            }
        }
        for (int i = 0; i < QUEUE.size(); i++) {
            items.set(QUEUE.get(i), i < write ? kept[i] : ItemStack.EMPTY);
        }
    }

    private void spendFuel(@Nullable Level level) {
        ItemStack fuel = items.get(SLOT_FUEL);
        ItemStackTemplate remainder = fuel.getCraftingRemainder();
        fuel.shrink(1);
        if (remainder != null) {
            if (fuel.isEmpty()) {
                items.set(SLOT_FUEL, remainder.create());
            } else if (level != null) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), remainder.create());
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        ItemStack flagon = items.get(SLOT_FLAGON);
        if (ItemStack.matches(flagon, syncedFlagon) || level == null || level.isClientSide()) {
            return;
        }
        syncedFlagon = flagon.copy();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level != null) {
            Containers.dropContents(level, pos, this);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.buffs_potions.brewing_stack");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return StackBrewing.isFuel(stack) ? FUEL_MAX : 1;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new BrewingStackMenu(containerId, inventory, this, dataAccess);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        brewTime = input.getIntOr("BrewTime", 0);
        brewingDose = ItemStack.EMPTY;
        brewingIngredient = ItemStack.EMPTY;
        syncedFlagon = items.get(SLOT_FLAGON).copy();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("BrewTime", brewTime);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        ItemStack existing = getItem(slot);
        if (slot == SLOT_FUEL) {
            if (!StackBrewing.isFuel(stack)) {
                return false;
            }
            return existing.isEmpty()
                    || (existing.getCount() < FUEL_MAX && ItemStack.isSameItemSameComponents(existing, stack));
        }
        if (!existing.isEmpty()) {
            return false;
        }
        PotionBrewing brewing = level != null ? level.potionBrewing() : PotionBrewing.EMPTY;
        if (slot == SLOT_FLAGON) {
            return com.buffspotions.registry.ModItems.fitsBrewingStack(stack) && getItem(slot).isEmpty();
        }
        if (slot == SLOT_INGREDIENT || QUEUE.contains(slot)) {
            return StackBrewing.isIngredient(brewing, stack);
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.DOWN) {
            return SLOTS_DOWN;
        }
        if (direction == Direction.UP) {
            return SLOTS_UP;
        }
        return SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction direction) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        return slot != SLOT_INGREDIENT && !QUEUE.contains(slot);
    }
}
