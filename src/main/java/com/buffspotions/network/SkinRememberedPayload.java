package com.buffspotions.network;

import com.buffspotions.BuffsPotions;
import com.buffspotions.client.SkinMemory;
import com.buffspotions.skin.SkinPixels;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** One skin from this world's cache, sent so clients can draw it without a lookup. */
public record SkinRememberedPayload(String name, Optional<UUID> id, boolean slim, byte[] skin, byte[] cape, byte[] elytra)
        implements CustomPacketPayload {
    public static final Type<SkinRememberedPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "skin_remembered"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkinRememberedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16),
            SkinRememberedPayload::name,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            SkinRememberedPayload::id,
            ByteBufCodecs.BOOL,
            SkinRememberedPayload::slim,
            ByteBufCodecs.byteArray(SkinPixels.MAX_BYTES),
            SkinRememberedPayload::skin,
            ByteBufCodecs.byteArray(SkinPixels.MAX_BYTES),
            SkinRememberedPayload::cape,
            ByteBufCodecs.byteArray(SkinPixels.MAX_BYTES),
            SkinRememberedPayload::elytra,
            SkinRememberedPayload::new
    );

    public static void handle(SkinRememberedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SkinMemory.accept(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
