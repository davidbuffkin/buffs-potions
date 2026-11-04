package com.buffspotions.mixin.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Advancement icons are drawn with no item owner, and a clock with no owner stays on its first frame.
 * Drawing it the way an inventory slot does lets the vanilla model follow the sun.
 */
@Mixin(GuiGraphicsExtractor.class)
abstract class AdvancementClockMixin {
    @Inject(method = "fakeItem(Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$clockFollowsSun(ItemStack stack, int x, int y, int seed, CallbackInfo callback) {
        if (!stack.is(Items.CLOCK)) {
            return;
        }
        ((GuiGraphicsExtractor) (Object) this).item(stack, x, y, seed);
        callback.cancel();
    }
}
