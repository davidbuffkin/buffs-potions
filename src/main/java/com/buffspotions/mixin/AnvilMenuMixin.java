package com.buffspotions.mixin;

import com.buffspotions.item.PotionBeltItem;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {
    @Redirect(
            method = "createResultInternal",
            at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I")
    )
    private int buffsPotions$beltRepair(int damage, int perUnit) {
        ItemStack input = ((AnvilMenu) (Object) this).getSlot(0).getItem();
        if (input.getItem() instanceof PotionBeltItem) {
            perUnit = Math.max(1, input.getMaxDamage() / 2);
        }
        return Math.min(damage, perUnit);
    }
}
