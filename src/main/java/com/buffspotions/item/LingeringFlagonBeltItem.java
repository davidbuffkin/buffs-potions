package com.buffspotions.item;

import com.buffspotions.registry.ModDataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class LingeringFlagonBeltItem extends FlagonBeltItem {
    public LingeringFlagonBeltItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean throwsPotions() {
        return true;
    }

    @Override
    public boolean lingers() {
        return true;
    }

    @Override
    public int modeCount() {
        return 3;
    }

    @Override
    public int throwMode(ItemStack belt) {
        return Math.clamp(belt.getOrDefault(ModDataComponents.BELT_MODE.get(), 2), 0, 2);
    }
}
