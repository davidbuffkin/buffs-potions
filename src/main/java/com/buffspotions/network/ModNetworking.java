package com.buffspotions.network;

import com.buffspotions.BuffsPotions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class ModNetworking {
    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("9")
                .playToServer(BeltActionPayload.TYPE, BeltActionPayload.STREAM_CODEC, BeltActionPayload::handle)
                .playToServer(BeltKeyPayload.TYPE, BeltKeyPayload.STREAM_CODEC, BeltKeyPayload::handle)
                .playToServer(ToolSwapPayload.TYPE, ToolSwapPayload.STREAM_CODEC, ToolSwapPayload::handle)
                .playToServer(StandPlacePayload.TYPE, StandPlacePayload.STREAM_CODEC, StandPlacePayload::handle)
                .playToServer(SkinOfferPayload.TYPE, SkinOfferPayload.STREAM_CODEC, SkinOfferPayload::handle)
                .playToServer(ShriekerSavePayload.TYPE, ShriekerSavePayload.STREAM_CODEC, ShriekerSavePayload::handle)
                .playToClient(SkinRememberedPayload.TYPE, SkinRememberedPayload.STREAM_CODEC, SkinRememberedPayload::handle)
                .playToClient(ShriekerOpenPayload.TYPE, ShriekerOpenPayload.STREAM_CODEC, ShriekerOpenPayload::handle);
    }

    private ModNetworking() {
    }
}
