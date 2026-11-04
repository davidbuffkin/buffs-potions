package com.buffspotions.mixin;

import com.buffspotions.advancement.BrewedPotionGrant;
import com.buffspotions.menu.StandBookMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Slot.class)
abstract class BrewingStandTakeMixin {
    @Inject(method = "onTake", at = @At("HEAD"))
    private void buffsPotions$brewingStandBottle(Player player, ItemStack stack, CallbackInfo callback) {
        Slot slot = (Slot) (Object) this;
        if (player instanceof ServerPlayer server
                && (player.containerMenu instanceof BrewingStandMenu || player.containerMenu instanceof StandBookMenu)
                && slot.index >= 0
                && slot.index <= 2) {
            BrewedPotionGrant.bottle(server, stack);
        }
    }
}
