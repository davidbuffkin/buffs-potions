package com.buffspotions.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BrewingStandBlock.class)
abstract class BrewingStandBlockMixin {
    @Inject(method = "animateTick", at = @At("TAIL"))
    private void buffsPotions$embers(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo callback) {
        if (!(level.getBlockEntity(pos) instanceof BrewingStandBlockEntity stand)
                || ((BrewingStandBellowsAccess) stand).buffsPotions$brewTime() <= 0) {
            return;
        }
        // Crying obsidian drips on one tick in five. The blaze rod sheds embers twice as often, and only while brewing.
        if (random.nextInt(2) != 0) {
            return;
        }
        boolean acrossX = random.nextBoolean();
        double outward = random.nextBoolean() ? 2.5 / 16.0 : -2.5 / 16.0;
        double along = (random.nextDouble() - 0.5) * (2.0 / 16.0);
        double x = pos.getX() + 0.5 + (acrossX ? outward : along);
        double z = pos.getZ() + 0.5 + (acrossX ? along : outward);
        level.addParticle(ParticleTypes.DRIPPING_LAVA, x, pos.getY() + 14.0 / 16.0, z, 0.0, 0.0, 0.0);
    }
}
