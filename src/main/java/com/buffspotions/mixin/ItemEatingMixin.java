package com.buffspotions.mixin;

import com.buffspotions.effect.ItemEating;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
abstract class ItemEatingMixin {
    /** Set when this eat should keep the item's meal and add Item Eater's hunger afterward. */
    private static final ThreadLocal<Boolean> ADD_HUNGER = ThreadLocal.withInitial(() -> false);
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$startEating(
            Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> callback
    ) {
        if (!ItemEating.canEat(player, player.getItemInHand(hand))) {
            return;
        }
        player.startUsingItem(hand);
        callback.setReturnValue(InteractionResult.CONSUME);
    }

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$eatDuration(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Integer> callback) {
        if (ItemEating.canEat(user, stack)) {
            callback.setReturnValue(ItemEating.EAT_TICKS);
        }
    }

    @Inject(method = "onUseTick", at = @At("HEAD"))
    private void buffsPotions$eatSounds(
            Level level, LivingEntity entity, ItemStack stack, int remaining, CallbackInfo callback
    ) {
        ItemEating.tickEat(entity, stack, remaining);
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$finishEating(
            ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> callback
    ) {
        if (!ItemEating.canEat(entity, stack)) {
            return;
        }
        if (ItemEating.hasFoodValue(stack)) {
            ADD_HUNGER.set(true);
            return;
        }
        ADD_HUNGER.set(false);
        if (entity instanceof Player player) {
            ItemEating.finishEat(player, stack);
            ItemEating.feed(player);
        }
        stack.consume(1, entity);
        callback.setReturnValue(stack);
    }

    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void buffsPotions$addHunger(
            ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> callback
    ) {
        if (!ADD_HUNGER.get()) {
            return;
        }
        ADD_HUNGER.set(false);
        if (entity instanceof Player player) {
            ItemEating.feed(player);
        }
    }
}
