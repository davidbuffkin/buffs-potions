package com.buffspotions.client;

import com.buffspotions.flagon.FlagonBlock;
import com.buffspotions.flagon.FlagonBlockEntity;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.Remedies;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.Direction;

public final class FlagonRenderState extends BlockEntityRenderState {
    public Direction facing = Direction.NORTH;
    public int doses;
    public int fluidColor;
    public int fluidBottom;

    public static void extract(FlagonBlockEntity blockEntity, FlagonRenderState state) {
        state.facing = blockEntity.getBlockState().getValue(FlagonBlock.FACING);
        FlagonContents contents = blockEntity.contents();
        state.doses = contents.doses();
        paint(contents, state);
    }

    public static void paint(FlagonContents contents, FlagonRenderState state) {
        state.fluidColor = 0xE0000000 | (contents.fluidColor() & 0xFFFFFF);
        state.fluidBottom = Remedies.isRemedy(contents.dose()) ? 0xE0FFFFFF : state.fluidColor;
    }
}
