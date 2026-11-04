package com.buffspotions.mixin.client;

import com.buffspotions.effect.ItemEating;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
abstract class ItemEatAnimationMixin {
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$eatAnimation(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> callback) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        for (Player player : minecraft.level.players()) {
            if (player.isUsingItem() && ItemEating.canEat(player, player.getUseItem()) && ItemStack.isSameItem(player.getUseItem(), stack)) {
                callback.setReturnValue(ItemUseAnimation.EAT);
                return;
            }
        }
    }
}
