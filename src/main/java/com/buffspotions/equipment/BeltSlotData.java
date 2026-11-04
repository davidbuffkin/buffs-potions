package com.buffspotions.equipment;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public final class BeltSlotData {
    public static final MapCodec<BeltSlotData> CODEC = ItemStack.OPTIONAL_CODEC
            .fieldOf("belt")
            .xmap(BeltSlotData::new, BeltSlotData::stack);
    public static final StreamCodec<RegistryFriendlyByteBuf, BeltSlotData> STREAM_CODEC =
            ItemStack.OPTIONAL_STREAM_CODEC.map(BeltSlotData::new, BeltSlotData::stack);

    private ItemStack stack;

    public BeltSlotData() {
        this(ItemStack.EMPTY);
    }

    public BeltSlotData(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack stack() {
        return stack;
    }

    public void set(ItemStack stack) {
        this.stack = stack;
    }
}
