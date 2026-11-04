package com.buffspotions.mixin;

import com.buffspotions.registry.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ImbueRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ImbueRecipe.class)
public abstract class ImbueRecipeMixin {
    @Inject(method = "assemble", at = @At("RETURN"))
    private void buffsPotions$copyEssence(CraftingInput input, CallbackInfoReturnable<ItemStack> callback) {
        ItemStack result = callback.getReturnValue();
        if (result.isEmpty()) {
            return;
        }
        var subject = input.getItem(1, 1).get(ModDataComponents.ESSENCE.get());
        if (subject != null) {
            result.set(ModDataComponents.ESSENCE.get(), subject);
        }
    }
}
