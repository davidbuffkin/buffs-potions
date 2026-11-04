package com.buffspotions.network;

import com.buffspotions.BuffsPotions;
import com.buffspotions.belt.BeltToolSwap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A real attack-button press, naming the block the player was looking at. */
public record ToolSwapPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ToolSwapPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "tool_swap"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToolSwapPayload> STREAM_CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, ToolSwapPayload::pos, ToolSwapPayload::new);

    public static void handle(ToolSwapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BeltToolSwap.swap(player, payload.pos());
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
