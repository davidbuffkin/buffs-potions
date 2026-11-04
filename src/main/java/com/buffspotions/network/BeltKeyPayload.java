package com.buffspotions.network;

import com.buffspotions.BuffsPotions;
import com.buffspotions.registry.ModAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BeltKeyPayload(boolean down) implements CustomPacketPayload {
    public static final Type<BeltKeyPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "belt_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BeltKeyPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, BeltKeyPayload::down, BeltKeyPayload::new);

    public static void handle(BeltKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                player.setData(ModAttachments.BELT_KEY, payload.down());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
