package com.buffspotions.mixin;

import com.buffspotions.effect.DurabilityEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ItemStack.class)
abstract class ItemStackDurabilityMixin {
    @ModifyVariable(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private int buffsPotions$fumbling(
            int amount,
            int ignored,
            ServerLevel level,
            @Nullable LivingEntity entity,
            Consumer<Item> onBreak
    ) {
        return DurabilityEffects.scaleIncomingDamage(amount, (ItemStack) (Object) this, entity);
    }

    @Inject(
            method = "processDurabilityChange(ILnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;)I",
            at = @At("RETURN"),
            cancellable = true
    )
    private void buffsPotions$finesse(
            int amount,
            ServerLevel level,
            @Nullable LivingEntity entity,
            CallbackInfoReturnable<Integer> callback
    ) {
        if (DurabilityEffects.skipDamage(callback.getReturnValue(), (ItemStack) (Object) this, entity)) {
            callback.setReturnValue(0);
        }
    }
}
