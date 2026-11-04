package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.crab.Crab;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Blockbench crab, drawn as its own cubes because each face has its own
 * texture coordinates and blank faces are left out.
 */
final class CrabMesh {
    private static final Identifier[] TEXTURES = {
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/entity/crab/body.png"),
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/entity/crab/eye.png"),
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/entity/crab/upper_claw.png"),
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/entity/crab/right_claw.png"),
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/entity/crab/leg.png"),
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "textures/entity/crab/baby_body.png")
    };
    /**
     * Java block models store face UVs in the project resolution. This project
     * is 16 by 16, including the body, whose image is 32 pixels on a side.
     */
    private static final float UV_RESOLUTION = 16.0F;

    private enum Kind {
        BODY, EYE, SMALL_CLAW, BIG_CLAW, LEG
    }

    private record Part(
            Kind kind,
            int texture,
            int leg,
            float x0, float y0, float z0,
            float x1, float y1, float z1,
            float rx, float ry, float rz,
            float ox, float oy, float oz,
            float[] uvs
    ) {
    }

    private static final Part[] PARTS = {
            new Part(Kind.BODY, 0, -1, 3.5F, 1.5F, 4.5F, 12.5F, 5.5F, 11.5F, 0F, 0F, 0F, 7.5F, 1.5F, 7.5F, new float[]{0F, 3.5F, 4.5F, 7F, 0F, 0F, 4.5F, 3.5F, 4.5F, 0F, 9F, 2F, 4.5F, 2F, 9F, 4F, 4.5F, 6F, 8F, 8F, 4.5F, 4F, 8F, 6F}),
            new Part(Kind.EYE, 1, -1, 10.5F, 4.5F, 4.45F, 11.5F, 6.5F, 4.45F, 0F, 0F, 0F, 10.5F, 4.5F, 3.45F, new float[]{0F, 0F, 1F, 0F, 0F, 0F, 1F, 0F, 0F, 0F, 1F, 2F, 1F, 0F, 2F, 2F, 0F, 0F, 0F, 2F, 0F, 0F, 0F, 2F}),
            new Part(Kind.EYE, 1, -1, 4.5F, 4.5F, 4.45F, 5.5F, 6.5F, 4.45F, 0F, 0F, 0F, 4.5F, 4.5F, 3.45F, new float[]{0F, 0F, 1F, 0F, 0F, 2F, 1F, 2F, 1F, 0F, 2F, 2F, 0F, 0F, 1F, 2F, 2F, 0F, 2F, 2F, 0F, 0F, 0F, 2F}),
            new Part(Kind.SMALL_CLAW, 3, -1, 11.23727F, 1.49901F, 0.73593F, 13.23727F, 3.49901F, 3.73593F, -45.47095F, 37.7899F, -35.17112F, 12.51777F, 2.25F, 4.35355F, new float[]{3F, 3F, 5F, 6F, 3F, 0F, 5F, 3F, 0F, 4F, 2F, 6F, 5F, 0F, 7F, 2F, 0F, 2F, 3F, 4F, 0F, 0F, 3F, 2F}),
            new Part(Kind.BIG_CLAW, 2, -1, 0.73273F, 2.8235F, -0.76462F, 2.73273F, 6.8235F, 6.23538F, -29.33896F, -41.19603F, 14.64151F, 2.42714F, 4F, 6.3447F, new float[]{7F, 7F, 9F, 14F, 7F, 0F, 9F, 7F, 0F, 8F, 2F, 12F, 2F, 8F, 4F, 12F, 0F, 4F, 7F, 8F, 0F, 0F, 7F, 4F}),
            new Part(Kind.LEG, 4, 0, 12.5F, -2F, 6.75F, 14.5F, 1F, 7.75F, 6.32526F, 10.80355F, 30.59873F, 11.5F, 1F, 7.25F, new float[]{0F, 0F, 0F, 0F, 4F, 0F, 7F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 3F, 0F, 4F, 3F}),
            new Part(Kind.LEG, 4, 0, 14.5F, 1F, 7.75F, 12.5F, -2F, 6.75F, 6.32526F, 10.80355F, 30.59873F, 11.5F, 1F, 7.25F, new float[]{4F, 0F, 7F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 3F, 3F, 4F, 0F, 0F, 0F, 0F, 0F}),
            new Part(Kind.LEG, 4, 2, 12.25F, -2F, 5.25F, 14.25F, 1F, 6.25F, 26.56507F, 37.76123F, 39.23157F, 11.25F, 1F, 5.75F, new float[]{0F, 0F, 0F, 0F, 4F, 0F, 7F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 3F, 0F, 4F, 3F}),
            new Part(Kind.LEG, 4, 2, 14.25F, 1F, 6.25F, 12.25F, -2F, 5.25F, 26.56507F, 37.76123F, 39.23157F, 11.25F, 1F, 5.75F, new float[]{4F, 0F, 7F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 3F, 3F, 4F, 0F, 0F, 0F, 0F, 0F}),
            new Part(Kind.LEG, 4, 4, 12.5F, -2F, 8.25F, 14.5F, 1F, 9.25F, -3.76613F, -6.49053F, 30.21361F, 11.5F, 1F, 8.75F, new float[]{0F, 0F, 0F, 0F, 4F, 0F, 7F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 3F, 0F, 4F, 3F}),
            new Part(Kind.LEG, 4, 4, 14.5F, 1F, 9.25F, 12.5F, -2F, 8.25F, -3.76613F, -6.49053F, 30.21361F, 11.5F, 1F, 8.75F, new float[]{4F, 0F, 7F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 3F, 3F, 4F, 0F, 0F, 0F, 0F, 0F}),
            new Part(Kind.LEG, 4, 6, 12F, -2F, 10.25F, 14F, 1F, 11.25F, -11.70092F, -19.3546F, 32.00204F, 11F, 1F, 10.75F, new float[]{0F, 0F, 0F, 0F, 4F, 0F, 7F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 3F, 0F, 4F, 3F}),
            new Part(Kind.LEG, 4, 6, 14F, 1F, 11.25F, 12F, -2F, 10.25F, -11.70092F, -19.3546F, 32.00204F, 11F, 1F, 10.75F, new float[]{4F, 0F, 7F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 3F, 3F, 4F, 0F, 0F, 0F, 0F, 0F}),
            new Part(Kind.LEG, 4, 1, 1.5F, -2F, 6.75F, 3.5F, 1F, 7.75F, 6.32526F, -10.80355F, -30.59873F, 4.5F, 1F, 7.25F, new float[]{0F, 0F, 0F, 0F, 7F, 0F, 4F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 4F, 0F, 3F, 3F, 0F, 0F, 0F, 0F}),
            new Part(Kind.LEG, 4, 1, 3.5F, 1F, 7.75F, 1.5F, -2F, 6.75F, 6.32526F, -10.80355F, -30.59873F, 4.5F, 1F, 7.25F, new float[]{7F, 0F, 4F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 4F, 3F, 3F, 0F}),
            new Part(Kind.LEG, 4, 3, 2.25F, -2F, 5.5F, 4.25F, 1F, 6.5F, 17.66852F, -27.73061F, -34.39393F, 5.25F, 1F, 6F, new float[]{0F, 0F, 0F, 0F, 7F, 0F, 4F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 4F, 0F, 3F, 3F, 0F, 0F, 0F, 0F}),
            new Part(Kind.LEG, 4, 3, 4.25F, 1F, 6.5F, 2.25F, -2F, 5.5F, 17.66852F, -27.73061F, -34.39393F, 5.25F, 1F, 6F, new float[]{7F, 0F, 4F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 4F, 3F, 3F, 0F}),
            new Part(Kind.LEG, 4, 5, 1.5F, -2F, 8.25F, 3.5F, 1F, 9.25F, -3.76613F, 6.49053F, -30.21361F, 4.5F, 1F, 8.75F, new float[]{0F, 0F, 0F, 0F, 7F, 0F, 4F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 4F, 0F, 3F, 3F, 0F, 0F, 0F, 0F}),
            new Part(Kind.LEG, 4, 5, 3.5F, 1F, 9.25F, 1.5F, -2F, 8.25F, -3.76613F, 6.49053F, -30.21361F, 4.5F, 1F, 8.75F, new float[]{7F, 0F, 4F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 4F, 3F, 3F, 0F}),
            new Part(Kind.LEG, 4, 7, 2F, -2F, 10.25F, 4F, 1F, 11.25F, -11.70092F, 19.3546F, -32.00204F, 5F, 1F, 10.75F, new float[]{0F, 0F, 0F, 0F, 7F, 0F, 4F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 4F, 0F, 3F, 3F, 0F, 0F, 0F, 0F}),
            new Part(Kind.LEG, 4, 7, 4F, 1F, 11.25F, 2F, -2F, 10.25F, -11.70092F, 19.3546F, -32.00204F, 5F, 1F, 10.75F, new float[]{7F, 0F, 4F, 1F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 0F, 4F, 3F, 3F, 0F})
    };

    /**
     * The baby is already modeled smaller than the adult. Each leg is paired with a
     * reversed cube so the outside texture also shows on the inward faces.
     */
    private static final Part[] BABY_PARTS = {
            new Part(Kind.BODY, 5, -1, 6.0F, 1.5F, 7.0F, 10.0F, 3.5F, 10.0F, 0.0F, 0.0F, 0.0F, 8.0F, 0.5F, 8.0F, new float[]{1.5F, 5.5F, 3.5F, 7.0F, 1.0F, 1.0F, 3.0F, 2.5F, 4.5F, 0.0F, 6.5F, 1.0F, 6.0F, 3.0F, 8.0F, 4.0F, 4.5F, 6.5F, 6.0F, 7.5F, 6.5F, 4.5F, 8.0F, 5.5F}),
            new Part(Kind.EYE, 1, -1, 9.0F, 2.5F, 6.95F, 10.0F, 4.5F, 6.95F, 0.0F, 0.0F, 0.0F, 9.0F, 2.5F, 5.95F, new float[]{0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F, 0.0F, 2.0F, 2.0F, 0.0F, 0.0F, 0.0F, 2.0F, 0.0F, 0.0F, 0.0F, 2.0F}),
            new Part(Kind.EYE, 1, -1, 6.0F, 2.5F, 6.95F, 7.0F, 4.5F, 6.95F, 0.0F, 0.0F, 0.0F, 6.0F, 2.5F, 5.95F, new float[]{0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 2.0F, 1.0F, 2.0F, 1.0F, 0.0F, 2.0F, 2.0F, 0.0F, 0.0F, 1.0F, 2.0F, 2.0F, 0.0F, 2.0F, 2.0F, 0.0F, 0.0F, 0.0F, 2.0F}),
            new Part(Kind.SMALL_CLAW, 3, -1, 3.48727F, 2.24901F, 2.73593F, 4.48727F, 4.24901F, 5.73593F, 1.14846F, -62.75892F, -28.62228F, 3.76777F, 3.0F, 7.35355F, new float[]{4.0F, 3.0F, 5.0F, 6.0F, 3.0F, 0.0F, 4.0F, 3.0F, 0.0F, 4.0F, 1.0F, 6.0F, 5.0F, 0.0F, 6.0F, 2.0F, 0.0F, 2.0F, 3.0F, 4.0F, 0.0F, 0.0F, 3.0F, 2.0F}),
            new Part(Kind.LEG, 4, 1, 10.0F, -1.0F, 7.75F, 11.0F, 1.0F, 8.75F, 6.32526F, 10.80355F, 30.59873F, 8.0F, 1.0F, 8.25F, new float[]{0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F, 3.0F}),
            new Part(Kind.LEG, 4, 1, 11.0F, 1.0F, 8.75F, 10.0F, -1.0F, 7.75F, 6.32526F, 10.80355F, 30.59873F, 8.0F, 1.0F, 8.25F, new float[]{4.0F, 0.0F, 5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F}),
            new Part(Kind.LEG, 4, 2, 10.0F, -1.0F, 8.25F, 11.0F, 1.0F, 9.25F, -3.76613F, -6.49053F, 30.21361F, 8.0F, 1.0F, 8.75F, new float[]{0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F, 3.0F}),
            new Part(Kind.LEG, 4, 2, 11.0F, 1.0F, 9.25F, 10.0F, -1.0F, 8.25F, -3.76613F, -6.49053F, 30.21361F, 8.0F, 1.0F, 8.75F, new float[]{4.0F, 0.0F, 5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F}),
            new Part(Kind.LEG, 4, 3, 10.0F, -1.0F, 8.0F, 11.0F, 1.0F, 9.0F, -13.75825F, -36.4336F, 36.34919F, 8.0F, 1.0F, 8.5F, new float[]{0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F, 3.0F}),
            new Part(Kind.LEG, 4, 3, 11.0F, 1.0F, 9.0F, 10.0F, -1.0F, 8.0F, -13.75825F, -36.4336F, 36.34919F, 8.0F, 1.0F, 8.5F, new float[]{4.0F, 0.0F, 5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F}),
            new Part(Kind.LEG, 4, 0, 10.25F, -1.0F, 8.0F, 11.25F, 1.0F, 9.0F, 26.56507F, 37.76123F, 39.23157F, 8.25F, 1.0F, 8.5F, new float[]{0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 0.0F, 5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 1.0F, 4.0F, 3.0F}),
            new Part(Kind.LEG, 4, 0, 11.25F, 1.0F, 9.0F, 10.25F, -1.0F, 8.0F, 26.56507F, 37.76123F, 39.23157F, 8.25F, 1.0F, 8.5F, new float[]{4.0F, 0.0F, 5.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 3.0F, 3.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F}),
            new Part(Kind.LEG, 4, 1, 5.0F, -1.0F, 7.75F, 6.0F, 1.0F, 8.75F, 6.32526F, -10.80355F, -30.59873F, 8.0F, 1.0F, 8.25F, new float[]{0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 1.0F, 3.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F}),
            new Part(Kind.LEG, 4, 1, 6.0F, 1.0F, 8.75F, 5.0F, -1.0F, 7.75F, 6.32526F, -10.80355F, -30.59873F, 8.0F, 1.0F, 8.25F, new float[]{5.0F, 0.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 3.0F, 3.0F, 1.0F}),
            new Part(Kind.LEG, 4, 0, 4.75F, -1.0F, 8.0F, 5.75F, 1.0F, 9.0F, 26.56507F, -37.76123F, -39.23157F, 7.75F, 1.0F, 8.5F, new float[]{0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 1.0F, 3.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F}),
            new Part(Kind.LEG, 4, 0, 5.75F, 1.0F, 9.0F, 4.75F, -1.0F, 8.0F, 26.56507F, -37.76123F, -39.23157F, 7.75F, 1.0F, 8.5F, new float[]{5.0F, 0.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 3.0F, 3.0F, 1.0F}),
            new Part(Kind.LEG, 4, 2, 5.0F, -1.0F, 8.25F, 6.0F, 1.0F, 9.25F, -3.76613F, 6.49053F, -30.21361F, 8.0F, 1.0F, 8.75F, new float[]{0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 1.0F, 3.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F}),
            new Part(Kind.LEG, 4, 2, 6.0F, 1.0F, 9.25F, 5.0F, -1.0F, 8.25F, -3.76613F, 6.49053F, -30.21361F, 8.0F, 1.0F, 8.75F, new float[]{5.0F, 0.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 3.0F, 3.0F, 1.0F}),
            new Part(Kind.LEG, 4, 3, 5.0F, -1.0F, 8.0F, 6.0F, 1.0F, 9.0F, -13.75825F, 36.4336F, -36.34919F, 8.0F, 1.0F, 8.5F, new float[]{0.0F, 0.0F, 0.0F, 0.0F, 5.0F, 0.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 1.0F, 3.0F, 3.0F, 0.0F, 0.0F, 0.0F, 0.0F}),
            new Part(Kind.LEG, 4, 3, 6.0F, 1.0F, 9.0F, 5.0F, -1.0F, 8.0F, -13.75825F, 36.4336F, -36.34919F, 8.0F, 1.0F, 8.5F, new float[]{5.0F, 0.0F, 4.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 4.0F, 3.0F, 3.0F, 1.0F})
    };

    private CrabMesh() {
    }

    static void submit(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CrabRenderState state,
            int light,
            int overlay,
            int color
    ) {
        // The roll turns the belly toward the wall. The sideways shift is applied after that roll.
        float roll = state.climbBlend * (state.leftLegsUp ? Mth.HALF_PI : -Mth.HALF_PI);
        float raise = clawRaise(state);
        float pivot = state.isBaby ? 2.5F : 3.5F;
        float intoWall = state.climbBlend * ((state.isBaby ? 1.5F : 3.25F) / 16.0F);

        poseStack.pushPose();
        poseStack.translate(state.leftLegsUp ? intoWall : -intoWall, 0.0F, 0.0F);
        poseStack.translate(0.0F, pivot / 16.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotation(roll));
        poseStack.translate(0.0F, -pivot / 16.0F, 0.0F);
        poseStack.translate(-0.5F, 0.0F, -0.5F);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        for (Part part : state.isBaby ? BABY_PARTS : PARTS) {
            if (part.kind == Kind.BIG_CLAW && !state.hasClaw) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(part.ox, part.oy, part.oz);
            if (part.kind == Kind.BIG_CLAW) {
                poseStack.mulPose(Axis.YP.rotation(clawWag(state)));
                poseStack.mulPose(Axis.XP.rotation(raise * 1.15F));
            }
            if (part.rx != 0.0F || part.ry != 0.0F || part.rz != 0.0F) {
                // Blockbench java models use ZYX Euler order.
                poseStack.mulPose(Axis.ZP.rotationDegrees(part.rz));
                poseStack.mulPose(Axis.YP.rotationDegrees(part.ry));
                poseStack.mulPose(Axis.XP.rotationDegrees(part.rx));
            }
            if (part.kind == Kind.LEG) {
                poseLeg(poseStack, part, state);
            } else if (part.kind == Kind.SMALL_CLAW && state.waveTicks == 0 && state.climbBlend < 0.2F) {
                poseStack.mulPose(Axis.YP.rotation(Mth.sin(state.ageInTicks * 0.08F) * 0.08F));
            } else if (part.kind == Kind.EYE && state.waveTicks == 0 && state.climbBlend < 0.2F) {
                poseStack.mulPose(Axis.YP.rotationDegrees(state.yRot * 0.15F));
            }
            poseStack.translate(-part.ox, -part.oy, -part.oz);
            draw(poseStack, collector, part, light, overlay, color);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static float clawRaise(CrabRenderState state) {
        if (state.waveTicks > 0 && state.climbBlend < 0.2F && !state.aggravated) {
            float progress = 1.0F - state.waveTicks / Crab.WAVE_TICKS;
            return Mth.sin(progress * (float) Math.PI);
        }
        if (state.aggravated && state.hasClaw) {
            return 1.0F;
        }
        return 0.0F;
    }

    /** Side-to-side of the big claw, in radians, around its shoulder. */
    private static float clawWag(CrabRenderState state) {
        if (state.waveTicks <= 0 || state.aggravated || state.climbBlend >= 0.2F) {
            return 0.0F;
        }
        float progress = 1.0F - state.waveTicks / Crab.WAVE_TICKS;
        float envelope = Mth.sin(progress * (float) Math.PI);
        return Mth.sin(progress * (float) Math.PI * 2.0F) * envelope * 0.45F;
    }

    private static void poseLeg(PoseStack poseStack, Part part, CrabRenderState state) {
        float speed = state.walkAnimationSpeed;
        if (speed < 0.01F) {
            return;
        }
        boolean left = (part.x0 + part.x1) < 16.0F;
        int rank = state.isBaby ? part.leg : Math.round((part.oz - 6.0F) / 1.6F);
        float phase = state.walkAnimationPos * 1.15F + rank * Mth.PI + (left ? Mth.PI : 0.0F);
        if (state.sidewaysGait) {
            float reach = Mth.cos(phase) * 0.85F * speed;
            float lift = Math.max(0.0F, Mth.sin(phase)) * 0.65F * speed;
            if (left) {
                reach = -reach;
                lift = -lift;
            }
            poseStack.mulPose(Axis.ZP.rotation(reach));
            poseStack.mulPose(Axis.XP.rotation(lift));
            return;
        }
        float swing = Mth.cos(phase) * 0.4F * speed;
        float lift = Math.max(0.0F, Mth.sin(phase)) * 0.32F * speed;
        if (left) {
            swing = -swing;
            lift = -lift;
        }
        poseStack.mulPose(Axis.YP.rotation(swing));
        poseStack.mulPose(Axis.ZP.rotation(lift));
    }

    static boolean babyLegsHaveInnerFaces() {
        int legs = 0;
        for (int index = 0; index < BABY_PARTS.length; index++) {
            Part outer = BABY_PARTS[index];
            if (outer.kind != Kind.LEG || outer.x0 > outer.x1) {
                continue;
            }
            legs++;
            if (index + 1 >= BABY_PARTS.length) {
                return false;
            }
            Part inner = BABY_PARTS[index + 1];
            if (inner.kind != Kind.LEG
                    || inner.x0 != outer.x1
                    || inner.y0 != outer.y1
                    || inner.z0 != outer.z1
                    || inner.x1 != outer.x0
                    || inner.y1 != outer.y0
                    || inner.z1 != outer.z0
                    || inner.rx != outer.rx
                    || inner.ry != outer.ry
                    || inner.rz != outer.rz
                    || inner.leg != outer.leg) {
                return false;
            }
            for (int channel = 0; channel < 4; channel++) {
                if (inner.uvs[channel] != outer.uvs[4 + channel]) {
                    return false;
                }
            }
        }
        return legs == 8;
    }

    private static void draw(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            Part part,
            int light,
            int overlay,
            int color
    ) {
        boolean translucent = part.texture == 0 || part.texture == 2 || part.texture == 3 || part.texture == 5;
        RenderType type = translucent
                ? RenderTypes.entityTranslucent(TEXTURES[part.texture])
                : RenderTypes.entityCutout(TEXTURES[part.texture]);
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> emit(part, pose, buffer, light, overlay, color)
        );
    }

    private static void emit(Part part, PoseStack.Pose pose, VertexConsumer buffer, int light, int overlay, int color) {
        for (Direction direction : Direction.values()) {
            if (isFlat(direction, part)) {
                continue;
            }
            FaceInfo info = FaceInfo.fromFacing(direction);
            int base = direction.ordinal() * 4;
            float u0 = part.uvs[base] / UV_RESOLUTION;
            float v0 = part.uvs[base + 1] / UV_RESOLUTION;
            float u1 = part.uvs[base + 2] / UV_RESOLUTION;
            float v1 = part.uvs[base + 3] / UV_RESOLUTION;
            if (u0 == u1 && v0 == v1) {
                continue;
            }
            float[] us = {u0, u0, u1, u1};
            float[] vs = {v0, v1, v1, v0};
            float nx = direction.getStepX();
            float ny = direction.getStepY();
            float nz = direction.getStepZ();
            for (int vertex = 0; vertex < 4; vertex++) {
                FaceInfo.VertexInfo infoVertex = info.getVertexInfo(vertex);
                float x = infoVertex.xFace().select(part.x0, part.y0, part.z0, part.x1, part.y1, part.z1);
                float y = infoVertex.yFace().select(part.x0, part.y0, part.z0, part.x1, part.y1, part.z1);
                float z = infoVertex.zFace().select(part.x0, part.y0, part.z0, part.x1, part.y1, part.z1);
                buffer.addVertex(pose, x, y, z)
                        .setColor(color)
                        .setUv(us[vertex], vs[vertex])
                        .setOverlay(overlay)
                        .setLight(light)
                        .setNormal(pose, nx, ny, nz);
            }
        }
    }

    private static boolean isFlat(Direction direction, Part part) {
        return switch (direction) {
            case DOWN, UP -> part.x0 == part.x1 || part.z0 == part.z1;
            case NORTH, SOUTH -> part.x0 == part.x1 || part.y0 == part.y1;
            case WEST, EAST -> part.y0 == part.y1 || part.z0 == part.z1;
        };
    }
}
