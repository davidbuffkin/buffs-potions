package com.buffspotions.client;

import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.AnchoredEnderFlagonItem;
import com.buffspotions.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record FlagonDoses() implements RangeSelectItemModelProperty {
    public static final MapCodec<FlagonDoses> MAP_CODEC = MapCodec.unit(new FlagonDoses());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        if (stack.is(ModItems.ENDER_FLAGON.get())) {
            return EnderFlagons.viewed().doses();
        }
        if (stack.is(ModItems.ANCHORED_ENDER_FLAGON.get())) {
            return AnchoredEnderFlagonItem.shown(stack).doses();
        }
        return FlagonContents.of(stack).doses();
    }

    @Override
    public MapCodec<FlagonDoses> type() {
        return MAP_CODEC;
    }
}
