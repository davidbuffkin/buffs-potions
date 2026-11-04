package com.buffspotions.mixin;

import com.buffspotions.belt.BeltMending;
import com.buffspotions.effect.Insight;
import com.buffspotions.equipment.BeltSlotAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {
    @Inject(method = "repairPlayerItems", at = @At("RETURN"))
    private void buffsPotions$syncBeltRepair(ServerPlayer player, int amount, CallbackInfoReturnable<Integer> callback) {
        ItemStack belt = BeltSlotAccess.getEquipped(player);
        if (BeltMending.hasMending(belt)) {
            BeltSlotAccess.setEquipped(player, belt);
        }
    }

    @Inject(method = "followNearbyPlayer", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$itemEaterPull(CallbackInfo callback) {
        if (Insight.pullsOrb((ExperienceOrb) (Object) this)) {
            callback.cancel();
        }
    }
}
