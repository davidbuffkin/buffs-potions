package com.buffspotions.effect;

import net.minecraft.server.MinecraftServer;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EphemeralTestServerProvider.class)
class PoisonousPotatoDropTest {
    @Test
    void badLuckRaisesTheMatureCropChance(MinecraftServer server) {
        assertEquals(0.02F, PoisonousPotatoDrop.chance(-1, -1), 0.0F);
        assertEquals(0.02F, PoisonousPotatoDrop.chance(0, -1), 0.0F);
        assertEquals(0.02F, PoisonousPotatoDrop.chance(3, -1), 0.0F);
        assertEquals(0.02F, PoisonousPotatoDrop.chance(0, 0), 0.0F);
        assertEquals(0.02F, PoisonousPotatoDrop.chance(1, 1), 0.0F);
        assertEquals(0.02F, PoisonousPotatoDrop.chance(1, 0), 0.0F);

        assertEquals(0.04F, PoisonousPotatoDrop.chance(-1, 0), 0.0F);
        assertEquals(0.08F, PoisonousPotatoDrop.chance(-1, 1), 0.0F);
        assertEquals(0.08F, PoisonousPotatoDrop.chance(-1, 3), 0.0F);
        assertEquals(0.08F, PoisonousPotatoDrop.chance(0, 1), 0.0F);
    }
}
