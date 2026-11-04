package com.buffspotions.mixin;

import com.buffspotions.advancement.BrewingTabPlacement;
import com.buffspotions.advancement.ExpelliarmusPlacement;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementTree;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerAdvancementManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerAdvancementManager.class)
abstract class ServerAdvancementManagerMixin {
    @Inject(method = "apply", at = @At("RETURN"))
    private void buffsPotions$placeExpelliarmus(
            Map<Identifier, Advancement> preparations,
            ResourceManager manager,
            ProfilerFiller profiler,
            CallbackInfo callback
    ) {
        AdvancementTree tree = ((ServerAdvancementManager) (Object) this).tree();
        ExpelliarmusPlacement.place(tree);
        BrewingTabPlacement.place(tree);
    }
}
