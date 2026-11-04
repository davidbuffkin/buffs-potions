package com.buffspotions.skin;

import com.mojang.serialization.DynamicOps;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class WorldSkinsTest {
    @Test
    void aWorldRemembersPlayersAndDisguiseLookups(MinecraftServer server) {
        WorldSkins skins = WorldSkins.create();
        UUID ada = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID stranger = UUID.fromString("22222222-2222-2222-2222-222222222222");
        byte[] first = image(1);
        byte[] changed = image(2);
        assertFalse(skins.remember("bad name", Optional.of(ada), false, first, new byte[0], new byte[0], ada));
        assertTrue(skins.remember("Ada", Optional.of(ada), false, first, new byte[0], new byte[0], ada));
        assertFalse(skins.remember("Ada", Optional.of(ada), false, first, new byte[0], new byte[0], ada));
        assertFalse(skins.remember("ada", Optional.of(ada), true, changed, new byte[0], new byte[0], stranger));
        assertArrayEquals(first, skins.find("ADA").orElseThrow().skin());
        assertTrue(skins.remember("Ada", Optional.of(ada), true, changed, new byte[0], new byte[0], ada));
        assertTrue(skins.find(ada).orElseThrow().slim());
        assertTrue(skins.remember("Notch", Optional.of(stranger), false, first, new byte[0], new byte[0], ada));
        assertEquals("Notch", skins.find(stranger).orElseThrow().name());

        DynamicOps<Tag> ops = NbtOps.INSTANCE;
        WorldSkins loaded = WorldSkins.codec().parse(ops, WorldSkins.codec().encodeStart(ops, skins).getOrThrow()).getOrThrow();
        assertArrayEquals(changed, loaded.find("Ada").orElseThrow().skin());
        assertEquals("Notch", loaded.find("notch").orElseThrow().name());
        assertTrue(loaded.find(ada).orElseThrow().subjectOwned());
        assertFalse(loaded.find("Notch").orElseThrow().subjectOwned());
    }

    private static byte[] image(int pixel) {
        int[] argb = new int[64 * 64];
        argb[0] = pixel;
        return SkinPixels.pack(64, 64, argb);
    }
}
