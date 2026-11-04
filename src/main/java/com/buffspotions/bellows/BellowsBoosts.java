package com.buffspotions.bellows;

import com.buffspotions.mixin.BrewingStandBellowsAccess;
import com.buffspotions.mixin.FurnaceBellowsAccess;
import com.buffspotions.registry.ModCriteria;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class BellowsBoosts {
    public static final int BOOST_TICKS = 40;
    public static final int COOLDOWN_TICKS = 20;
    /** One squeeze. Four of these spend one hunger point, after saturation is gone. */
    public static final float USE_EXHAUSTION = 0.5F;
    /**
     * Knockback I adds 1 to the attack attribute, and a hit divides that by 2
     * before calling {@link LivingEntity#knockback}.
     */
    public static final double KNOCKBACK = 0.5;

    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> UNTIL = new HashMap<>();

    private BellowsBoosts() {
    }

    public static boolean tryUse(Player player, Level level, BlockPos pos, ItemStack bellows) {
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        boolean stoked = stoke(server, pos);
        boolean oxidized = !stoked && oxidize(server, pos);
        boolean web = !stoked && !oxidized && clearCobweb(server, pos);
        if (!stoked && !oxidized && !web) {
            return false;
        }
        player.getCooldowns().addCooldown(bellows, COOLDOWN_TICKS);
        player.causeFoodExhaustion(USE_EXHAUSTION);
        if (stoked && player instanceof ServerPlayer serverPlayer) {
            ModCriteria.BELLOWS_BOOST.get().trigger(serverPlayer);
        }
        return true;
    }

    /** An unwaxed block that still has a later oxidation stage. */
    public static boolean canOxidize(BlockState state) {
        return nextStage(state) != null;
    }

    /** A block a player can blow on: oxidizable copper, or a cobweb. */
    public static boolean canUseOn(BlockState state) {
        return canOxidize(state) || state.is(Blocks.COBWEB);
    }

    /** The block is unwaxed copper and has not caught up to {@code than}. */
    public static boolean lessOxidizedThan(BlockState state, WeatheringCopper.WeatherState than) {
        if (!(state.getBlock() instanceof WeatheringCopper copper)) {
            return false;
        }
        return copper.getAge().ordinal() < than.ordinal();
    }

    public static boolean clearCobweb(ServerLevel level, BlockPos pos) {
        if (!level.getBlockState(pos).is(Blocks.COBWEB)) {
            return false;
        }
        if (!level.destroyBlock(pos, true)) {
            return false;
        }
        burst(level, pos, false, 1.0F);
        return true;
    }

    /** Pushes a mob the way a Knockback I hit does, without hurting it. */
    public static void blow(Player player, LivingEntity target) {
        target.knockback(
                KNOCKBACK,
                Mth.sin(player.getYRot() * (float) (Math.PI / 180.0)),
                -Mth.cos(player.getYRot() * (float) (Math.PI / 180.0)),
                player.damageSources().playerAttack(player),
                0.0F
        );
        if (player.level() instanceof ServerLevel level) {
            level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.4F, 1.35F);
            level.sendParticles(ParticleTypes.POOF, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 7, 0.2, 0.15, 0.2, 0.01);
        }
    }

    public static boolean oxidize(ServerLevel level, BlockPos pos) {
        return oxidize(level, pos, 1.0F);
    }

    public static boolean oxidize(ServerLevel level, BlockPos pos, float volumeScale) {
        BlockState next = nextStage(level.getBlockState(pos));
        if (next == null) {
            return false;
        }
        level.setBlockAndUpdate(pos, next);
        burst(level, pos, false, volumeScale);
        return true;
    }

    private static BlockState nextStage(BlockState state) {
        return WeatheringCopper.getNext(state.getBlock()).map(block -> block.withPropertiesOf(state)).orElse(null);
    }

    public static boolean stoke(ServerLevel level, BlockPos pos) {
        return stoke(level, pos, 1.0F);
    }

    public static boolean stoke(ServerLevel level, BlockPos pos, float volumeScale) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!isWorking(blockEntity)) {
            return false;
        }
        UNTIL.computeIfAbsent(level.dimension(), key -> new HashMap<>()).put(pos.immutable(), level.getGameTime() + BOOST_TICKS);
        burst(level, pos, blockEntity instanceof AbstractFurnaceBlockEntity, volumeScale);
        return true;
    }

    public static boolean isWorking(BlockEntity blockEntity) {
        if (blockEntity instanceof BellowsTarget target) {
            return target.isWorking();
        }
        if (blockEntity instanceof BrewingStandBlockEntity stand) {
            return ((BrewingStandBellowsAccess) stand).buffsPotions$brewTime() > 0;
        }
        if (blockEntity instanceof AbstractFurnaceBlockEntity furnace) {
            FurnaceBellowsAccess access = (FurnaceBellowsAccess) furnace;
            return access.buffsPotions$litTimeRemaining() > 0 && access.buffsPotions$cookingTimer() > 0;
        }
        return false;
    }

    public static boolean isActive(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return false;
        }
        Map<BlockPos, Long> boosts = UNTIL.get(level.dimension());
        if (boosts == null || boosts.isEmpty()) {
            return false;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Long>> entries = boosts.entrySet().iterator();
        boolean active = false;
        while (entries.hasNext()) {
            Map.Entry<BlockPos, Long> entry = entries.next();
            if (now >= entry.getValue()) {
                entries.remove();
            } else if (entry.getKey().equals(pos)) {
                active = true;
            }
        }
        return active;
    }

    public static void trickle(ServerLevel level, BlockPos pos) {
        level.sendParticles(ParticleTypes.POOF, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 1, 0.12, 0.04, 0.12, 0.0);
    }

    private static void burst(ServerLevel level, BlockPos pos, boolean flames, float volumeScale) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.65;
        double z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.POOF, x, y, z, 7, 0.18, 0.06, 0.18, 0.01);
        if (flames) {
            level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 3, 0.1, 0.04, 0.1, 0.0);
        }
        level.playSound(null, pos, SoundEvents.BREEZE_SHOOT, SoundSource.BLOCKS, 0.4F * volumeScale, 1.35F);
    }
}
