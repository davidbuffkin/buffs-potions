package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.EyedDisplay;
import com.buffspotions.effect.SeekAim;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID, value = Dist.CLIENT)
public final class SeekingClient {
    /** One full turn of the compass needle while a block search is running. */
    private static final float TICKS_PER_TURN = 40.0F;
    private static float spin;

    static {
        EyedDisplay.VOMITING = () -> ParameterNames.join(ModEffects.ITEM_VOMITING, "effect.buffs_potions.item_vomiting.named", false);
        EyedDisplay.SEEKING = () -> ParameterNames.join(ModEffects.SEEKING, "effect.buffs_potions.seeking.named", true);
        EyedDisplay.DISGUISE = () -> ParameterNames.join(ModEffects.DISGUISE, "effect.buffs_potions.disguise.named", false);
    }

    private SeekingClient() {
    }

    public static float spin() {
        return spin;
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused()) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || !player.hasEffect(ModEffects.SEEKING) || !player.hasData(ModAttachments.SEEK_AIM.get())) {
            return;
        }
        SeekAim aim = player.getData(ModAttachments.SEEK_AIM.get());
        if (!aim.searching()) {
            return;
        }
        float turns = event.getPartialTick().getRealtimeDeltaTicks() / TICKS_PER_TURN;
        if (turns > 0.0F) {
            spin = Mth.positiveModulo(spin + turns, 1.0F);
        }
    }
}
