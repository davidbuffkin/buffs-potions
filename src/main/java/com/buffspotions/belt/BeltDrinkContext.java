package com.buffspotions.belt;

import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;

public final class BeltDrinkContext {
    private static final ThreadLocal<ItemStack> SOURCE = new ThreadLocal<>();

    public static <T> T finish(ItemStack belt, Supplier<T> action) {
        ItemStack previous = SOURCE.get();
        SOURCE.set(belt);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                SOURCE.remove();
            } else {
                SOURCE.set(previous);
            }
        }
    }

    public static ItemStack explicitSource() {
        ItemStack source = SOURCE.get();
        return source == null ? ItemStack.EMPTY : source;
    }

    private BeltDrinkContext() {
    }
}
