package com.buffspotions.stack;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import net.minecraft.world.item.ItemStack;

/** A full flagon of a dose, and whether another flagon holds that same fluid. */
public final class StackFlagons {
    private StackFlagons() {
    }

    public static ItemStack filled(ItemStack dose) {
        if (dose == null || dose.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack flagon = new ItemStack(ModItems.FLAGON.get());
        flagon.set(ModDataComponents.FLAGON_CONTENTS.get(), new FlagonContents(dose.copyWithCount(1), FlagonContents.MAX_DOSES));
        return flagon;
    }

    public static boolean sameFluid(ItemStack have, ItemStack wantedFlagon) {
        if (!ModItems.fitsBrewingStack(have)) {
            return false;
        }
        return sameFluid(FlagonContents.of(have), wantedFlagon);
    }

    public static boolean sameFluid(FlagonContents have, ItemStack wantedFlagon) {
        if (have.isEmpty() || have.isHoney() || wantedFlagon == null || !ModItems.fitsBrewingStack(wantedFlagon)) {
            return false;
        }
        FlagonContents want = FlagonContents.of(wantedFlagon);
        return !want.isEmpty() && ItemStack.isSameItemSameComponents(have.dose(), want.dose());
    }

    public static boolean playerEssence(ItemStack stack) {
        return ModItems.fitsBrewingStack(stack) && playerEssence(FlagonContents.of(stack));
    }

    public static boolean playerEssence(FlagonContents contents) {
        return !contents.isEmpty() && EyedBrewing.isPlayerEssence(contents.dose());
    }
}
