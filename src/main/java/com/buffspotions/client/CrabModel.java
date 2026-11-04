package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.Identifier;

/**
 * Placeholder layer. The visible crab is {@link CrabMesh}, which keeps the
 * Blockbench face coordinates the entity box format cannot store.
 */
public class CrabModel extends EntityModel<CrabRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "crab"),
            "main"
    );

    public CrabModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(new MeshDefinition(), 16, 16);
    }

    @Override
    public void setupAnim(CrabRenderState state) {
        super.setupAnim(state);
    }
}
