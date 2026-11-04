package com.buffspotions.client;

import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.AnchoredEnderFlagonItem;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModItems;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record RemedyDose() implements ConditionalItemModelProperty {
    public static final MapCodec<RemedyDose> MAP_CODEC = MapCodec.unit(new RemedyDose());

    @Override
    public boolean get(
            ItemStack stack,
            @Nullable ClientLevel level,
            @Nullable LivingEntity owner,
            int seed,
            ItemDisplayContext displayContext
    ) {
        ItemStack dose = stack.is(ModItems.ENDER_FLAGON.get())
                ? EnderFlagons.viewed().dose()
                : stack.is(ModItems.ANCHORED_ENDER_FLAGON.get())
                        ? AnchoredEnderFlagonItem.shown(stack).dose()
                        : FlagonContents.of(stack).dose();
        return Remedies.isRemedy(dose);
    }

    @Override
    public MapCodec<RemedyDose> type() {
        return MAP_CODEC;
    }
}
