package com.buffspotions.trade;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.trading.TradeSet;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class WanderingTraderTradeTest {
    @Test
    void specialOffersIncludeFlowerWalkingAndAnEmptyFlagon(MinecraftServer server) {
        TradeSet set = server.registryAccess()
                .lookupOrThrow(Registries.TRADE_SET)
                .getOrThrow(ResourceKey.create(Registries.TRADE_SET, Identifier.parse("minecraft:wandering_trader/uncommon")))
                .value();
        List<String> trades = set.getTrades().stream()
                .map(holder -> holder.unwrapKey().orElseThrow().identifier().toString())
                .toList();
        assertEquals(2, set.calculateNumberOfTrades(null));
        assertTrue(trades.contains("minecraft:wandering_trader/emerald_long_invisibility_potion"));
        assertTrue(trades.contains("buffs_potions:wandering_trader/flower_walking"));
        assertTrue(trades.contains("buffs_potions:wandering_trader/empty_flagon"));
        assertEquals(trades.size(), trades.stream().distinct().count());
    }
}
