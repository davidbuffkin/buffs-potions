package com.buffspotions.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record SeekAim(int phase, double x, double y, double z, String dimension) {
    public static final int SEARCHING = 0;
    public static final int FOUND = 1;
    public static final int LOST = 2;
    public static final SeekAim NONE = new SeekAim(LOST, 0.0, 0.0, 0.0, "");
    public static final SeekAim SEARCH = new SeekAim(SEARCHING, 0.0, 0.0, 0.0, "");
    public static final Codec<SeekAim> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("phase", -1).forGetter(SeekAim::phase),
            Codec.DOUBLE.optionalFieldOf("x", 0.0).forGetter(SeekAim::x),
            Codec.DOUBLE.optionalFieldOf("y", 0.0).forGetter(SeekAim::y),
            Codec.DOUBLE.optionalFieldOf("z", 0.0).forGetter(SeekAim::z),
            Codec.STRING.optionalFieldOf("dimension", "").forGetter(SeekAim::dimension),
            Codec.BOOL.optionalFieldOf("present", false).forGetter(aim -> aim.phase == FOUND)
    ).apply(instance, SeekAim::fromSaved));
    public static final StreamCodec<RegistryFriendlyByteBuf, SeekAim> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SeekAim::phase,
            ByteBufCodecs.DOUBLE,
            SeekAim::x,
            ByteBufCodecs.DOUBLE,
            SeekAim::y,
            ByteBufCodecs.DOUBLE,
            SeekAim::z,
            ByteBufCodecs.STRING_UTF8,
            SeekAim::dimension,
            SeekAim::new
    );

    private static SeekAim fromSaved(int phase, double x, double y, double z, String dimension, boolean present) {
        if (phase < SEARCHING || phase > LOST) {
            phase = present ? FOUND : LOST;
        }
        return new SeekAim(phase, x, y, z, dimension == null ? "" : dimension);
    }

    public static SeekAim found(ResourceKey<Level> dimension, Vec3 position) {
        return new SeekAim(FOUND, position.x, position.y, position.z, dimension.identifier().toString());
    }

    public boolean searching() {
        return phase == SEARCHING;
    }

    public boolean found() {
        return phase == FOUND;
    }

    public boolean lost() {
        return phase == LOST;
    }

    public Vec3 position() {
        return new Vec3(x, y, z);
    }
}
