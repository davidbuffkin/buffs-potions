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

/** Crops that bonemeal has grown at least once. The mark lasts until that plant is broken. */
public final class BonemealedCrops extends SavedData {
    private static final Codec<BonemealedCrops> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.listOf().optionalFieldOf("spots", List.of()).forGetter(data -> List.copyOf(data.spots))
    ).apply(instance, BonemealedCrops::from));
    public static final SavedDataType<BonemealedCrops> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "bonemealed_crops"),
            level -> new BonemealedCrops(),
            level -> CODEC
    );

    private final Set<String> spots = new HashSet<>();

    private BonemealedCrops() {
    }

    public static void mark(ServerLevel level, BlockPos pos) {
        BonemealedCrops data = get(level.getServer());
        if (data.spots.add(key(level.dimension().identifier(), pos))) {
            data.setDirty();
        }
    }

    public static boolean marked(ServerLevel level, BlockPos pos) {
        return get(level.getServer()).spots.contains(key(level.dimension().identifier(), pos));
    }

    /** @return whether this position had been grown with bonemeal */
    public static boolean claim(ServerLevel level, BlockPos pos) {
        BonemealedCrops data = get(level.getServer());
        if (!data.spots.remove(key(level.dimension().identifier(), pos))) {
            return false;
        }
        data.setDirty();
        return true;
    }

    private static BonemealedCrops get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    private static BonemealedCrops from(List<String> spots) {
        BonemealedCrops data = new BonemealedCrops();
        data.spots.addAll(spots);
        return data;
    }

    private static String key(Identifier dimension, BlockPos pos) {
        return dimension + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
    }
}
