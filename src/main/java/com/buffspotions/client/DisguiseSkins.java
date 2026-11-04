package com.buffspotions.client;

import com.buffspotions.registry.ModAttachments;
import com.mojang.authlib.GameProfile;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.util.Util;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Resolves the disguise subject's skin. A name that does not resolve keeps its effect name and uses Big_Buff's skin.
 */
public final class DisguiseSkins {
    public static final String FALLBACK_NAME = "Big_Buff";

    private static final ConcurrentHashMap<String, Lookup> LOOKUPS = new ConcurrentHashMap<>();

    private DisguiseSkins() {
    }

    /**
     * The disguise subject, or null when this player should be drawn as themselves.
     * Other clients never receive the effect list, so they key off the synced name.
     * A spyglass reveals real skins and nametags only to the player looking through it.
     */
    public static String subjectName(Entity entity) {
        if (viewerIsScoping()) {
            return null;
        }
        String name = entity.getData(ModAttachments.DISGUISE.get());
        if (name == null || name.isBlank()) {
            return null;
        }
        return name;
    }

    /** Resolved skin for a disguise subject, or the default skin while the lookup is still running. */
    public static PlayerSkin skin(String name) {
        PlayerSkin resolved = resolve(name);
        if (resolved != null) {
            return resolved;
        }
        if (missing(name) && !name.equalsIgnoreCase(FALLBACK_NAME)) {
            PlayerSkin fallback = resolve(FALLBACK_NAME);
            if (fallback != null) {
                return fallback;
            }
        }
        return DefaultPlayerSkin.getDefaultSkin();
    }

    private static boolean viewerIsScoping() {
        Player viewer = Minecraft.getInstance().player;
        return viewer != null && viewer.isScoping();
    }

    public static PlayerSkin apply(AbstractClientPlayer player, PlayerSkin own) {
        String name = subjectName(player);
        if (name == null) {
            return own;
        }
        PlayerSkin resolved = resolve(name);
        if (resolved != null) {
            return resolved;
        }
        if (missing(name) && !name.equalsIgnoreCase(FALLBACK_NAME)) {
            PlayerSkin fallback = resolve(FALLBACK_NAME);
            if (fallback != null) {
                return fallback;
            }
        }
        return own;
    }

    private static PlayerSkin resolve(String name) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection != null) {
            PlayerInfo online = connection.getPlayerInfoIgnoreCase(name);
            if (online != null) {
                PlayerSkin skin = online.getSkin();
                if (skin != null && skin.body() instanceof ClientAsset.DownloadedTexture) {
                    SkinMemory.keep(name, online.getProfile().id(), skin);
                    return skin;
                }
            }
        }
        PlayerSkin cached = SkinMemory.skin(name);
        if (cached != null) {
            return cached;
        }
        String key = name.toLowerCase(Locale.ROOT);
        Lookup lookup = LOOKUPS.get(key);
        if (lookup == null) {
            if (LOOKUPS.putIfAbsent(key, Lookup.pending()) == null) {
                search(name, key);
            }
            return null;
        }
        return lookup.status == Status.FOUND ? lookup.skin : null;
    }

    private static boolean missing(String name) {
        Lookup lookup = LOOKUPS.get(name.toLowerCase(Locale.ROOT));
        return lookup != null && lookup.status == Status.MISSING;
    }

    private static void search(String name, String key) {
        Util.nonCriticalIoPool().execute(() -> {
            Optional<GameProfile> profile;
            try {
                profile = Minecraft.getInstance().services().profileResolver().fetchByName(name);
            } catch (RuntimeException ignored) {
                profile = Optional.empty();
            }
            Optional<GameProfile> found = profile;
            Minecraft.getInstance().execute(() -> finish(key, found));
        });
    }

    private static void finish(String key, Optional<GameProfile> profile) {
        if (profile.isEmpty()) {
            LOOKUPS.put(key, Lookup.missing());
            return;
        }
        try {
            Minecraft.getInstance().getSkinManager().get(profile.get()).thenAccept(skin ->
                    Minecraft.getInstance().execute(() -> {
                        skin.ifPresent(found -> SkinMemory.keep(key, profile.get().id(), found));
                        LOOKUPS.put(key, skin.map(Lookup::found).orElseGet(Lookup::missing));
                    })
            );
        } catch (RuntimeException ignored) {
            LOOKUPS.put(key, Lookup.missing());
        }
    }

    private enum Status {
        PENDING,
        FOUND,
        MISSING
    }

    private record Lookup(Status status, PlayerSkin skin) {
        private static Lookup pending() {
            return new Lookup(Status.PENDING, null);
        }

        private static Lookup missing() {
            return new Lookup(Status.MISSING, null);
        }

        private static Lookup found(PlayerSkin skin) {
            return new Lookup(Status.FOUND, skin);
        }
    }
}
