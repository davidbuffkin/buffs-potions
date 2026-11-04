package com.buffspotions.still;

import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModDataComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;

/** Whether an inventory stack can fill a still recipe ghost. */
public final class StillStacks {
    private StillStacks() {
    }

    public static boolean satisfies(ItemStack have, ItemStack want) {
        if (have.isEmpty() || want.isEmpty() || !have.is(want.getItem())) {
            return false;
        }
        PotionContents wanted = want.get(DataComponents.POTION_CONTENTS);
        if (wanted != null) {
            PotionContents held = have.get(DataComponents.POTION_CONTENTS);
            if (held == null || !held.potion().equals(wanted.potion())) {
                return false;
            }
        }
        if (want.has(ModDataComponents.MUNDANE.get()) && !MundaneBrewing.isMundane(have)) {
            return false;
        }
        if (want.has(ModDataComponents.REMEDY.get()) && !want.get(ModDataComponents.REMEDY.get()).equals(Remedies.effectOf(have))) {
            return false;
        }
        var essence = want.get(ModDataComponents.ESSENCE.get());
        if (essence != null && !essence.equals(have.get(ModDataComponents.ESSENCE.get()))) {
            return false;
        }
        if (EyedBrewing.isLatent(want) && !EyedBrewing.isLatent(have)) {
            return false;
        }
        return true;
    }
}
