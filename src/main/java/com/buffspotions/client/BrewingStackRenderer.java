package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.AnchoredEnderFlagonItem;
import com.buffspotions.registry.ModItems;
import com.buffspotions.stack.BrewingStackBlock;
import com.buffspotions.stack.BrewingStackBlockEntity;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.standalone.StandaloneModelKey;
import org.jspecify.annotations.Nullable;

public final class BrewingStackRenderer implements BlockEntityRenderer<BrewingStackBlockEntity, BrewingStackRenderState> {
    public static final StandaloneModelKey<BlockStateModelPart> FLAGON_OPEN = new StandaloneModelKey<>(
            () -> BuffsPotions.MOD_ID + ":block/flagon_open"
    );
    public static final StandaloneModelKey<BlockStateModelPart> ANCHORED_FLAGON = new StandaloneModelKey<>(
            () -> BuffsPotions.MOD_ID + ":block/anchored_ender_flagon_open"
    );
    private static final Identifier FLAGON_OPEN_MODEL =
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "block/flagon_open");
    private static final Identifier ANCHORED_FLAGON_MODEL =
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "block/anchored_ender_flagon_open");

    private final SpriteGetter sprites;

    public BrewingStackRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
    }

    public static Identifier flagonModel() {
        return FLAGON_OPEN_MODEL;
    }

    public static Identifier anchoredModel() {
        return ANCHORED_FLAGON_MODEL;
    }

    @Override
    public BrewingStackRenderState createRenderState() {
        return new BrewingStackRenderState();
    }

    @Override
    public void extractRenderState(
            BrewingStackBlockEntity blockEntity,
            BrewingStackRenderState state,
            float partialTick,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPosition, breakProgress);
        ItemStack flagon = blockEntity.getItem(BrewingStackBlockEntity.SLOT_FLAGON);
        state.hasFlagon = !flagon.isEmpty();
        state.anchored = flagon.is(ModItems.ANCHORED_ENDER_FLAGON.get());
        FlagonContents contents = state.anchored ? AnchoredEnderFlagonItem.shown(flagon) : FlagonContents.of(flagon);
        state.doses = contents.doses();
        FlagonRenderState painted = new FlagonRenderState();
        FlagonRenderState.paint(contents, painted);
        state.fluidColor = painted.fluidColor;
        state.fluidBottom = painted.fluidBottom;
        state.facing = blockEntity.getBlockState().getValue(BrewingStackBlock.FACING);
    }

    @Override
    public void submit(
            BrewingStackRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera
    ) {
        if (!state.hasFlagon) {
            return;
        }
        BlockStateModelPart flagon = Minecraft.getInstance().getModelManager().getStandaloneModel(
                state.anchored ? ANCHORED_FLAGON : FLAGON_OPEN
        );
        if (flagon == null) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(switch (state.facing) {
            case WEST -> -90.0F;
            case NORTH -> 180.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        }));
        poseStack.translate(-0.5F, 2.0F / 16.0F, -0.5F);
        collector.submitBlockModel(
                poseStack,
                Sheets.translucentBlockItemSheet(),
                List.of(flagon),
                new int[0],
                state.lightCoords,
                OverlayTexture.NO_OVERLAY,
                0
        );
        FlagonRenderer.submitFluid(poseStack, collector, this.sprites, state.doses, state.fluidColor, state.fluidBottom, state.lightCoords);
        poseStack.popPose();
    }
}
