package com.buffspotions.flagon;

import com.buffspotions.BuffsPotions;
import com.mojang.serialization.Codec;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Ender flagon liquid kept for players who are offline, so an anchored flagon can still be used. */
public final class EnderFlagonStores extends SavedData {
    private static final Codec<EnderFlagonStores> CODEC = Codec.unboundedMap(UUIDUtil.STRING_CODEC, FlagonContents.CODEC)
            .xmap(EnderFlagonStores::new, data -> Map.copyOf(data.contents));
    public static final SavedDataType<EnderFlagonStores> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "ender_flagons"),
            level -> new EnderFlagonStores(),
            level -> CODEC
    );

    private final Map<UUID, FlagonContents> contents = new HashMap<>();

    private EnderFlagonStores() {
    }

    private EnderFlagonStores(Map<UUID, FlagonContents> contents) {
        this.contents.putAll(contents);
    }

    public static EnderFlagonStores get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean has(UUID player) {
        return contents.containsKey(player);
    }

    public FlagonContents get(UUID player) {
        return contents.getOrDefault(player, FlagonContents.EMPTY);
    }

    public void put(UUID player, FlagonContents contents) {
        this.contents.put(player, contents);
        setDirty();
    }
}
