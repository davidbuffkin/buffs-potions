package com.buffspotions.flagon;

import com.buffspotions.effect.Formation;
import com.buffspotions.effect.Incarnation;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class FlagonInteractions {
    private FlagonInteractions() {
    }

    public static InteractionResult useItem(FlagonBlockEntity flagon, Player player, InteractionHand hand) {
        return useStored(flagon.getBlockPos(), player, hand, flagon.contents(), flagon::setContents, true);
    }

    public static InteractionResult useEnder(BlockPos pos, Player player, InteractionHand hand) {
        return useStored(pos, player, hand, EnderFlagons.of(player), contents -> EnderFlagons.set(player, contents), true);
    }

    public static InteractionResult useAnchored(AnchoredEnderFlagonBlockEntity flagon, Player player, InteractionHand hand) {
        FlagonContents contents = flagon.contents();
        if (player.level() instanceof net.minecraft.server.level.ServerLevel server && flagon.playerId() != null) {
            contents = EnderFlagons.contents(server.getServer(), flagon.playerId());
        }
        return useStored(flagon.getBlockPos(), player, hand, contents, flagon::setContents, true);
    }

    private static InteractionResult useStored(
            BlockPos pos,
            Player player,
            InteractionHand hand,
            FlagonContents contents,
            Consumer<FlagonContents> set,
            boolean allowPour
    ) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof FlagonItem flagon) {
            if (!allowPour) {
                return InteractionResult.PASS;
            }
            return pour(pos, player, held, flagon, contents, set);
        }
        if (held.is(Items.GLASS_BOTTLE) && !contents.isEmpty()) {
            return takeDose(pos, player, hand, held, contents, set);
        }
        if (FlagonContents.isDoseItem(held) && contents.canInsertDose(held)) {
            return addDose(pos, player, hand, held, contents, set);
        }
        return drink(pos, player, contents, set);
    }

    private static InteractionResult addDose(
            BlockPos pos, Player player, InteractionHand hand, ItemStack held, FlagonContents contents, Consumer<FlagonContents> set
    ) {
        if (!player.level().isClientSide()) {
            set.accept(contents.insertDose(held));
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
            play(pos, player, SoundEvents.BOTTLE_EMPTY, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult takeDose(
            BlockPos pos, Player player, InteractionHand hand, ItemStack held, FlagonContents contents, Consumer<FlagonContents> set
    ) {
        if (!player.level().isClientSide()) {
            set.accept(contents.removeDose());
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, contents.bottledDose()));
            play(pos, player, SoundEvents.BOTTLE_FILL, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult drink(
            BlockPos pos,
            Player player,
            FlagonContents contents,
            Consumer<FlagonContents> set
    ) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        Level level = player.level();
        if (level.isClientSide()) {
            FlagonDrinkLatch.arm();
        }
        if (contents.isEmpty()) {
            if (!level.isClientSide()) {
                level.playSound(null, pos, SoundEvents.DECORATED_POT_INSERT_FAIL, SoundSource.BLOCKS, 0.5F, 1.1F);
            }
            return InteractionResult.CONSUME;
        }
        if (!level.isClientSide()) {
            Incarnation.at(Vec3.atBottomCenterOf(pos.above()));
            Formation.atFlagon(pos.above());
            try {
                applyDose(player, contents.dose());
            } finally {
                Incarnation.clearAt();
                Formation.clearFlagon();
            }
            if (!player.hasInfiniteMaterials()) {
                set.accept(contents.removeDose());
                pulseObservers(level, pos);
            }
            level.playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    contents.isHoney() ? SoundEvents.HONEY_DRINK : SoundEvents.GENERIC_DRINK,
                    SoundSource.PLAYERS,
                    0.5F,
                    1.0F
            );
            if (player instanceof ServerPlayer server) {
                ModCriteria.FLAGON_PLACED_DRINK.get().trigger(server);
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static void applyDose(Player player, ItemStack dose) {
        if (dose.is(Items.HONEY_BOTTLE)) {
            dose.copy().finishUsingItem(player.level(), player);
            return;
        }
        PotionContents potion = dose.get(DataComponents.POTION_CONTENTS);
        if (potion != null) {
            potion.onConsume(player.level(), player, dose, Consumables.DEFAULT_DRINK);
        }
    }

    public static DispenseMove dispenseMove(FlagonContents contents, ItemStack stack) {
        if (stack.is(Items.GLASS_BOTTLE)) {
            if (contents.isEmpty()) {
                return DispenseMove.unchanged(contents, stack);
            }
            return replaceOne(stack, contents.bottledDose(), contents.removeDose());
        }
        if (FlagonContents.isDoseItem(stack)) {
            FlagonContents updated = contents.insertDose(stack);
            if (updated == contents) {
                return DispenseMove.unchanged(contents, stack);
            }
            return replaceOne(stack, new ItemStack(Items.GLASS_BOTTLE), updated);
        }
        if (stack.is(ModItems.FLAGON.get())) {
            return pourDispensed(contents, stack);
        }
        return DispenseMove.IGNORED;
    }

    private static DispenseMove pourDispensed(FlagonContents destination, ItemStack flagon) {
        FlagonContents source = FlagonContents.of(flagon);
        if (source.isEmpty() || !destination.canAccept(source.dose(), 1)) {
            return DispenseMove.IGNORED;
        }
        int moved = Math.min(source.doses(), destination.space());
        ItemStack updated = flagon.copyWithCount(1);
        updated.set(ModDataComponents.FLAGON_CONTENTS.get(), source.remove(moved));
        FlagonContents filled = destination.insert(source.dose(), moved);
        if (flagon.getCount() == 1) {
            return new DispenseMove(true, filled, updated, ItemStack.EMPTY);
        }
        ItemStack remaining = flagon.copy();
        remaining.shrink(1);
        return new DispenseMove(true, filled, remaining, updated);
    }

    public static void pulseObservers(Level level, BlockPos pos) {
        if (level.isClientSide() || level.getBlockState(pos).getBlock() instanceof EnderFlagonBlock) {
            return;
        }
        level.getBlockState(pos).updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
    }

    private static DispenseMove replaceOne(ItemStack stack, ItemStack product, FlagonContents contents) {
        if (stack.getCount() == 1) {
            return new DispenseMove(true, contents, product, ItemStack.EMPTY);
        }
        ItemStack remaining = stack.copy();
        remaining.shrink(1);
        return new DispenseMove(true, contents, remaining, product.copy());
    }

    private static InteractionResult pour(
            BlockPos pos,
            Player player,
            ItemStack held,
            FlagonItem flagon,
            FlagonContents destination,
            Consumer<FlagonContents> set
    ) {
        FlagonContents source = flagon.playerContents(player, held);
        if (sameEnderStore(player, held, pos) || source.isEmpty() || !destination.canAccept(source.dose(), 1)) {
            return InteractionResult.PASS;
        }
        int moved = Math.min(source.doses(), destination.space());
        if (!player.level().isClientSide()) {
            set.accept(destination.insert(source.dose(), moved));
            flagon.writeContents(player, held, source.remove(moved));
            play(pos, player, SoundEvents.BOTTLE_EMPTY, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    /** Pouring a store into itself would add the doses and then remove them. */
    private static boolean sameEnderStore(Player player, ItemStack held, BlockPos pos) {
        java.util.UUID destination = null;
        if (player.level().getBlockEntity(pos) instanceof AnchoredEnderFlagonBlockEntity anchored) {
            destination = anchored.playerId();
        } else if (player.level().getBlockState(pos).getBlock() instanceof EnderFlagonBlock) {
            destination = player.getUUID();
        }
        java.util.UUID source = EnderFlagons.storeId(player, held);
        return destination != null && destination.equals(source);
    }

    private static void play(BlockPos pos, Player player, SoundEvent sound, float pitch) {
        player.level().playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, pitch);
    }

    public record DispenseMove(boolean handled, FlagonContents contents, ItemStack slot, ItemStack extra) {
        public static final DispenseMove IGNORED = new DispenseMove(false, FlagonContents.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

        public static DispenseMove unchanged(FlagonContents contents, ItemStack slot) {
            return new DispenseMove(true, contents, slot, ItemStack.EMPTY);
        }
    }
}
