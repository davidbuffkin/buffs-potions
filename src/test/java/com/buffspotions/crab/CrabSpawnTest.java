package com.buffspotions.crab;

import net.minecraft.world.entity.EntitySpawnReason;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrabSpawnTest {
    @Test
    void oneInTenNaturalCrabsIsABaby() {
        assertTrue(Crab.spawnsAsBaby(EntitySpawnReason.NATURAL, 0));
        assertTrue(Crab.spawnsAsBaby(EntitySpawnReason.CHUNK_GENERATION, 0));
        assertFalse(Crab.spawnsAsBaby(EntitySpawnReason.NATURAL, 1));
        assertFalse(Crab.spawnsAsBaby(EntitySpawnReason.NATURAL, 9));
        assertFalse(Crab.spawnsAsBaby(EntitySpawnReason.BREEDING, 0));
        assertFalse(Crab.spawnsAsBaby(EntitySpawnReason.SPAWN_ITEM_USE, 0));
        assertFalse(Crab.spawnsAsBaby(EntitySpawnReason.COMMAND, 0));
    }
}
