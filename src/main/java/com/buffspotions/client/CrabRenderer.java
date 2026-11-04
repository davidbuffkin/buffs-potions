package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.crab.Crab;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public class CrabRenderer extends MobRenderer<Crab, CrabRenderState, CrabModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/entity/crab/body.png");
    private static final int INVISIBLE_TINT = 0x26FFFFFF;

    public CrabRenderer(EntityRendererProvider.Context context) {
        super(context, new CrabModel(context.bakeLayer(CrabModel.LAYER)), 0.35F);
    }

    @Override
    public Identifier getTextureLocation(CrabRenderState state) {
        return TEXTURE;
    }

    @Override
    public CrabRenderState createRenderState() {
        return new CrabRenderState();
    }

    @Override
    public void extractRenderState(Crab entity, CrabRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.hasClaw = entity.hasClaw();
        state.aggravated = entity.isAggravated();
        state.waveTicks = entity.waveTicks();
        state.climbing = entity.isClimbing();
        state.climbBlend = entity.climbBlend(partialTicks);
        state.wallFacing = entity.getWallFacing();
        this.shadowRadius = state.isBaby ? 0.2F : 0.35F;
        float yaw = entity.getYRot(partialTicks) * Mth.DEG_TO_RAD;
        double side = state.wallFacing.getStepX() * Mth.cos(yaw) + state.wallFacing.getStepZ() * Mth.sin(yaw);
        state.leftLegsUp = side < 0.0;
        double dx = entity.getX() - entity.xo;
        double dz = entity.getZ() - entity.zo;
        float forward = (float) (dx * -Mth.sin(yaw) + dz * Mth.cos(yaw));
        float strafe = (float) (dx * Mth.cos(yaw) + dz * Mth.sin(yaw));
        state.sidewaysGait = entity.isClimbing() || Math.abs(strafe) >= Math.abs(forward);
    }

    @Override
    public void submit(CrabRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        float scale = state.scale;
        poseStack.scale(scale, scale, scale);
        this.setupRotations(state, poseStack, state.bodyRot, scale);
        if (!state.isInvisible || !state.isInvisibleToPlayer) {
            int color = state.isInvisible ? INVISIBLE_TINT : -1;
            CrabMesh.submit(
                    poseStack,
                    collector,
                    state,
                    state.lightCoords,
                    LivingEntityRenderer.getOverlayCoords(state, this.getWhiteOverlayProgress(state)),
                    color
            );
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected @Nullable RenderType getRenderType(CrabRenderState state, boolean isBodyVisible, boolean forceTransparent, boolean appearGlowing) {
        return null;
    }
}
