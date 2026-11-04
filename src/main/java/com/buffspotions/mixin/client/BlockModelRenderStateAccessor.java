package com.buffspotions.mixin.client;

import java.util.List;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockModelRenderState.class)
public interface BlockModelRenderStateAccessor {
    @Accessor("modelParts")
    @Nullable List<?> buffsPotions$modelParts();

    @Accessor("specialRenderer")
    @Nullable SpecialModelRenderer<?> buffsPotions$specialRenderer();

    @Accessor("specialRendererTransformation")
    @Nullable Matrix4fc buffsPotions$specialRendererTransformation();
}
