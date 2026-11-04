package com.buffspotions.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface ItemStackRenderStateAccess {
    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] buffsPotions$layers();

    @Accessor("activeLayerCount")
    int buffsPotions$activeLayerCount();
}
