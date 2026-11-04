package com.buffspotions.mixin.client;

import com.buffspotions.client.DisguiseSkins;
import com.buffspotions.client.DisguiseStandMark;
import com.buffspotions.client.DisguiseStandModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adult zombies, villagers, and armor stands with a disguise are drawn as that player.
 * Baby zombies stay zombies. A spyglass leaves the name blank, so the real body draws.
 */
@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityDisguiseMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
            at = @At("RETURN")
    )
    private void buffsPotions$disguiseName(LivingEntity entity, LivingEntityRenderState state, float partialTicks, CallbackInfo callback) {
        DisguiseStandMark mark = (DisguiseStandMark) (Object) state;
        String name = disguises(entity) ? DisguiseSkins.subjectName(entity) : null;
        mark.buffsPotions$setDisguiseName(name == null ? "" : name);
        mark.buffsPotions$setStillArms(entity instanceof ArmorStand && !entity.hasCustomName());
        if (name != null && !name.isEmpty() && state.nameTag != null) {
            float height = EntityTypes.PLAYER.getDimensions().height();
            if (state.isBaby) {
                height *= 0.5F;
            }
            state.nameTagAttachment = new Vec3(0.0, height, 0.0);
        }
    }

    @Inject(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void buffsPotions$disguise(
            LivingEntityRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera,
            CallbackInfo callback
    ) {
        String name = ((DisguiseStandMark) (Object) state).buffsPotions$disguiseName();
        if (name.isEmpty()) {
            return;
        }
        DisguiseStandModel.submit(name, state, poseStack, submitNodeCollector);
        ((EntityRendererNameInvoker) (Object) this).buffsPotions$submitNameDisplay(state, poseStack, submitNodeCollector, camera);
        callback.cancel();
    }

    private static boolean disguises(LivingEntity entity) {
        if (entity instanceof ArmorStand) {
            return true;
        }
        if (entity instanceof Villager villager) {
            return !villager.isBaby();
        }
        return entity instanceof Zombie zombie && !zombie.isBaby();
    }
}
