package com.buffspotions.effect;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public record DislocationAnchor(
        ResourceKey<Level> dimension,
        UUID shadowId,
        double x,
        double y,
        double z,
        float yRot,
        float xRot,
        int amplifier
) {
    public static final MapCodec<DislocationAnchor> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(DislocationAnchor::dimension),
            UUIDUtil.CODEC.fieldOf("shadow").forGetter(DislocationAnchor::shadowId),
            Codec.DOUBLE.fieldOf("x").forGetter(DislocationAnchor::x),
            Codec.DOUBLE.fieldOf("y").forGetter(DislocationAnchor::y),
            Codec.DOUBLE.fieldOf("z").forGetter(DislocationAnchor::z),
            Codec.FLOAT.fieldOf("y_rot").forGetter(DislocationAnchor::yRot),
            Codec.FLOAT.fieldOf("x_rot").forGetter(DislocationAnchor::xRot),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(DislocationAnchor::amplifier)
    ).apply(instance, DislocationAnchor::new));

    public Vec3 position() {
        return new Vec3(this.x, this.y, this.z);
    }

    public ChunkPos chunk() {
        return ChunkPos.containing(BlockPos.containing(this.x, this.y, this.z));
    }
}
