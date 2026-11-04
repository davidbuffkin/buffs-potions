package com.buffspotions.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.buffspotions.registry.ModDataComponents;

public class SplashFlagonBeltItem extends FlagonBeltItem {
    public SplashFlagonBeltItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public boolean throwsPotions() {
        return true;
    }

    @Override
    public int modeCount() {
        return 2;
    }

    @Override
    public int throwMode(ItemStack belt) {
        int mode = belt.getOrDefault(ModDataComponents.BELT_MODE.get(), -1);
        if (mode >= 0) {
            return Math.clamp(mode, 0, 1);
        }
        return belt.getOrDefault(ModDataComponents.BELT_THROWING.get(), true) ? 1 : 0;
    }
}
