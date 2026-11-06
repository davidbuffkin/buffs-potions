package com.buffspotions.menu;

import com.buffspotions.belt.BeltContents;
import com.buffspotions.belt.BeltEnchantments;
import com.buffspotions.equipment.BeltSlotAccess;
import com.buffspotions.item.PotionBeltItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BeltItemContainer implements Container {
    public static final int MAIN_HAND = 0;
    public static final int OFF_HAND = 1;
    public static final int EQUIPPED = 2;
    public static final int EXTERNAL = 3;
    public static final int INVENTORY = 100;

    private final Player player;
    private final int source;
    private final Container external;
    private final int externalSlot;
    private final ItemStack mirror;

    public BeltItemContainer(Player player, int source) {
        this(player, source, null, 0, ItemStack.EMPTY);
    }

    /** Client view of a belt opened from a chest or other container. */
    public BeltItemContainer(Player player, ItemStack mirror) {
        this(player, EXTERNAL, null, 0, mirror.copy());
    }

    public BeltItemContainer(Player player, Container external, int externalSlot) {
        this(player, EXTERNAL, external, externalSlot, ItemStack.EMPTY);
    }

    public static void writeOpenData(RegistryFriendlyByteBuf buffer, ItemStack belt) {
        buffer.writeVarInt(EXTERNAL);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer, belt);
    }

    public static BeltItemContainer fromNetwork(Player player, RegistryFriendlyByteBuf data) {
        int source = data.readVarInt();
        if (source == EXTERNAL) {
            return new BeltItemContainer(player, ItemStack.OPTIONAL_STREAM_CODEC.decode(data));
        }
        return new BeltItemContainer(player, source);
    }

    private BeltItemContainer(Player player, int source, Container external, int externalSlot, ItemStack mirror) {
        this.player = player;
        this.source = source;
        this.external = external;
        this.externalSlot = externalSlot;
        this.mirror = mirror;
    }

    public static int inventorySource(int inventorySlot) {
        return INVENTORY + inventorySlot;
    }

    public ItemStack belt() {
        if (external != null) {
            return external.getItem(externalSlot);
        }
        if (source == EXTERNAL) {
            return mirror;
        }
        if (source >= INVENTORY) {
            int index = source - INVENTORY;
            if (index < 0 || index >= player.getInventory().getContainerSize()) {
                return ItemStack.EMPTY;
            }
            return player.getInventory().getItem(index);
        }
        return switch (source) {
            case MAIN_HAND -> player.getMainHandItem();
            case OFF_HAND -> player.getOffhandItem();
            case EQUIPPED -> BeltSlotAccess.getEquipped(player);
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public int getContainerSize() {
        return BeltContents.BASE_SLOTS + BeltContents.bottleSlotCount(belt());
    }

    @Override
    public boolean isEmpty() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (isBottleSlot(slot)) {
            return BeltContents.bottle(belt(), slot - BeltContents.BASE_SLOTS);
        }
        return BeltContents.get(belt(), storedSlot(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (isBottleSlot(slot)) {
            ItemStack existing = getItem(slot).copy();
            if (existing.isEmpty() || amount <= 0) {
                return ItemStack.EMPTY;
            }
            ItemStack removed = existing.split(amount);
            setItem(slot, existing);
            return removed;
        }
        ItemStack existing = getItem(slot);
        if (existing.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = existing.copyWithCount(1);
        setItem(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (isBottleSlot(slot)) {
            BeltContents.setBottle(belt(), slot - BeltContents.BASE_SLOTS, stack);
        } else {
            BeltContents.set(belt(), storedSlot(slot), stack);
        }
        setChanged();
    }

    private boolean isBottleSlot(int slot) {
        return slot >= BeltContents.BASE_SLOTS && slot < getContainerSize();
    }

    private int storedSlot(int slot) {
        return BeltEnchantments.rowStart(belt()) + slot;
    }

    @Override
    public void setChanged() {
        if (external != null) {
            external.setChanged();
            return;
        }
        if (source == EXTERNAL) {
            return;
        }
        if (source == EQUIPPED) {
            BeltSlotAccess.setEquipped(player, belt());
        }
        player.getInventory().setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.player != player) {
            return false;
        }
        if (source == EXTERNAL && external == null) {
            return true;
        }
        return belt().getItem() instanceof PotionBeltItem;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (isBottleSlot(slot)) {
            return stack.is(Items.GLASS_BOTTLE);
        }
        return BeltContents.accepts(player, belt(), stack);
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < getContainerSize(); slot++) {
            setItem(slot, ItemStack.EMPTY);
        }
    }
}
