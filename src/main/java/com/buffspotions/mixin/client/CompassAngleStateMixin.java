package com.buffspotions.mixin.client;

import com.buffspotions.client.SeekingClient;
import com.buffspotions.effect.SeekAim;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CompassAngleState.class)
abstract class CompassAngleStateMixin {
    @Shadow
    abstract float getRandomlySpinningRotation(int seed, long gameTime);

    @Shadow
    abstract float getRotationTowardsCompassTarget(ItemOwner owner, long gameTime, BlockPos compassTargetPos);

    @Inject(method = "calculate", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$seekingNeedle(
            ItemStack stack,
            ClientLevel level,
            int seed,
            ItemOwner owner,
            CallbackInfoReturnable<Float> callback
    ) {
        if (!stack.is(Items.COMPASS) || stack.has(DataComponents.LODESTONE_TRACKER)) {
            return;
        }
        LivingEntity living = owner.asLivingEntity();
        if (!(living instanceof Player player) || !player.hasEffect(ModEffects.SEEKING)) {
            return;
        }
        if (!player.hasData(ModAttachments.SEEK_AIM.get())) {
            callback.setReturnValue(this.getRandomlySpinningRotation(seed, level.getGameTime()));
            return;
        }
        SeekAim aim = player.getData(ModAttachments.SEEK_AIM.get());
        if (aim.searching()) {
            callback.setReturnValue(uniform(owner));
            return;
        }
        boolean here = aim.found()
                && aim.dimension().equals(owner.level().dimension().identifier().toString());
        if (here) {
            callback.setReturnValue(this.getRotationTowardsCompassTarget(
                    owner,
                    level.getGameTime(),
                    BlockPos.containing(aim.x(), aim.y(), aim.z())
            ));
            return;
        }
        callback.setReturnValue(this.getRandomlySpinningRotation(seed, level.getGameTime()));
    }

    private static float uniform(ItemOwner owner) {
        float yaw = Mth.positiveModulo(owner.getVisualRotationYInDegrees() / 360.0F, 1.0F);
        return Mth.positiveModulo(0.5F - (yaw - 0.25F - SeekingClient.spin()), 1.0F);
    }
}
