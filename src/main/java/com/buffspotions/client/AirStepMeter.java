package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.AirStep;
import com.buffspotions.registry.ModEffects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/** The horse jump bar, drained by air jumps. The left two sections are red in the sprite. */
public final class AirStepMeter {
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "hud/air_step_bar_background");
    private static final Identifier PROGRESS = Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "hud/air_step_bar_progress");
    private static final int WIDTH = 182;
    private static final int HEIGHT = 5;

    private AirStepMeter() {
    }

    public static boolean showing(Player player) {
        return player instanceof LocalPlayer local
                && local.jumpableVehicle() == null
                && !local.onGround()
                && AirStep.used(local) > 0
                && local.hasEffect(ModEffects.WIND_JUMPING);
    }

    public static boolean render(GuiGraphicsExtractor graphics, Player player) {
        if (!showing(player)) {
            return false;
        }
        MobEffectInstance effect = player.getEffect(ModEffects.WIND_JUMPING);
        if (effect == null) {
            return false;
        }
        int limit = AirStep.limit(effect.getAmplifier());
        int remaining = Math.max(0, limit - AirStep.used(player));
        int left = (graphics.guiWidth() - WIDTH) / 2;
        int top = graphics.guiHeight() - 24 - HEIGHT;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, left, top, WIDTH, HEIGHT);
        int filled = Mth.ceil(remaining / (float) limit * WIDTH);
        if (filled > 0) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS, WIDTH, HEIGHT, 0, 0, left, top, filled, HEIGHT);
        }
        return true;
    }
}
