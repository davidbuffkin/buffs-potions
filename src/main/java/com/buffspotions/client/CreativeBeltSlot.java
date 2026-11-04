package com.buffspotions.client;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Inventory-menu belt slot placed beside the creative offhand slot. */
public final class CreativeBeltSlot extends Slot {
    private final Slot delegate;

    public CreativeBeltSlot(Slot delegate) {
        super(delegate.container, delegate.getSlotIndex(), 17, 20);
        this.delegate = delegate;
    }

    @Override
    public ItemStack getItem() {
        return delegate.getItem();
    }

    @Override
    public boolean hasItem() {
        return delegate.hasItem();
    }

    @Override
    public void set(ItemStack stack) {
        delegate.set(stack);
    }

    @Override
    public void setByPlayer(ItemStack stack, ItemStack previous) {
        delegate.setByPlayer(stack, previous);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return delegate.mayPlace(stack);
    }

    @Override
    public boolean mayPickup(Player player) {
        return delegate.mayPickup(player);
    }

    @Override
    public ItemStack remove(int amount) {
        return delegate.remove(amount);
    }

    @Override
    public @Nullable Identifier getNoItemIcon() {
        return delegate.getNoItemIcon();
    }

    @Override
    public int getMaxStackSize() {
        return delegate.getMaxStackSize();
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return delegate.getMaxStackSize(stack);
    }

    @Override
    public boolean isActive() {
        return delegate.isActive();
    }
}
