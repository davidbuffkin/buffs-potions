package com.buffspotions.network;

import com.buffspotions.BuffsPotions;
import com.buffspotions.client.CalibratedSculkShriekerScreen;
import com.buffspotions.shrieker.ShriekerText;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ShriekerOpenPayload(BlockPos pos, String text) implements CustomPacketPayload {
    public static final Type<ShriekerOpenPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "shrieker_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ShriekerOpenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ShriekerOpenPayload::pos,
            ByteBufCodecs.stringUtf8(ShriekerText.MAX_LENGTH * 3),
            ShriekerOpenPayload::text,
            ShriekerOpenPayload::new
    );

    public static void handle(ShriekerOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().gui.setScreen(new CalibratedSculkShriekerScreen(payload.pos(), payload.text())));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
