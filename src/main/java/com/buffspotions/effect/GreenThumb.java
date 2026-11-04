package com.buffspotions.effect;

import com.buffspotions.registry.ModEffects;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.TorchflowerCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.jspecify.annotations.Nullable;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

/**
 * Fully grown seed crops spend one seed to replant themselves. A wider set of mature harvests can drop one experience.
 */
public final class GreenThumb {
    private static final float[] CHANCES = {0.50F, 0.65F, 0.80F, 1.0F};
    /** Fruit positions whose attached stem was still present when the break began. */
    private static final Map<UUID, BlockPos> GROWN_FRUIT = new ConcurrentHashMap<>();
    /** Torchflowers whose bloom was recorded before this break removed them. */
    private static final Map<UUID, BlockPos> GROWN_TORCH = new ConcurrentHashMap<>();

    private GreenThumb() {
    }

    public static float chance(int amplifier) {
        int index = Math.max(0, amplifier);
        if (index >= CHANCES.length) {
            index = CHANCES.length - 1;
        }
        return CHANCES[index];
    }

    /**
     * Removing a melon or pumpkin detaches its stem before drops are decided, so the attachment has to be noted here.
     */
    public static void rememberFruit(BreakBlockEvent event) {
        if (event.isCanceled() || event.getLevel().isClientSide() || !(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getState().is(Blocks.TORCHFLOWER) && event.getLevel() instanceof ServerLevel server) {
            boolean grown = GrownTorchflowers.claim(server, event.getPos());
            if (grown && player.hasEffect(ModEffects.GREEN_THUMB)) {
                GROWN_TORCH.put(player.getUUID(), event.getPos().immutable());
            } else {
                GROWN_TORCH.remove(player.getUUID());
            }
        }
        if (!player.hasEffect(ModEffects.GREEN_THUMB)) {
            return;
        }
        Block stem = stemBlock(event.getState());
        if (stem == null) {
            return;
        }
        if (attachedStem(event.getLevel(), event.getPos(), stem)) {
            GROWN_FRUIT.put(player.getUUID(), event.getPos().immutable());
        } else {
            GROWN_FRUIT.remove(player.getUUID());
        }
    }

    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockState state = event.getState();
        BlockPos pos = event.getPos();
        boolean bonemealed = forgetBonemeal(level, state, pos);
        if (state.is(Blocks.TORCHFLOWER) && !(event.getBreaker() instanceof ServerPlayer)) {
            GrownTorchflowers.claim(level, pos);
            return;
        }
        if (!(event.getBreaker() instanceof ServerPlayer player)) {
            return;
        }
        MobEffectInstance effect = player.getEffect(ModEffects.GREEN_THUMB);
        if (effect == null) {
            return;
        }
        replant(level, pos, state, event.getDrops());
        boolean harvested = earnedExperience(player, state, level, pos);
        if (harvested && countsAsOneCrop(state)) {
            VermillionHarvest.note(player);
        }
        if (!bonemealed && harvested && player.getRandom().nextFloat() < chance(effect.getAmplifier())) {
            event.setDroppedExperience(event.getDroppedExperience() + 1);
        }
    }

    /**
     * A successful bonemeal application, from a player or a dispenser, taints this plant.
     * {@code fruitBefore} lists melon and pumpkin positions already beside a stem, so only fruit this application grew is marked.
     */
    public static void noteBonemeal(ServerLevel level, BlockPos pos, @Nullable BlockPos[] fruitBefore) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PitcherCropBlock) {
            BlockPos lower = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
            BonemealedCrops.mark(level, lower);
        } else if (state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof CocoaBlock
                || state.getBlock() instanceof SweetBerryBushBlock
                || state.is(Blocks.TORCHFLOWER)) {
            BonemealedCrops.mark(level, pos);
        }
        if (fruitBefore == null) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos side = pos.relative(direction);
            BlockState fruit = level.getBlockState(side);
            if ((fruit.is(Blocks.MELON) || fruit.is(Blocks.PUMPKIN)) && !contains(fruitBefore, side)) {
                BonemealedCrops.mark(level, side);
            }
        }
    }

    public static void noteTorchflower(CropGrowEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            noteTorchflower(level, event.getOriginalState(), event.getState(), event.getPos());
            clearSweetBerryBonemeal(level, event.getOriginalState(), event.getState(), event.getPos());
        }
    }

    /** A sweet berry that grows on its own is no longer a bonemealed harvest. */
    private static void clearSweetBerryBonemeal(ServerLevel level, BlockState before, BlockState after, BlockPos pos) {
        if (!(before.getBlock() instanceof SweetBerryBushBlock) || !(after.getBlock() instanceof SweetBerryBushBlock)) {
            return;
        }
        int grown = after.getValue(SweetBerryBushBlock.AGE);
        if (grown > 1 && grown > before.getValue(SweetBerryBushBlock.AGE)) {
            BonemealedCrops.claim(level, pos);
        }
    }

    public static void noteTorchflower(ServerLevel level, BlockState before, BlockState after, BlockPos pos) {
        if (before.getBlock() instanceof TorchflowerCropBlock && after.is(Blocks.TORCHFLOWER)) {
            GrownTorchflowers.mark(level, pos);
        }
    }

    public static void preventTrample(BlockEvent.FarmlandTrampleEvent event) {
        if (event.getEntity() instanceof Player player && player.hasEffect(ModEffects.GREEN_THUMB)) {
            event.setCanceled(true);
        }
    }

    public static void onRightClickHarvest(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        VermillionHarvest.note(player);
        if (BonemealedCrops.marked(level, pos)) {
            return;
        }
        MobEffectInstance effect = player.getEffect(ModEffects.GREEN_THUMB);
        if (effect == null || player.getRandom().nextFloat() >= chance(effect.getAmplifier())) {
            return;
        }
        state.getBlock().popExperience(level, pos, 1);
    }

    /** Wheat, beetroot, carrot, potato, nether wart, and cocoa that are not fully grown. */
    public static boolean immatureSeedCrop(BlockState state) {
        Block block = state.getBlock();
        if (block != Blocks.WHEAT
                && block != Blocks.BEETROOTS
                && block != Blocks.CARROTS
                && block != Blocks.POTATOES
                && block != Blocks.NETHER_WART
                && block != Blocks.COCOA) {
            return false;
        }
        return !replants(state);
    }

    static boolean replants(BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.WHEAT || block == Blocks.BEETROOTS || block == Blocks.CARROTS || block == Blocks.POTATOES) {
            return ((CropBlock) block).isMaxAge(state);
        }
        if (block == Blocks.NETHER_WART) {
            return state.getValue(NetherWartBlock.AGE) == NetherWartBlock.MAX_AGE;
        }
        if (block == Blocks.COCOA) {
            return state.getValue(CocoaBlock.AGE) == CocoaBlock.MAX_AGE;
        }
        return false;
    }

    /** A pitcher plant is one crop, so only its lower half is counted. */
    private static boolean countsAsOneCrop(BlockState state) {
        return !(state.getBlock() instanceof PitcherCropBlock)
                || state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER;
    }

    static boolean grantsExperience(BlockState state, Level level, BlockPos pos) {
        if (replants(state)) {
            return true;
        }
        Block block = state.getBlock();
        if (block instanceof PitcherCropBlock) {
            return state.getValue(PitcherCropBlock.AGE) == PitcherCropBlock.MAX_AGE;
        }
        return false;
    }

    private static boolean forgetBonemeal(ServerLevel level, BlockState state, BlockPos pos) {
        if (!remembersBonemeal(state)) {
            return false;
        }
        boolean marked = BonemealedCrops.claim(level, pos);
        if (state.getBlock() instanceof PitcherCropBlock) {
            BlockPos other = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            marked |= BonemealedCrops.claim(level, other);
        }
        return marked;
    }

    private static boolean remembersBonemeal(BlockState state) {
        Block block = state.getBlock();
        return block instanceof CropBlock
                || block instanceof CocoaBlock
                || block instanceof SweetBerryBushBlock
                || block instanceof PitcherCropBlock
                || state.is(Blocks.TORCHFLOWER)
                || state.is(Blocks.MELON)
                || state.is(Blocks.PUMPKIN);
    }

    private static boolean contains(BlockPos[] positions, BlockPos pos) {
        for (BlockPos position : positions) {
            if (pos.equals(position)) {
                return true;
            }
        }
        return false;
    }

    private static boolean earnedExperience(ServerPlayer player, BlockState state, ServerLevel level, BlockPos pos) {
        if (state.is(Blocks.TORCHFLOWER)) {
            BlockPos grown = GROWN_TORCH.remove(player.getUUID());
            return (grown != null && grown.equals(pos)) || GrownTorchflowers.claim(level, pos);
        }
        Block stem = stemBlock(state);
        if (stem == null) {
            return grantsExperience(state, level, pos);
        }
        BlockPos grown = GROWN_FRUIT.remove(player.getUUID());
        return grown != null && grown.equals(pos);
    }

    private static Block stemBlock(BlockState state) {
        if (state.is(Blocks.MELON)) {
            return Blocks.ATTACHED_MELON_STEM;
        }
        if (state.is(Blocks.PUMPKIN)) {
            return Blocks.ATTACHED_PUMPKIN_STEM;
        }
        return null;
    }

    private static void replant(ServerLevel level, BlockPos pos, BlockState harvested, List<ItemEntity> drops) {
        if (!replants(harvested)) {
            return;
        }
        BlockState planted = planted(harvested);
        BlockState there = level.getBlockState(pos);
        if (planted == null || (!there.isAir() && !there.canBeReplaced()) || !planted.canSurvive(level, pos)) {
            return;
        }
        if (!takeOne(drops, seedOf(harvested.getBlock()))) {
            return;
        }
        level.setBlock(pos, planted, Block.UPDATE_ALL);
    }

    private static BlockState planted(BlockState harvested) {
        Block block = harvested.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.getStateForAge(0);
        }
        if (block == Blocks.NETHER_WART) {
            return harvested.setValue(NetherWartBlock.AGE, 0);
        }
        if (block == Blocks.COCOA) {
            return harvested.setValue(CocoaBlock.AGE, 0);
        }
        return null;
    }

    private static Item seedOf(Block block) {
        if (block == Blocks.WHEAT) {
            return Items.WHEAT_SEEDS;
        }
        if (block == Blocks.BEETROOTS) {
            return Items.BEETROOT_SEEDS;
        }
        if (block == Blocks.CARROTS) {
            return Items.CARROT;
        }
        if (block == Blocks.POTATOES) {
            return Items.POTATO;
        }
        if (block == Blocks.NETHER_WART) {
            return Items.NETHER_WART;
        }
        if (block == Blocks.COCOA) {
            return Items.COCOA_BEANS;
        }
        return Items.AIR;
    }

    private static boolean takeOne(List<ItemEntity> drops, Item item) {
        if (item == Items.AIR) {
            return false;
        }
        Iterator<ItemEntity> iterator = drops.iterator();
        while (iterator.hasNext()) {
            ItemEntity entity = iterator.next();
            ItemStack stack = entity.getItem();
            if (!stack.is(item) || stack.isEmpty()) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                iterator.remove();
            } else {
                entity.setItem(stack);
            }
            return true;
        }
        return false;
    }

    private static boolean attachedStem(BlockGetter level, BlockPos fruit, Block stemBlock) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockState stem = level.getBlockState(fruit.relative(direction));
            if (stem.is(stemBlock) && stem.getValue(HorizontalDirectionalBlock.FACING) == direction.getOpposite()) {
                return true;
            }
        }
        return false;
    }
}
