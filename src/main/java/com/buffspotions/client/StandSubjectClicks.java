package com.buffspotions.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.jspecify.annotations.Nullable;

/** The subject shown on the stand recipe button at the moment it was clicked. */
public final class StandSubjectClicks {
    private static @Nullable RecipeDisplayId armedId;
    private static @Nullable Item armedItem;

    private StandSubjectClicks() {
    }

    public static void arm(RecipeDisplayId id, Item item) {
        armedId = id;
        armedItem = item;
    }

    public static void clear() {
        armedId = null;
        armedItem = null;
    }

    public static @Nullable Item take(RecipeDisplayId id) {
        Item item = armedId != null && armedId.equals(id) ? armedItem : null;
        clear();
        return item;
    }
}
