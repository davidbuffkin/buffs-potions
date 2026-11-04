package com.buffspotions.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.Direction;

public class CrabRenderState extends LivingEntityRenderState {
    public boolean hasClaw = true;
    public boolean aggravated;
    public int waveTicks;
    public boolean climbing;
    /** 0 on the floor, 1 fully rolled onto a wall. */
    public float climbBlend;
    public Direction wallFacing = Direction.NORTH;
    /** Which horizontal side the wall is on, used to roll the belly against it. */
    public boolean leftLegsUp;
    /** True when the crab is strafing or climbing, so the legs step sideways. */
    public boolean sidewaysGait;
}
