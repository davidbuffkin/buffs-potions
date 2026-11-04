package com.buffspotions.network;

import com.buffspotions.BuffsPotions;
import com.buffspotions.skin.SkinPixels;
import com.buffspotions.skin.WorldSkins;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A skin the client already has, offered to the world cache. */
public record SkinOfferPayload(String name, Optional<UUID> id, boolean slim, byte[] skin, byte[] cape, byte[] elytra)
        implements CustomPacketPayload {
    public static final Type<SkinOfferPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "skin_offer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SkinOfferPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(16),
            SkinOfferPayload::name,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC),
            SkinOfferPayload::id,
            ByteBufCodecs.BOOL,
            SkinOfferPayload::slim,
            ByteBufCodecs.byteArray(SkinPixels.MAX_BYTES),
            SkinOfferPayload::skin,
            ByteBufCodecs.byteArray(SkinPixels.MAX_BYTES),
            SkinOfferPayload::cape,
            ByteBufCodecs.byteArray(SkinPixels.MAX_BYTES),
            SkinOfferPayload::elytra,
            SkinOfferPayload::new
    );

    public static void handle(SkinOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.level().getServer() == null) {
                return;
            }
            WorldSkins skins = WorldSkins.get(player.level().getServer());
            if (!skins.remember(payload.name(), payload.id(), payload.slim(), payload.skin(), payload.cape(), payload.elytra(), player.getUUID())) {
                return;
            }
            skins.find(payload.name()).ifPresent(saved ->
                    PacketDistributor.sendToAllPlayers(WorldSkins.remembered(saved)));
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
