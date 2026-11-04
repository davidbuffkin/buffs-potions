package com.buffspotions.client;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public final class BrewingStackRenderState extends BlockEntityRenderState {
    public boolean hasFlagon;
    public boolean anchored;
    public int doses;
    public int fluidColor;
    public int fluidBottom;
    public Direction facing = Direction.SOUTH;
}
