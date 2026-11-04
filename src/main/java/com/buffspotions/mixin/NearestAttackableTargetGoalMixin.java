package com.buffspotions.mixin;

import com.buffspotions.registry.ModAttachments;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NearestAttackableTargetGoal.class)
abstract class NearestAttackableTargetGoalMixin {
    @Shadow
    protected LivingEntity target;

    @Shadow
    protected Class targetType;

    @Inject(method = "findTarget", at = @At("RETURN"))
    private void buffsPotions$targetShadow(CallbackInfo callback) {
        if (this.targetType != Player.class && this.targetType != net.minecraft.server.level.ServerPlayer.class) {
            return;
        }
        Mob mob = ((TargetGoalAccess) (Object) this).buffsPotions$mob();
        if (!(mob.level() instanceof ServerLevel level) || level.getDifficulty() == Difficulty.PEACEFUL) {
            return;
        }
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB area = mob.getBoundingBox().inflate(range, range, range);
        double best = this.target == null ? range * range : mob.distanceToSqr(this.target);
        Mannequin chosen = null;
        for (Mannequin shadow : level.getEntitiesOfClass(Mannequin.class, area, entity -> entity.hasData(ModAttachments.ENDER_SHADOW))) {
            if (!mob.canAttack(shadow)) {
                continue;
            }
            if (((TargetGoalAccess) (Object) this).buffsPotions$mustSee() && !mob.getSensing().hasLineOfSight(shadow)) {
                continue;
            }
            double distance = mob.distanceToSqr(shadow);
            if (distance < best) {
                best = distance;
                chosen = shadow;
            }
        }
        if (chosen != null) {
            this.target = chosen;
        }
    }
}
