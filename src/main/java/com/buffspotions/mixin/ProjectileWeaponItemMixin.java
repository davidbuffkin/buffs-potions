package com.buffspotions.mixin;

import com.buffspotions.belt.BeltTipping;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {
    @Inject(method = "shoot", at = @At("HEAD"))
    private void buffsPotions$beginShot(
            ServerLevel level,
            LivingEntity shooter,
            InteractionHand hand,
            ItemStack weapon,
            List<ItemStack> projectiles,
            float power,
            float uncertainty,
            boolean isCrit,
            LivingEntity targetOverride,
            CallbackInfo callback
    ) {
        BeltTipping.beginShot(shooter);
    }

    @Inject(method = "shoot", at = @At("RETURN"))
    private void buffsPotions$endShot(
            ServerLevel level,
            LivingEntity shooter,
            InteractionHand hand,
            ItemStack weapon,
            List<ItemStack> projectiles,
            float power,
            float uncertainty,
            boolean isCrit,
            LivingEntity targetOverride,
            CallbackInfo callback
    ) {
        BeltTipping.endShot();
    }

    @Inject(method = "createProjectile", at = @At("RETURN"))
    private void buffsPotions$tip(
            Level level,
            LivingEntity shooter,
            ItemStack weapon,
            ItemStack projectile,
            boolean isCrit,
            CallbackInfoReturnable<Projectile> callback
    ) {
        if (callback.getReturnValue() instanceof AbstractArrow arrow) {
            BeltTipping.tip(arrow, shooter, projectile);
        }
    }
}
