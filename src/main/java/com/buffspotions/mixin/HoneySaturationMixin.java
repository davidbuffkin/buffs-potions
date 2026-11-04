package com.buffspotions.mixin;

import com.buffspotions.belt.HoneySaturation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodProperties.class)
abstract class HoneySaturationMixin {
    @Inject(method = "onConsume", at = @At("RETURN"))
    private void buffsPotions$honeySaturation(
            Level level, LivingEntity user, ItemStack stack, Consumable consumable, CallbackInfo callback
    ) {
        if (!stack.is(Items.HONEY_BOTTLE) || !(user instanceof Player player)) {
            return;
        }
        HoneySaturation.afterEat(player, ((FoodProperties) (Object) this).saturation());
    }
}
