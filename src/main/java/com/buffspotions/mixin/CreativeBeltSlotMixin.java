package com.buffspotions.mixin;

import com.buffspotions.equipment.BeltSlotAccess;
import com.buffspotions.equipment.FallbackBeltContainer;
import com.buffspotions.item.PotionBeltItem;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
abstract class CreativeBeltSlotMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handleSetCreativeModeSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void buffsPotions$creativeBelt(ServerboundSetCreativeModeSlotPacket packet, CallbackInfo callback) {
        int beltSlot = -1;
        for (Slot slot : this.player.inventoryMenu.slots) {
            if (slot.container instanceof FallbackBeltContainer) {
                beltSlot = slot.index;
                break;
            }
        }
        if (beltSlot < 0 || packet.slotNum() != beltSlot || !this.player.hasInfiniteMaterials()) {
            return;
        }
        ItemStack stack = packet.itemStack();
        if (!stack.isEmpty() && !(stack.getItem() instanceof PotionBeltItem)) {
            callback.cancel();
            return;
        }
        if (!stack.isItemEnabled(this.player.level().enabledFeatures())) {
            callback.cancel();
            return;
        }
        ItemStack previous = BeltSlotAccess.getEquipped(this.player);
        ItemStack equipped = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.matches(previous, equipped)) {
            callback.cancel();
            return;
        }
        BeltSlotAccess.setEquipped(this.player, equipped);
        if (!equipped.isEmpty() && !ItemStack.isSameItem(previous, equipped)) {
            this.player.level().playSound(
                    null,
                    this.player.getX(),
                    this.player.getY(),
                    this.player.getZ(),
                    SoundEvents.ARMOR_EQUIP_GENERIC.value(),
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
        callback.cancel();
    }
}
