package com.buffspotions.effect;

import com.buffspotions.config.BuffsPotionsConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class BloomfootedEffect extends BuffMobEffect {
    public static final double INTERESTING_CHANCE = 0.15;

    private static final Map<LivingEntity, BlockPos> LAST_GROUND = new WeakHashMap<>();
    private static final List<Block> SMALL_FLOWERS = List.of(
            Blocks.DANDELION,
            Blocks.POPPY,
            Blocks.BLUE_ORCHID,
            Blocks.ALLIUM,
            Blocks.AZURE_BLUET,
            Blocks.RED_TULIP,
            Blocks.ORANGE_TULIP,
            Blocks.WHITE_TULIP,
            Blocks.PINK_TULIP,
            Blocks.OXEYE_DAISY,
            Blocks.CORNFLOWER,
            Blocks.LILY_OF_THE_VALLEY,
            Blocks.GOLDEN_DANDELION
    );
    private static final List<Block> INTERESTING_SPROUTS = List.of(
            Blocks.SUNFLOWER,
            Blocks.LILAC,
            Blocks.ROSE_BUSH,
            Blocks.PEONY,
            Blocks.PITCHER_PLANT,
            Blocks.TALL_GRASS,
            Blocks.LARGE_FERN,
            Blocks.TALL_DRY_GRASS,
            Blocks.OAK_SAPLING,
            Blocks.SPRUCE_SAPLING,
            Blocks.BIRCH_SAPLING,
            Blocks.JUNGLE_SAPLING,
            Blocks.ACACIA_SAPLING,
            Blocks.DARK_OAK_SAPLING,
            Blocks.CHERRY_SAPLING,
            Blocks.PALE_OAK_SAPLING,
            Blocks.RED_MUSHROOM,
            Blocks.BROWN_MUSHROOM,
            Blocks.FERN,
            Blocks.BUSH,
            Blocks.FIREFLY_BUSH,
            Blocks.FLOWERING_AZALEA,
            Blocks.WITHER_ROSE,
            Blocks.OPEN_EYEBLOSSOM,
            Blocks.CLOSED_EYEBLOSSOM
    );

    public BloomfootedEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    public static double sproutChance(int amplifier) {
        return Math.min(1.0, BuffsPotionsConfig.flowerSproutChance() * (Math.max(0, amplifier) + 1));
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplification) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplification) {
        sprinklePetals(level, mob, amplification);
        BlockPos ground = mob.blockPosition().below();
        BlockPos previous = LAST_GROUND.put(mob, ground);
        if (previous == null || previous.equals(ground)) {
            return true;
        }
        BlockState groundState = level.getBlockState(ground);
        if (groundState.is(Blocks.GRASS_BLOCK)) {
            if (mob.getRandom().nextDouble() < sproutChance(amplification) && sprout(level, mob, ground.above())) {
                celebrate(level, mob);
            }
        } else if (groundState.is(Blocks.FARMLAND)) {
            if (mob.getRandom().nextDouble() < sproutChance(amplification) && growCrop(level, ground.above())) {
                celebrate(level, mob);
            }
        }
        return true;
    }

    @Override
    public void onMobRemoved(ServerLevel level, LivingEntity mob, int amplifier, Entity.RemovalReason reason) {
        LAST_GROUND.remove(mob);
    }

    private static void sprinklePetals(ServerLevel level, LivingEntity mob, int amplification) {
        int potionLevel = Math.max(1, amplification + 1);
        if (mob.getRandom().nextFloat() >= potionLevel / 20.0F) {
            return;
        }
        level.sendParticles(
                ParticleTypes.CHERRY_LEAVES,
                mob.getX(),
                mob.getY() + mob.getBbHeight() * 0.55,
                mob.getZ(),
                1,
                0.2,
                0.1,
                0.2,
                0.0
        );
    }

    private static boolean sprout(ServerLevel level, LivingEntity mob, BlockPos pos) {
        Block interesting = INTERESTING_SPROUTS.get(mob.getRandom().nextInt(INTERESTING_SPROUTS.size()));
        Block ordinary = SMALL_FLOWERS.get(mob.getRandom().nextInt(SMALL_FLOWERS.size()));
        if (mob.getRandom().nextDouble() < BuffsPotionsConfig.flowerInterestingChance() && place(level, pos, interesting)) {
            return true;
        }
        return place(level, pos, ordinary);
    }

    private static boolean place(ServerLevel level, BlockPos pos, Block block) {
        BlockState state = block.defaultBlockState();
        if (!state.canSurvive(level, pos)) {
            return false;
        }
        if (block instanceof DoublePlantBlock) {
            if (!canReplace(level.getBlockState(pos)) || !canReplace(level.getBlockState(pos.above()))) {
                return false;
            }
            DoublePlantBlock.placeAt(level, state, pos, Block.UPDATE_ALL);
            return true;
        }
        if (!canReplace(level.getBlockState(pos))) {
            return false;
        }
        level.setBlock(pos, state, Block.UPDATE_ALL);
        return true;
    }

    private static boolean canReplace(BlockState state) {
        return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty() && !(state.getBlock() instanceof DoublePlantBlock));
    }

    private static boolean growCrop(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state)) {
            return false;
        }
        level.setBlock(pos, crop.getStateForAge(crop.getAge(state) + 1), Block.UPDATE_ALL);
        return true;
    }

    private static void celebrate(ServerLevel level, LivingEntity mob) {
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(), SoundEvents.BONE_MEAL_USE, SoundSource.PLAYERS, 0.7F, 1.0F);
        level.sendParticles(
                ParticleTypes.HAPPY_VILLAGER,
                mob.getX(),
                mob.getY() + 0.1,
                mob.getZ(),
                6,
                0.35,
                0.15,
                0.35,
                0.01
        );
    }
}
