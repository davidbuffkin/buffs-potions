package com.buffspotions.client;

import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.AnchoredEnderFlagonItem;
import com.buffspotions.potion.Remedies;
import com.buffspotions.registry.ModItems;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/** One horizontal slice of a remedy fill. Band 0 is the top and keeps the effect color; band 3 is white. */
public record RemedyBandTint(int band) implements ItemTintSource {
    public static final MapCodec<RemedyBandTint> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.intRange(0, 3).fieldOf("band").forGetter(RemedyBandTint::band)
    ).apply(instance, RemedyBandTint::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner) {
        ItemStack dose = stack;
        if (stack.is(ModItems.ENDER_FLAGON.get())) {
            dose = EnderFlagons.viewed().dose();
        } else if (stack.is(ModItems.ANCHORED_ENDER_FLAGON.get())) {
            dose = AnchoredEnderFlagonItem.shown(stack).dose();
        } else if (stack.is(ModItems.FLAGON.get())) {
            dose = FlagonContents.of(stack).dose();
        }
        var effect = Remedies.effectOf(dose);
        int color = effect == null ? 0xFFFFFF : effect.value().getColor();
        float towardColor = 1.0F - this.band / 3.0F;
        return ARGB.opaque(lerp(0xFFFFFF, color, towardColor));
    }

    @Override
    public MapCodec<RemedyBandTint> type() {
        return MAP_CODEC;
    }

    private static int lerp(int from, int to, float t) {
        int red = channel((from >> 16) & 255, (to >> 16) & 255, t);
        int green = channel((from >> 8) & 255, (to >> 8) & 255, t);
        int blue = channel(from & 255, to & 255, t);
        return (red << 16) | (green << 8) | blue;
    }

    private static int channel(int from, int to, float t) {
        return Math.round(from + (to - from) * t);
    }
}
