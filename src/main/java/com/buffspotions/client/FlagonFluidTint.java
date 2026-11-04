package com.buffspotions.client;

import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.AnchoredEnderFlagonItem;
import com.buffspotions.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record FlagonFluidTint() implements ItemTintSource {
    public static final MapCodec<FlagonFluidTint> MAP_CODEC = MapCodec.unit(new FlagonFluidTint());

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        FlagonContents contents = stack.is(ModItems.ENDER_FLAGON.get())
                ? EnderFlagons.viewed()
                : stack.is(ModItems.ANCHORED_ENDER_FLAGON.get())
                        ? AnchoredEnderFlagonItem.shown(stack)
                        : FlagonContents.of(stack);
        return ARGB.opaque(contents.fluidColor());
    }

    @Override
    public MapCodec<FlagonFluidTint> type() {
        return MAP_CODEC;
    }
}
