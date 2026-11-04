package com.buffspotions.skin;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

/** One player's skin, remembered for this world. */
public record CachedSkin(
        String name,
        Optional<UUID> id,
        boolean slim,
        byte[] skin,
        byte[] cape,
        byte[] elytra,
        boolean subjectOwned
) {
    public boolean sameImage(CachedSkin other) {
        return slim == other.slim
                && Arrays.equals(skin, other.skin)
                && Arrays.equals(cape, other.cape)
                && Arrays.equals(elytra, other.elytra);
    }
}
