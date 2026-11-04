package com.buffspotions.cauldron;

import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.event.RegisterCauldronInteractionEvent;

public final class CauldronFills {
    private static final Identifier WATER = Identifier.withDefaultNamespace("water");

    private CauldronFills() {
    }

    public static void register(RegisterCauldronInteractionEvent.Interaction event) {
        event.register(WATER, Items.GLASS_BOTTLE, CauldronFills::fillBottle);
        event.registerToAll(Items.POTION, CauldronFills::usePotion);
        event.registerToAll(Items.SPLASH_POTION, CauldronFills::emptyPotion);
        event.registerToAll(Items.LINGERING_POTION, CauldronFills::emptyPotion);
        event.registerToAll(ModItems.FLAGON.get(), CauldronFills::useFlagon);
        event.registerToAll(ModItems.ENDER_FLAGON.get(), CauldronFills::useEnderFlagon);
        event.registerToAll(ModItems.ANCHORED_ENDER_FLAGON.get(), CauldronFills::useAnchoredFlagon);
    }

    private static InteractionResult fillBottle(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held
    ) {
        if (!level.isClientSide()) {
            player.setItemInHand(
                    hand,
                    ItemUtils.createFilledResult(held, player, PotionContents.createItemStack(Items.POTION, Potions.WATER))
            );
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            filledFromCauldron(player);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult usePotion(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held
    ) {
        PotionContents contents = held.get(DataComponents.POTION_CONTENTS);
        if (contents != null && contents.is(Potions.WATER)) {
            return pourWaterBottle(state, level, pos, player, hand, held);
        }
        return emptyPotion(state, level, pos, player, hand, held);
    }

    private static InteractionResult pourWaterBottle(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held
    ) {
        BlockState filled = filledWater(state);
        if (filled == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(Items.POTION));
            level.setBlockAndUpdate(pos, filled);
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult emptyPotion(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held
    ) {
        if (held.get(DataComponents.POTION_CONTENTS) == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
            player.awardStat(Stats.USE_CAULDRON);
            player.awardStat(Stats.ITEM_USED.get(held.getItem()));
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult useFlagon(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held
    ) {
        FlagonContents contents = FlagonContents.of(held);
        if (state.is(Blocks.WATER_CAULDRON) && contents.canFillFromWater()) {
            boolean empty = contents.isEmpty();
            if (!level.isClientSide()) {
                exchange(player, hand, held, contents.fillWithWater());
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (empty) {
                    filledFromCauldron(player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (contents.isWater() && contents.doses() >= FlagonContents.BUCKET_DOSES) {
            BlockState filled = fullWater(state);
            if (filled != null) {
                if (!level.isClientSide()) {
                    exchange(player, hand, held, contents.remove(FlagonContents.BUCKET_DOSES));
                    level.setBlockAndUpdate(pos, filled);
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (!contents.isEmpty() && !contents.isWater()) {
            if (!level.isClientSide()) {
                exchange(player, hand, held, FlagonContents.EMPTY);
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static InteractionResult useEnderFlagon(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held
    ) {
        FlagonContents contents = EnderFlagons.of(player);
        if (state.is(Blocks.WATER_CAULDRON) && contents.canFillFromWater()) {
            boolean empty = contents.isEmpty();
            if (!level.isClientSide()) {
                keepHeld(player, held, contents.fillWithWater());
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (empty) {
                    filledFromCauldron(player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (contents.isWater() && contents.doses() >= FlagonContents.BUCKET_DOSES) {
            BlockState filled = fullWater(state);
            if (filled != null) {
                if (!level.isClientSide()) {
                    keepHeld(player, held, contents.remove(FlagonContents.BUCKET_DOSES));
                    level.setBlockAndUpdate(pos, filled);
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (!contents.isEmpty() && !contents.isWater()) {
            if (!level.isClientSide()) {
                keepHeld(player, held, FlagonContents.EMPTY);
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static InteractionResult useAnchoredFlagon(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack held
    ) {
        if (EnderFlagons.anchor(held) == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        FlagonContents contents = level instanceof net.minecraft.server.level.ServerLevel server
                ? EnderFlagons.live(server, held)
                : FlagonContents.of(held);
        if (state.is(Blocks.WATER_CAULDRON) && contents.canFillFromWater()) {
            boolean empty = contents.isEmpty();
            if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel server) {
                EnderFlagons.store(server, held, contents.fillWithWater());
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(held.getItem()));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (empty) {
                    filledFromCauldron(player);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (contents.isWater() && contents.doses() >= FlagonContents.BUCKET_DOSES) {
            BlockState filled = fullWater(state);
            if (filled != null) {
                if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel server) {
                    EnderFlagons.store(server, held, contents.remove(FlagonContents.BUCKET_DOSES));
                    player.awardStat(Stats.USE_CAULDRON);
                    player.awardStat(Stats.ITEM_USED.get(held.getItem()));
                    level.setBlockAndUpdate(pos, filled);
                    level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.gameEvent(null, GameEvent.FLUID_PLACE, pos);
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (!contents.isEmpty() && !contents.isWater()) {
            if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel server) {
                EnderFlagons.store(server, held, FlagonContents.EMPTY);
                player.awardStat(Stats.USE_CAULDRON);
                player.awardStat(Stats.ITEM_USED.get(held.getItem()));
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static void filledFromCauldron(Player player) {
        if (player instanceof ServerPlayer server) {
            ModCriteria.CAULDRON_WATER.get().trigger(server);
        }
    }

    private static void keepHeld(Player player, ItemStack held, FlagonContents contents) {
        EnderFlagons.set(player, contents);
        player.awardStat(Stats.USE_CAULDRON);
        player.awardStat(Stats.ITEM_USED.get(held.getItem()));
    }

    private static void exchange(Player player, InteractionHand hand, ItemStack held, FlagonContents contents) {
        ItemStack result = held.copyWithCount(1);
        result.set(ModDataComponents.FLAGON_CONTENTS.get(), contents);
        player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, result));
        player.awardStat(Stats.USE_CAULDRON);
        player.awardStat(Stats.ITEM_USED.get(held.getItem()));
    }

    private static BlockState filledWater(BlockState state) {
        if (state.is(Blocks.CAULDRON)) {
            return Blocks.WATER_CAULDRON.defaultBlockState();
        }
        if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) < 3) {
            return state.cycle(LayeredCauldronBlock.LEVEL);
        }
        return null;
    }

    private static BlockState fullWater(BlockState state) {
        if (state.is(Blocks.CAULDRON) || (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) < 3)) {
            return Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3);
        }
        return null;
    }
}
