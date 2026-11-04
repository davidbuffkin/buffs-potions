package com.buffspotions.equipment;

import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.registry.ModAttachments;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class FallbackBeltContainer implements Container {
    private final Player player;

    public FallbackBeltContainer(Player player) {
        this.player = player;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return getItem(0).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot == 0 ? player.getData(ModAttachments.BELT_SLOT).stack() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack existing = getItem(0);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = existing.copyWithCount(1);
        setItem(0, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return removeItem(slot, 1);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot != 0) {
            return;
        }
        ItemStack previous = getItem(0);
        ItemStack equipped = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.matches(previous, equipped)) {
            return;
        }
        player.getData(ModAttachments.BELT_SLOT).set(equipped);
        setChanged();
        if (!player.level().isClientSide()
                && !equipped.isEmpty()
                && !ItemStack.isSameItem(previous, equipped)) {
            player.level().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
    }

    @Override
    public void setChanged() {
        player.syncData(ModAttachments.BELT_SLOT);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.player == player && player.isAlive();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof PotionBeltItem;
    }

    @Override
    public void clearContent() {
        setItem(0, ItemStack.EMPTY);
    }
}
