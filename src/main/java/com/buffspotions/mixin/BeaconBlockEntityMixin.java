package com.buffspotions.mixin;

import com.buffspotions.registry.ModEffects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntity.class)
abstract class BeaconBlockEntityMixin {
    @Shadow
    @Final
    @Mutable
    public static List<List<Holder<MobEffect>>> BEACON_EFFECTS;

    @Shadow
    @Final
    @Mutable
    private static Set<Holder<MobEffect>> VALID_EFFECTS;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void buffsPotions$beaconPowers(CallbackInfo callback) {
        List<List<Holder<MobEffect>>> tiers = new ArrayList<>();
        for (List<Holder<MobEffect>> tier : BEACON_EFFECTS) {
            tiers.add(new ArrayList<>(tier));
        }
        tiers.get(2).add(ModEffects.REACHING);
        tiers.get(3).add(ModEffects.STOPPERED);
        BEACON_EFFECTS = tiers.stream().map(List::copyOf).toList();
        VALID_EFFECTS = BEACON_EFFECTS.stream().flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());
    }
}
