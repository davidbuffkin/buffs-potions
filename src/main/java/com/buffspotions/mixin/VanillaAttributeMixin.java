package com.buffspotions.mixin;

import com.buffspotions.config.BuffsPotionsConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.effect.MobEffect$AttributeTemplate")
abstract class VanillaAttributeMixin {
    @Shadow
    @Final
    private Identifier id;

    @Shadow
    @Final
    private AttributeModifier.Operation operation;

    @Inject(method = "create", at = @At("RETURN"), cancellable = true)
    private void buffsPotions$curve(int amplifier, CallbackInfoReturnable<AttributeModifier> callback) {
        int level = Math.max(0, amplifier) + 1;
        Double amount = switch (this.id.getPath()) {
            case "effect.speed" -> BuffsPotionsConfig.swiftnessBonus(level);
            case "effect.slowness" -> -BuffsPotionsConfig.slownessPenalty(level);
            case "effect.strength" -> BuffsPotionsConfig.strengthBonus(level);
            case "effect.weakness" -> -BuffsPotionsConfig.weaknessPenalty(level);
            default -> null;
        };
        if (amount == null) {
            return;
        }
        callback.setReturnValue(new AttributeModifier(this.id, amount, this.operation));
    }
}
