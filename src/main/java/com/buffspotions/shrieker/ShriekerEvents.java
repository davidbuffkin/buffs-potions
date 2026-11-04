package com.buffspotions.shrieker;

import com.buffspotions.BuffsPotions;
import com.buffspotions.registry.ModBlocks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CustomClickActionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class ShriekerEvents {
    private ShriekerEvents() {
    }

    /** Shift-right-click toggles listening even when the player is holding a block. */
    @SubscribeEvent
    public static void keepShiftClick(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().level().getBlockState(event.getPos()).is(ModBlocks.CALIBRATED_SCULK_SHRIEKER.get())) {
            return;
        }
        if (event.getEntity().isSecondaryUseActive()) {
            event.setUseBlock(TriState.TRUE);
            event.setUseItem(TriState.FALSE);
        }
    }

    @SubscribeEvent
    public static void stopListening(CustomClickActionEvent event) {
        if (!CalibratedSculkShriekerBlockEntity.STOP_LISTENING.equals(event.getIdentifier())) {
            return;
        }
        event.setCanceled(true);
        if (event.getPlayer() instanceof ServerPlayer player) {
            CalibratedSculkShriekerBlockEntity.stopFromClick(player, event.getPayload());
        }
    }
}
