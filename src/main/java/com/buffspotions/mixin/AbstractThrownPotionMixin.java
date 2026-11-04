package com.buffspotions.mixin;

import com.buffspotions.potion.Remedies;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.level.block.LevelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AbstractThrownPotion.class)
abstract class AbstractThrownPotionMixin {
    @ModifyArg(
            method = "onHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(ILnet/minecraft/core/BlockPos;I)V"
            ),
            index = 0
    )
    private int buffsPotions$remedySplash(int type) {
        AbstractThrownPotion potion = (AbstractThrownPotion) (Object) this;
        if (type == LevelEvent.PARTICLES_SPELL_POTION_SPLASH
                && potion instanceof ThrownSplashPotion
                && Remedies.isRemedy(potion.getItem())) {
            return LevelEvent.PARTICLES_INSTANT_POTION_SPLASH;
        }
        return type;
    }
}
