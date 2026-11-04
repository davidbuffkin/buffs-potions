package com.buffspotions.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

public record TrackedItem(Optional<Holder<Item>> item) {
    public static final TrackedItem EMPTY = new TrackedItem(Optional.empty());
    public static final Codec<TrackedItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.ITEM.holderByNameCodec().optionalFieldOf("item").forGetter(TrackedItem::item)
    ).apply(instance, TrackedItem::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrackedItem> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(Registries.ITEM)),
            TrackedItem::item,
            TrackedItem::new
    );

    public static TrackedItem of(Holder<Item> item) {
        return new TrackedItem(Optional.of(item));
    }
}
