package com.buffspotions.client;

import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonAnchor;
import com.buffspotions.registry.ModDataComponents;
import com.mojang.serialization.MapCodec;
import java.util.UUID;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** Tints the anchored flagon's stopper to the anchor's locator-bar color. An unbound stack uses the local player. */
public record AnchorStopperTint() implements ItemTintSource {
    public static final MapCodec<AnchorStopperTint> MAP_CODEC = MapCodec.unit(new AnchorStopperTint());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        FlagonAnchor anchor = stack.get(ModDataComponents.FLAGON_ANCHOR.get());
        UUID player = anchor == null ? EnderFlagons.clientPlayer() : anchor.player();
        return AnchorStopperColor.of(player);
    }

    @Override
    public MapCodec<AnchorStopperTint> type() {
        return MAP_CODEC;
    }
}
