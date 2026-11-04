package com.buffspotions.mixin;

import com.buffspotions.cauldron.CauldronDispense;
import com.buffspotions.flagon.AnchoredEnderFlagonBlockEntity;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.flagon.FlagonInteractions;
import com.buffspotions.flagon.FlagonVessel;
import com.buffspotions.registry.ModCriteria;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public abstract class DispenserBlockMixin {
    @Inject(method = "dispenseFrom", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$exchangeWithFlagon(ServerLevel level, BlockState state, BlockPos pos, CallbackInfo callback) {
        if ((Object) this instanceof net.minecraft.world.level.block.DropperBlock) {
            return;
        }
        Direction facing = state.getValue(DispenserBlock.FACING);
        BlockPos target = pos.relative(facing);
        if (level.getBlockState(target).is(Blocks.WATER_CAULDRON)) {
            fillFromWaterCauldron(level, pos, target, callback);
            return;
        }
        if (!(level.getBlockEntity(target) instanceof FlagonVessel flagon)) {
            return;
        }
        DispenserBlockEntity dispenser = level.getBlockEntity(pos, BlockEntityTypes.DISPENSER).orElse(null);
        if (dispenser == null) {
            return;
        }
        int slot = dispenser.getRandomSlot(level.getRandom());
        if (slot < 0) {
            return;
        }
        ItemStack stack = dispenser.getItem(slot);
        FlagonInteractions.DispenseMove move = FlagonInteractions.dispenseMove(flagon.contents(), stack);
        if (!move.handled()) {
            return;
        }
        callback.cancel();
        if (move.contents() == flagon.contents()) {
            level.levelEvent(1001, pos, 0);
            return;
        }
        flagon.setContents(move.contents());
        dispenser.setItem(slot, move.slot());
        if (!move.extra().isEmpty() && !insertExtra(dispenser, move.extra())) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, move.extra());
        }
        level.levelEvent(1000, pos, 0);
        level.playSound(
                null,
                target,
                stack.is(Items.GLASS_BOTTLE) ? SoundEvents.BOTTLE_FILL : SoundEvents.BOTTLE_EMPTY,
                SoundSource.BLOCKS,
                1.0F,
                1.0F
        );
        anchorsAweigh(level, flagon, stack);
    }

    private static void fillFromWaterCauldron(ServerLevel level, BlockPos pos, BlockPos target, CallbackInfo callback) {
        DispenserBlockEntity dispenser = level.getBlockEntity(pos, BlockEntityTypes.DISPENSER).orElse(null);
        if (dispenser == null) {
            return;
        }
        int slot = dispenser.getRandomSlot(level.getRandom());
        if (slot < 0) {
            return;
        }
        ItemStack stack = dispenser.getItem(slot);
        ItemStack filled = CauldronDispense.fill(stack);
        if (filled.isEmpty()) {
            return;
        }
        callback.cancel();
        stack.shrink(1);
        if (stack.isEmpty()) {
            dispenser.setItem(slot, filled);
        } else if (!insertExtra(dispenser, filled)) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, filled);
        }
        level.levelEvent(1000, pos, 0);
        level.playSound(null, target, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** One bottle dose moved through an anchored flagon grants the advancement to that online player. */
    private static void anchorsAweigh(ServerLevel level, FlagonVessel flagon, ItemStack stack) {
        if (!(flagon instanceof AnchoredEnderFlagonBlockEntity anchored)) {
            return;
        }
        if (!stack.is(Items.GLASS_BOTTLE) && !FlagonContents.isDoseItem(stack)) {
            return;
        }
        UUID ownerId = anchored.playerId();
        if (ownerId == null) {
            return;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null) {
            ModCriteria.ANCHORS_AWEIGH.get().trigger(owner);
        }
    }

    private static boolean insertExtra(DispenserBlockEntity dispenser, ItemStack extra) {
        for (int slot = 0; slot < dispenser.getContainerSize(); slot++) {
            ItemStack existing = dispenser.getItem(slot);
            if (existing.isEmpty()) {
                dispenser.setItem(slot, extra);
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, extra) && existing.getCount() < existing.getMaxStackSize()) {
                existing.grow(extra.getCount());
                return true;
            }
        }
        return false;
    }
}
