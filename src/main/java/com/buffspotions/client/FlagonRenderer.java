package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.flagon.FlagonBlockEntity;
import com.buffspotions.flagon.FlagonContents;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class FlagonRenderer implements BlockEntityRenderer<FlagonBlockEntity, FlagonRenderState> {
    private static final SpriteId FLUID = new SpriteId(
            Sheets.DECORATED_POT_SHEET,
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "entity/decorated_pot/flagon_fluid")
    );
    private static final float MIN = 4.25F / 16.0F;
    private static final float MAX = 11.75F / 16.0F;
    private static final float BOTTOM = 1.05F / 16.0F;
    private static final float CEILING = 11.85F / 16.0F;
    private final SpriteGetter sprites;

    public FlagonRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    @Override
    public FlagonRenderState createRenderState() {
        return new FlagonRenderState();
    }

    @Override
    public void extractRenderState(
            FlagonBlockEntity blockEntity,
            FlagonRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        FlagonRenderState.extract(blockEntity, state);
    }

    public static void submitFluid(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            SpriteGetter sprites,
            int doses,
            int topColor,
            int bottomColor,
            int light
    ) {
        if (doses <= 0) {
            return;
        }
        TextureAtlasSprite sprite = sprites.get(FLUID);
        RenderType type = FLUID.renderType(id -> RenderTypes.entityTranslucent(id));
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            float top = BOTTOM + doses / (float) FlagonContents.MAX_DOSES * (CEILING - BOTTOM);
            float u0 = sprite.getU0();
            float u1 = sprite.getU1();
            float v0 = sprite.getV0();
            float v1 = sprite.getV1();
            quad(buffer, pose, MIN, top, MIN, MAX, top, MIN, MAX, top, MAX, MIN, top, MAX, u0, v0, u1, v1, BOTTOM, top, bottomColor, topColor, light, 0.0F, 1.0F, 0.0F);
            quad(buffer, pose, MIN, BOTTOM, MAX, MIN, top, MAX, MAX, top, MAX, MAX, BOTTOM, MAX, u0, v0, u1, v1, BOTTOM, top, bottomColor, topColor, light, 0.0F, 0.0F, 1.0F);
            quad(buffer, pose, MAX, BOTTOM, MIN, MAX, top, MIN, MIN, top, MIN, MIN, BOTTOM, MIN, u0, v0, u1, v1, BOTTOM, top, bottomColor, topColor, light, 0.0F, 0.0F, -1.0F);
            quad(buffer, pose, MIN, BOTTOM, MIN, MIN, top, MIN, MIN, top, MAX, MIN, BOTTOM, MAX, u0, v0, u1, v1, BOTTOM, top, bottomColor, topColor, light, -1.0F, 0.0F, 0.0F);
            quad(buffer, pose, MAX, BOTTOM, MAX, MAX, top, MAX, MAX, top, MIN, MAX, BOTTOM, MIN, u0, v0, u1, v1, BOTTOM, top, bottomColor, topColor, light, 1.0F, 0.0F, 0.0F);
        });
    }

    @Override
    public void submit(FlagonRenderState state, PoseStack poseStack, SubmitNodeCollector collector, net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        submitFluid(poseStack, collector, this.sprites, state.doses, state.fluidColor, state.fluidBottom, state.lightCoords);
    }

    private static void quad(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float u0, float v0, float u1, float v1,
            float bottomY, float topY, int bottomColor, int topColor, int light,
            float nx, float ny, float nz
    ) {
        vertex(buffer, pose, x0, y0, z0, u0, v0, colorAt(y0, bottomY, topY, bottomColor, topColor), light, nx, ny, nz);
        vertex(buffer, pose, x1, y1, z1, u0, v1, colorAt(y1, bottomY, topY, bottomColor, topColor), light, nx, ny, nz);
        vertex(buffer, pose, x2, y2, z2, u1, v1, colorAt(y2, bottomY, topY, bottomColor, topColor), light, nx, ny, nz);
        vertex(buffer, pose, x3, y3, z3, u1, v0, colorAt(y3, bottomY, topY, bottomColor, topColor), light, nx, ny, nz);
    }

    private static int colorAt(float y, float bottomY, float topY, int bottomColor, int topColor) {
        float span = topY - bottomY;
        float t = span <= 0.0F ? 1.0F : (y - bottomY) / span;
        t = Math.max(0.0F, Math.min(1.0F, t));
        return lerp(bottomColor, topColor, t);
    }

    private static int lerp(int from, int to, float t) {
        int alpha = channel(from >>> 24, to >>> 24, t);
        int red = channel((from >> 16) & 255, (to >> 16) & 255, t);
        int green = channel((from >> 8) & 255, (to >> 8) & 255, t);
        int blue = channel(from & 255, to & 255, t);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    private static int channel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }

    private static void vertex(
            VertexConsumer buffer,
            PoseStack.Pose pose,
            float x, float y, float z,
            float u, float v,
            int color, int light,
            float nx, float ny, float nz
    ) {
        buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
    }

}
