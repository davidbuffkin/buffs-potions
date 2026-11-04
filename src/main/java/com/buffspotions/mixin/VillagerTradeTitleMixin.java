package com.buffspotions.mixin;

import com.buffspotions.registry.ModAttachments;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** A disguised villager's trade screen uses the player's name. */
@Mixin(Villager.class)
abstract class VillagerTradeTitleMixin {
    @Redirect(
            method = "startTrading",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/npc/villager/Villager;getDisplayName()Lnet/minecraft/network/chat/Component;"
            )
    )
    private Component buffsPotions$disguiseTitle(Villager villager) {
        String name = villager.getData(ModAttachments.DISGUISE.get());
        if (name != null && !name.isBlank()) {
            return Component.literal(name);
        }
        return villager.getDisplayName();
    }
}
