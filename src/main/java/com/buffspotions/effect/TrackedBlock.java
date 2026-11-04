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
import net.minecraft.world.level.block.Block;

public record TrackedBlock(Optional<Holder<Block>> block) {
    public static final TrackedBlock EMPTY = new TrackedBlock(Optional.empty());
    public static final Codec<TrackedBlock> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BuiltInRegistries.BLOCK.holderByNameCodec().optionalFieldOf("block").forGetter(TrackedBlock::block)
    ).apply(instance, TrackedBlock::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, TrackedBlock> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(ByteBufCodecs.holderRegistry(Registries.BLOCK)),
            TrackedBlock::block,
            TrackedBlock::new
    );

    public static TrackedBlock of(Holder<Block> block) {
        return new TrackedBlock(Optional.of(block));
    }
}
