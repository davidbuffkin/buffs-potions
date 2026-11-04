package com.buffspotions.mixin.client;

import com.buffspotions.client.AirStepMeter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class HudAirStepMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extractContextualInfoBarBackground", at = @At("HEAD"), cancellable = true)
    private void buffsPotions$airStepBar(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        if (this.minecraft.player != null && AirStepMeter.render(graphics, this.minecraft.player)) {
            callback.cancel();
        }
    }
}
