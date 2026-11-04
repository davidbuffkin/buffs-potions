package com.buffspotions.flagon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The player whose ender flagon an anchored flagon reads and writes. */
public record FlagonAnchor(UUID player, String name) {
    public static final Codec<FlagonAnchor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.LENIENT_CODEC.fieldOf("player").forGetter(FlagonAnchor::player),
            Codec.STRING.optionalFieldOf("name", "").forGetter(FlagonAnchor::name)
    ).apply(instance, FlagonAnchor::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, FlagonAnchor> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            FlagonAnchor::player,
            ByteBufCodecs.STRING_UTF8,
            FlagonAnchor::name,
            FlagonAnchor::new
    );
}
