package com.buffspotions.advancement;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.EyedBrewing;
import com.buffspotions.potion.PotionGrades;
import com.buffspotions.registry.ModCriteria;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Grants brewing advancements when a finished brew leaves a stand or a stack. */
public final class BrewedPotionGrant {
    private BrewedPotionGrant() {
    }

    public static void bottle(ServerPlayer player, ItemStack stack) {
        grantShared(player, stack);
        if (EyedBrewing.isLatent(stack)) {
            ModCriteria.LATENT_STAND.get().trigger(player);
        }
    }

    public static void flagon(Player player, ItemStack flagon) {
        if (!(player instanceof ServerPlayer server)) {
            return;
        }
        ItemStack dose = FlagonContents.of(flagon).dose();
        grantShared(server, dose);
        if (PotionGrades.insightThree(dose)) {
            ModCriteria.INSIGHT_THREE.get().trigger(server);
        }
        if (PotionGrades.nightVisionTwice(dose)) {
            ModCriteria.NIGHT_VISION_FAR.get().trigger(server);
        }
        if (PotionGrades.strengthClarified(dose)) {
            ModCriteria.STRENGTH_MEDIUM.get().trigger(server);
        }
    }

    private static void grantShared(ServerPlayer player, ItemStack dose) {
        if (PotionGrades.speedTwo(dose)) {
            ModCriteria.LEVEL_TWO.get().trigger(player);
        }
        if (PotionGrades.longWaterBreathing(dose)) {
            ModCriteria.EXTENDED.get().trigger(player);
        }
    }
}
