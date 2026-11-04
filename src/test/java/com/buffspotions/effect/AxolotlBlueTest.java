package com.buffspotions.effect;

import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EphemeralTestServerProvider.class)
class AxolotlBlueTest {
    @Test
    void luckRaisesTheBlueChance(MinecraftServer server) {
        assertEquals(2, AxolotlBlue.weight(-1));
        assertEquals(3, AxolotlBlue.weight(0));
        assertEquals(4, AxolotlBlue.weight(1));
        assertEquals(4, AxolotlBlue.weight(3));
    }
}
