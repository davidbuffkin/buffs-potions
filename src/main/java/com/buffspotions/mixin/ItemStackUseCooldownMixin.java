package com.buffspotions.mixin;

import com.buffspotions.belt.BeltConsumptionService;
import com.buffspotions.item.PotionBeltItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
abstract class ItemStackUseCooldownMixin {
    @Inject(method = "applyAfterUseComponentSideEffects", at = @At("RETURN"))
    private void buffsPotions$beltCooldown(LivingEntity user, ItemStack stackBeforeUsing, CallbackInfoReturnable<ItemStack> callback) {
        if (!(user instanceof ServerPlayer player) || !(stackBeforeUsing.getItem() instanceof PotionBeltItem)) {
            return;
        }
        // The belt's UseCooldown group always writes a full second after use, which replaces Quick Quaff.
        player.getCooldowns().addCooldown(stackBeforeUsing, BeltConsumptionService.cooldownTicks(stackBeforeUsing));
    }
}
