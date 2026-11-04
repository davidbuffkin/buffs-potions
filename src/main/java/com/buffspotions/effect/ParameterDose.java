package com.buffspotions.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One active subject of a parameterized effect. Several can share an effect type.
 * {@code expiresAt} is the level game time when the dose ends, so the list only syncs when it changes.
 */
public record ParameterDose(String effect, EssenceSubject subject, int amplifier, long expiresAt) {
    /** Game time that never arrives. A dose with this expiry lasts until it is replaced. */
    public static final long NEVER = Long.MAX_VALUE;
    public static final Codec<ParameterDose> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("effect").forGetter(ParameterDose::effect),
            EssenceSubject.CODEC.fieldOf("subject").forGetter(ParameterDose::subject),
            Codec.INT.fieldOf("amplifier").forGetter(ParameterDose::amplifier),
            Codec.LONG.fieldOf("expires").forGetter(ParameterDose::expiresAt)
    ).apply(instance, ParameterDose::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ParameterDose> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            ParameterDose::effect,
            EssenceSubject.STREAM_CODEC,
            ParameterDose::subject,
            ByteBufCodecs.VAR_INT,
            ParameterDose::amplifier,
            ByteBufCodecs.VAR_LONG,
            ParameterDose::expiresAt,
            ParameterDose::new
    );
}
