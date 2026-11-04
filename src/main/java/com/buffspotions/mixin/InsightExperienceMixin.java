package com.buffspotions.mixin;

import com.buffspotions.registry.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
abstract class InsightExperienceMixin {
    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int buffsPotions$insight(int points) {
        Player player = (Player) (Object) this;
        MobEffectInstance insight = player.getEffect(ModEffects.INSIGHTFUL);
        if (insight == null || points <= 0) {
            return points;
        }
        float chance = 0.25F * (insight.getAmplifier() + 1);
        int whole = (int) chance;
        float fraction = chance - whole;
        int extra = points * whole;
        for (int point = 0; point < points; point++) {
            if (player.getRandom().nextFloat() < fraction) {
                extra++;
            }
        }
        return points + extra;
    }
}
