package com.buffspotions.witch;

import com.buffspotions.registry.ModPotions;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(EphemeralTestServerProvider.class)
class WitchThrowTest {
    @Test
    void fullHealthCloseRangeIsAlwaysConfoundingAndOtherwiseOneInFive(MinecraftServer server) {
        assertEquals(ModPotions.INSTANT_CONFUSION.value(), WitchThrow.choose(true, 4, Potions.HARMING).value());
        assertEquals(ModPotions.INSTANT_CONFUSION.value(), WitchThrow.choose(false, 0, Potions.POISON).value());
        assertEquals(Potions.SLOWNESS.value(), WitchThrow.choose(false, 1, Potions.SLOWNESS).value());
        assertEquals(Potions.WEAKNESS.value(), WitchThrow.choose(false, 4, Potions.WEAKNESS).value());
        assertEquals(Potions.HEALING.value(), WitchThrow.choose(false, 2, Potions.HEALING).value());
    }
}
