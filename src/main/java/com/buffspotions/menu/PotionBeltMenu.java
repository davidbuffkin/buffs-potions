package com.buffspotions.menu;

import com.buffspotions.BuffsPotions;
import com.buffspotions.belt.BeltContents;
import com.buffspotions.item.FlagonBeltItem;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.registry.ModMenus;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PotionBeltMenu extends AbstractContainerMenu {
    private static final int DOSE_SLOTS = BeltContents.BASE_SLOTS;
    private static final int BOTTLE_SHIFT = 18;
    private final BeltItemContainer belt;
    private final int bottleSlots;

    public PotionBeltMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf data) {
        this(containerId, inventory, BeltItemContainer.fromNetwork(inventory.player, data));
    }

    public PotionBeltMenu(int containerId, Inventory inventory, int source) {
        this(containerId, inventory, new BeltItemContainer(inventory.player, source));
    }

    public PotionBeltMenu(int containerId, Inventory inventory, Container container, int slot) {
        this(containerId, inventory, new BeltItemContainer(inventory.player, container, slot));
    }

    private PotionBeltMenu(int containerId, Inventory inventory, BeltItemContainer belt) {
        super(ModMenus.POTION_BELT.get(), containerId);
        this.belt = belt;
        this.bottleSlots = BeltContents.bottleSlotCount(belt.belt());
        belt.startOpen(inventory.player);
        int inventoryY = 51 + (bottleSlots > 0 ? BOTTLE_SHIFT : 0);
        int hotbarY = 109 + (bottleSlots > 0 ? BOTTLE_SHIFT : 0);

        for (int column = 0; column < DOSE_SLOTS; column++) {
            addSlot(new Slot(belt, column, 8 + column * 18, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return BeltContents.accepts(inventory.player, belt.belt(), stack);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }

                @Override
                public Identifier getNoItemIcon() {
                    if (belt.belt().getItem() instanceof FlagonBeltItem) {
                        return Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "container/slot/flagon");
                    }
                    return Identifier.withDefaultNamespace("container/slot/potion");
                }
            });
        }

        for (int column = 0; column < bottleSlots; column++) {
            addSlot(new Slot(belt, DOSE_SLOTS + column, 62 + column * 18, 38) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(Items.GLASS_BOTTLE);
                }

                @Override
                public int getMaxStackSize() {
                    return 64;
                }

                @Override
                public Identifier getNoItemIcon() {
                    return Identifier.withDefaultNamespace("container/slot/potion");
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, hotbarY));
        }
    }

    public boolean hasBottleRow() {
        return bottleSlots > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = slot.getItem();
        ItemStack original = existing.copy();
        int contents = DOSE_SLOTS + bottleSlots;
        boolean bottleSlot = index >= DOSE_SLOTS && index < contents;
        if (bottleSlot) {
            // Bottle slots store stacks inside an item component, so getItem() is a copy.
            // Shift-click must write the leftover count back or the stack is lost.
            ItemStack moving = existing.copy();
            if (!moveItemStackTo(moving, contents, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.setByPlayer(moving.isEmpty() ? ItemStack.EMPTY : moving);
            return original;
        }
        if (index < contents) {
            if (!moveItemStackTo(existing, contents, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (bottleSlots > 0 && existing.is(Items.GLASS_BOTTLE)) {
            if (BeltContents.insertFirst(belt.belt(), existing, DOSE_SLOTS, player) < 0) {
                return ItemStack.EMPTY;
            }
        } else if (BeltContents.accepts(player, belt.belt(), existing)) {
            if (!moveItemStackTo(existing, 0, DOSE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!player.level().isClientSide()
                    && existing.getItem() instanceof FlagonItem
                    && belt.belt().getItem() instanceof FlagonBeltItem throwing
                    && throwing.throwsPotions(belt.belt())) {
                player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
            }
            return ItemStack.EMPTY;
        }

        if (existing.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return belt.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        belt.stopOpen(player);
    }
}
