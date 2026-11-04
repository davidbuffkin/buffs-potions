package com.buffspotions.mixin;

import com.buffspotions.belt.BeltDeathExperience;
import com.buffspotions.equipment.BeltSlotAccess;
import com.buffspotions.item.PotionBeltItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gamerules.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
abstract class PlayerDeathExperienceMixin {
    /** Wearing a belt drops one third of held experience. Left in place so it can be turned back on. */
    private static final boolean ENABLED = false;

    @Inject(method = "getBaseExperienceReward", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$dropThird(ServerLevel level, CallbackInfoReturnable<Integer> callback) {
        if (!ENABLED) {
            return;
        }
        Player player = (Player) (Object) this;
        if (level.getGameRules().get(GameRules.KEEP_INVENTORY) || player.isSpectator()) {
            return;
        }
        if (!(BeltSlotAccess.getEquipped(player).getItem() instanceof PotionBeltItem)) {
            return;
        }
        callback.setReturnValue(BeltDeathExperience.dropped(player));
    }
}
