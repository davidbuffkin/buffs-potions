package com.buffspotions.network;

import com.buffspotions.BuffsPotions;
import com.buffspotions.belt.BeltContents;
import com.buffspotions.belt.BeltSource;
import com.buffspotions.belt.BeltConsumptionService;
import com.buffspotions.belt.BeltEnchantments;
import com.buffspotions.belt.BeltTipping;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.item.FlagonBeltItem;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.menu.BeltItemContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BeltActionPayload(Action action, int value) implements CustomPacketPayload {
    public static final Type<BeltActionPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "belt_action"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BeltActionPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    payload -> payload.action.ordinal(),
                    ByteBufCodecs.VAR_INT,
                    BeltActionPayload::value,
                    (ordinal, value) -> new BeltActionPayload(Action.byOrdinal(ordinal), value)
            );

    public static void handle(BeltActionPayload payload, IPayloadContext context) {
        if (payload.action == Action.SWITCH_MODE && payload.value >= 256 && context.player() instanceof ServerPlayer player) {
            int packed = payload.value - 256;
            BeltTipping.pendingShot(player.getUUID(), packed / 32, packed % 32);
            return;
        }
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            switch (payload.action) {
                case SELECT -> select(player, payload.value, false);
                case START_DRINK -> select(player, payload.value, true);
                case STOP_DRINK -> {
                    player.getData(ModAttachments.BELT_USE).cancel();
                    player.syncData(ModAttachments.BELT_USE);
                }
                case SCROLL -> scroll(player, payload.value);
                case OPEN_EQUIPPED -> {
                    if (!BeltSource.equipped(player).isEmpty()) {
                        PotionBeltItem.openMenu(player, BeltItemContainer.EQUIPPED);
                    }
                }
                case START_EQUIPPED -> BeltConsumptionService.activateEquipped(player);
                case SWITCH_ROW -> switchRow(player);
                case SWITCH_MODE -> switchMode(player, payload.value);
                case OPEN_HAND -> openHand(player, payload.value);
            }
        });
    }

    private static void openHand(ServerPlayer player, int source) {
        if (source >= BeltItemContainer.INVENTORY) {
            int index = source - BeltItemContainer.INVENTORY;
            if (index < 0 || index >= player.getInventory().getContainerSize()) {
                return;
            }
            if (player.getInventory().getItem(index).getItem() instanceof PotionBeltItem) {
                PotionBeltItem.openMenu(player, source);
            }
            return;
        }
        if (source == BeltItemContainer.OFF_HAND) {
            if (player.getOffhandItem().getItem() instanceof PotionBeltItem) {
                PotionBeltItem.openMenu(player, BeltItemContainer.OFF_HAND);
            }
            return;
        }
        if (player.getMainHandItem().getItem() instanceof PotionBeltItem) {
            PotionBeltItem.openMenu(player, BeltItemContainer.MAIN_HAND);
        }
    }

    private static void select(ServerPlayer player, int column, boolean startDrink) {
        if (column < 0 || column >= BeltContents.BASE_SLOTS) {
            return;
        }
        ItemStack belt = BeltSource.shown(player);
        if (belt.isEmpty()) {
            return;
        }
        int slot = BeltEnchantments.rowStart(belt) + column;

        BeltContents.setActiveSlot(belt, slot);
        BeltSource.saveShown(player, belt);
        BeltConsumptionService.publish(player, belt);

        var state = player.getData(ModAttachments.BELT_USE);
        state.cancel();
        state.clearAdvance();
        if (startDrink
                && BeltSource.heldHand(player) == null
                && !player.isUsingItem()
                && PotionBeltItem.canDrink(player, belt)
                && BeltContents.isConsumableCell(belt, BeltContents.get(belt, slot))) {
            state.start(slot);
        }
        player.syncData(ModAttachments.BELT_USE);
    }

    private static void scroll(ServerPlayer player, int direction) {
        if (direction == 0) {
            return;
        }
        ItemStack belt = BeltSource.shown(player);
        if (belt.isEmpty()) {
            return;
        }
        int slot = BeltContents.scrollInRow(
                belt,
                direction,
                BeltEnchantments.rowStart(belt),
                BeltContents.BASE_SLOTS
        );
        BeltContents.setActiveSlot(belt, slot);
        BeltSource.saveShown(player, belt);
        BeltConsumptionService.publish(player, belt);
        var state = player.getData(ModAttachments.BELT_USE);
        state.clearAdvance();
        player.syncData(ModAttachments.BELT_USE);
    }

    private static void switchMode(ServerPlayer player, int throwing) {
        ItemStack belt = BeltSource.shown(player);
        if (!(belt.getItem() instanceof FlagonBeltItem flagon) || !flagon.hasThrowMode()) {
            return;
        }
        FlagonBeltItem.setMode(belt, throwing);
        BeltSource.saveShown(player, belt);
        BeltConsumptionService.publish(player, belt);
        var state = player.getData(ModAttachments.BELT_USE);
        state.cancel();
        state.clearAdvance();
        player.syncData(ModAttachments.BELT_USE);
    }

    private static void switchRow(ServerPlayer player) {
        ItemStack belt = BeltSource.shown(player);
        if (belt.isEmpty() || !BeltEnchantments.hasGauge(belt)) {
            return;
        }
        int nextRow = 1 - BeltEnchantments.row(belt);
        belt.set(ModDataComponents.BELT_ROW.get(), nextRow);
        int column = Math.floorMod(BeltContents.activeSlot(belt), BeltContents.BASE_SLOTS);
        BeltContents.setActiveSlot(belt, nextRow * BeltContents.BASE_SLOTS + column);
        BeltSource.saveShown(player, belt);
        BeltConsumptionService.publish(player, belt);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public enum Action {
        SELECT,
        START_DRINK,
        STOP_DRINK,
        SCROLL,
        OPEN_EQUIPPED,
        START_EQUIPPED,
        OPEN_HAND,
        SWITCH_ROW,
        SWITCH_MODE;

        private static Action byOrdinal(int ordinal) {
            return values()[Math.clamp(ordinal, 0, values().length - 1)];
        }
    }
}
