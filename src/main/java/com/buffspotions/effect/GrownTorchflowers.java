package com.buffspotions.effect;

import com.buffspotions.BuffsPotions;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Torchflowers that bloomed from a crop. A placed torchflower is the same block, so the growth has to be remembered. */
public final class GrownTorchflowers extends SavedData {
    private static final Codec<GrownTorchflowers> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("spots", List.of()).forGetter(data -> List.copyOf(data.spots))
    ).apply(instance, GrownTorchflowers::from));
    public static final SavedDataType<GrownTorchflowers> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "grown_torchflowers"),
            level -> new GrownTorchflowers(),
            level -> CODEC
    );

    private final Set<String> spots = new HashSet<>();

    private GrownTorchflowers() {
    }

    public static void mark(ServerLevel level, BlockPos pos) {
        GrownTorchflowers data = get(level.getServer());
        if (data.spots.add(key(level.dimension().identifier(), pos))) {
            data.setDirty();
        }
    }

    /** @return whether this position had bloomed from a crop */
    public static boolean claim(ServerLevel level, BlockPos pos) {
        GrownTorchflowers data = get(level.getServer());
        if (!data.spots.remove(key(level.dimension().identifier(), pos))) {
            return false;
        }
        data.setDirty();
        return true;
    }

    private static GrownTorchflowers get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private static GrownTorchflowers from(List<String> spots) {
        GrownTorchflowers data = new GrownTorchflowers();
        data.spots.addAll(spots);
        return data;
    }

    private static String key(Identifier dimension, BlockPos pos) {
        return dimension + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
