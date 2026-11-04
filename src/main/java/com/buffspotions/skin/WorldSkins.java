package com.buffspotions.skin;

import com.buffspotions.BuffsPotions;
import com.buffspotions.network.SkinRememberedPayload;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.neoforge.network.PacketDistributor;

/** Skins this world has already seen, so later play can draw them without a lookup. */
public final class WorldSkins extends SavedData {
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Codec<byte[]> BYTES = Codec.BYTE_BUFFER.xmap(buffer -> {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }, ByteBuffer::wrap);
    private static final Codec<CachedSkin> SKIN = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("name").forGetter(CachedSkin::name),
            UUIDUtil.CODEC.optionalFieldOf("id").forGetter(CachedSkin::id),
            Codec.BOOL.optionalFieldOf("slim", false).forGetter(CachedSkin::slim),
            BYTES.fieldOf("skin").forGetter(CachedSkin::skin),
            BYTES.optionalFieldOf("cape", new byte[0]).forGetter(CachedSkin::cape),
            BYTES.optionalFieldOf("elytra", new byte[0]).forGetter(CachedSkin::elytra),
            Codec.BOOL.optionalFieldOf("subject", false).forGetter(CachedSkin::subjectOwned)
    ).apply(instance, CachedSkin::new));
    private static final Codec<WorldSkins> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            SKIN.listOf().optionalFieldOf("skins", List.of()).forGetter(WorldSkins::stored)
    ).apply(instance, WorldSkins::from));
    public static final SavedDataType<WorldSkins> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "skins"),
            level -> new WorldSkins(),
            level -> CODEC
    );

    private final Map<String, CachedSkin> byName = new LinkedHashMap<>();
    private final Map<UUID, String> byId = new LinkedHashMap<>();

    private WorldSkins() {
    }

    public static WorldSkins create() {
        return new WorldSkins();
    }

    public static WorldSkins get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public static Codec<WorldSkins> codec() {
        return CODEC;
    }

    /**
     * Keeps a skin the uploader already has. The player themselves can replace their entry.
     * A disguise lookup can fill a name this world has not seen, and does not replace that player's own copy.
     */
    public boolean remember(
            String name,
            Optional<UUID> id,
            boolean slim,
            byte[] skin,
            byte[] cape,
            byte[] elytra,
            UUID uploader
    ) {
        if (name == null || !NAME.matcher(name).matches() || !SkinPixels.skin(skin) || !SkinPixels.extra(cape) || !SkinPixels.extra(elytra)) {
            return false;
        }
        boolean subject = id.isPresent() && id.get().equals(uploader);
        String key = name.toLowerCase(Locale.ROOT);
        CachedSkin previous = byName.get(key);
        if (previous != null && previous.subjectOwned() && !subject) {
            return false;
        }
        CachedSkin incoming = new CachedSkin(name, id, slim, skin.clone(), cape.clone(), elytra.clone(), subject);
        if (previous != null && previous.sameImage(incoming) && previous.id().equals(id) && previous.subjectOwned() == subject) {
            return false;
        }
        if (previous != null) {
            previous.id().ifPresent(byId::remove);
        }
        byName.put(key, incoming);
        incoming.id().ifPresent(uuid -> byId.put(uuid, key));
        setDirty();
        return true;
    }

    public Optional<CachedSkin> find(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byName.get(name.toLowerCase(Locale.ROOT)));
    }

    public Optional<CachedSkin> find(UUID id) {
        String key = byId.get(id);
        return key == null ? Optional.empty() : Optional.ofNullable(byName.get(key));
    }

    public void sendTo(ServerPlayer player) {
        for (CachedSkin skin : byName.values()) {
            PacketDistributor.sendToPlayer(player, remembered(skin));
        }
    }

    public static SkinRememberedPayload remembered(CachedSkin skin) {
        return new SkinRememberedPayload(skin.name(), skin.id(), skin.slim(), skin.skin(), skin.cape(), skin.elytra());
    }

    private List<CachedSkin> stored() {
        return List.copyOf(byName.values());
    }

    private static WorldSkins from(List<CachedSkin> skins) {
        WorldSkins data = new WorldSkins();
        for (CachedSkin skin : skins) {
            if (!NAME.matcher(skin.name()).matches() || !SkinPixels.skin(skin.skin())) {
                continue;
            }
            String key = skin.name().toLowerCase(Locale.ROOT);
            data.byName.put(key, skin);
            skin.id().ifPresent(id -> data.byId.put(id, key));
        }
        return data;
    }
}
