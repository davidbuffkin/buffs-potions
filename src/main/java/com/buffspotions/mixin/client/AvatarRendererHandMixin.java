package com.buffspotions.mixin.client;

import com.buffspotions.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AvatarRenderer.class)
abstract class AvatarRendererHandMixin {
    /** Same skin alpha as the dislocated body. */
    private static final int TRANSLUCENT_SKIN = 0x99FFFFFF;

    @Redirect(
            method = "renderHand",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModelPart(Lnet/minecraft/client/model/geom/ModelPart;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
            )
    )
    private void buffsPotions$translucentHand(
            SubmitNodeCollector collector,
            ModelPart arm,
            PoseStack poseStack,
            RenderType renderType,
            int lightCoords,
            int overlayCoords,
            TextureAtlasSprite sprite
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        int color = player != null && player.hasEffect(ModEffects.DISLOCATED) ? TRANSLUCENT_SKIN : -1;
        collector.submitModelPart(arm, poseStack, renderType, lightCoords, overlayCoords, sprite, color, null);
    }
}
