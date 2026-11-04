package com.buffspotions.mixin.client;

import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChestSpecialRenderer.class)
public interface ChestSpecialRendererAccess {
    @Accessor("model")
    ChestModel buffsPotions$model();

    @Accessor("sprite")
    SpriteId buffsPotions$sprite();

    @Accessor("sprites")
    SpriteGetter buffsPotions$sprites();
}
