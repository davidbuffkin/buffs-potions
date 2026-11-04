package com.buffspotions.client;

import com.buffspotions.flagon.AnchoredEnderFlagonBlockEntity;
import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class AnchoredEnderFlagonRenderer implements BlockEntityRenderer<AnchoredEnderFlagonBlockEntity, FlagonRenderState> {
    private final SpriteGetter sprites;

    public AnchoredEnderFlagonRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public FlagonRenderState createRenderState() {
        return new FlagonRenderState();
    }

    @Override
    public void extractRenderState(
            AnchoredEnderFlagonBlockEntity blockEntity,
            FlagonRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        FlagonContents contents = blockEntity.contents();
        if (blockEntity.anchor() != null && blockEntity.anchor().player().equals(EnderFlagons.clientPlayer())) {
            contents = EnderFlagons.viewed();
        }
        state.doses = contents.doses();
        FlagonRenderState.paint(contents, state);
    }

    @Override
    public void submit(FlagonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        FlagonRenderer.submitFluid(
                poseStack,
                collector,
                this.sprites,
                state.doses,
                state.fluidColor,
                state.fluidBottom,
                state.lightCoords
        );
    }
}
