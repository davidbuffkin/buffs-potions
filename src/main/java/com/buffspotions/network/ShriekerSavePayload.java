package com.buffspotions.network;

import com.buffspotions.BuffsPotions;
import com.buffspotions.shrieker.CalibratedSculkShriekerBlockEntity;
import com.buffspotions.shrieker.ShriekerText;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShriekerSavePayload(BlockPos pos, String text) implements CustomPacketPayload {
    public static final Type<ShriekerSavePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "shrieker_save"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShriekerSavePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ShriekerSavePayload::pos,
            ByteBufCodecs.stringUtf8(ShriekerText.MAX_LENGTH * 3),
            ShriekerSavePayload::text,
            ShriekerSavePayload::new
    );

    public static void handle(ShriekerSavePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!player.isWithinBlockInteractionRange(payload.pos(), 8.0)) {
                return;
            }
            if (player.level().getBlockEntity(payload.pos()) instanceof CalibratedSculkShriekerBlockEntity shrieker) {
                shrieker.saveMessage(player, payload.text());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
