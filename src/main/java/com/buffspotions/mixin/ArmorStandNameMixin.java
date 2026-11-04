package com.buffspotions.mixin;

import com.buffspotions.entity.ArmorStandPoses;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Sneak-use cycles Bedrock poses. A stick gives the stand arms and leaves that pose alone. */
@Mixin(ArmorStand.class)
abstract class ArmorStandNameMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$stickArms(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> callback) {
        ArmorStand stand = (ArmorStand) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        if (stand.isMarker()) {
            return;
        }
        if (player.isShiftKeyDown() && !stack.is(Items.NAME_TAG)) {
            if (!player.level().isClientSide()) {
                ArmorStandPoses.cycle(stand);
                stand.level().playSound(null, stand.blockPosition(), SoundEvents.ARMOR_STAND_HIT, SoundSource.NEUTRAL, 0.5F, 1.0F);
            }
            callback.setReturnValue(player.level().isClientSide() ? InteractionResult.SUCCESS_SERVER : InteractionResult.SUCCESS);
            return;
        }
        if (stand.showArms() || !stack.is(Items.STICK)) {
            return;
        }
        if (!player.level().isClientSide()) {
            stand.setShowArms(true);
            if (!player.hasInfiniteMaterials()) {
                stack.shrink(1);
            }
        }
        callback.setReturnValue(player.level().isClientSide() ? InteractionResult.SUCCESS_SERVER : InteractionResult.SUCCESS);
    }
}
