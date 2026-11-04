package com.buffspotions.mixin;

import com.buffspotions.menu.StandBookMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandBlockEntity.class)
abstract class BrewingStandMenuSwapMixin {
    @Shadow
    protected ContainerData dataAccess;

    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$recipeBook(int containerId, Inventory inventory, CallbackInfoReturnable<AbstractContainerMenu> callback) {
        callback.setReturnValue(new StandBookMenu(containerId, inventory, (BrewingStandBlockEntity) (Object) this, this.dataAccess));
    }
}
