package com.buffspotions.effect;

import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModPotions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class FormationTest {
    @Test
    void onlyAirWaterAndLavaCanBeReplaced(MinecraftServer server) {
        assertTrue(Formation.canReplace(Blocks.AIR.defaultBlockState()));
        assertTrue(Formation.canReplace(Blocks.CAVE_AIR.defaultBlockState()));
        assertTrue(Formation.canReplace(Blocks.WATER.defaultBlockState()));
        assertTrue(Formation.canReplace(Blocks.LAVA.defaultBlockState()));
        assertFalse(Formation.canReplace(Blocks.STONE.defaultBlockState()));
        assertFalse(Formation.canReplace(Blocks.POWDER_SNOW.defaultBlockState()));
        assertFalse(Formation.canReplace(Blocks.SHORT_GRASS.defaultBlockState()));
    }

    @Test
    void formationCanSplashAndCannotLinger(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        ItemStack formation = PotionContents.createItemStack(Items.POTION, ModPotions.FORMATION);
        formation.set(ModDataComponents.ESSENCE.get(), EssenceSubject.ofItem(BuiltInRegistries.ITEM.wrapAsHolder(Items.STONE)));
        assertFalse(brewing.hasMix(formation, new ItemStack(Items.REDSTONE)));
        assertFalse(brewing.hasMix(formation, new ItemStack(Items.GLOWSTONE_DUST)));
        assertFalse(brewing.hasMix(formation, new ItemStack(Items.DRAGON_BREATH)));
        ItemStack splash = brewing.mix(new ItemStack(Items.GUNPOWDER), formation.copy());
        assertTrue(splash.is(Items.SPLASH_POTION));
        assertTrue(splash.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.FORMATION));
        assertFalse(brewing.hasMix(splash, new ItemStack(Items.DRAGON_BREATH)));
        assertFalse(brewing.mix(new ItemStack(Items.DRAGON_BREATH), splash.copy()).is(Items.LINGERING_POTION));
    }

    @Test
    void aPlayerDrinkTriesTheAirBelowTheirFeetFirst(MinecraftServer server) {
        BlockPos feet = new BlockPos(0, 64, 0);
        BlockState air = Blocks.AIR.defaultBlockState();
        assertEquals(feet.below(), Formation.playerDrinkPos(feet, air, air));
        assertEquals(feet.below(), Formation.playerDrinkPos(feet, air, Blocks.WATER.defaultBlockState()));
        assertEquals(feet.below(), Formation.playerDrinkPos(feet, air, Blocks.LAVA.defaultBlockState()));
        assertEquals(feet, Formation.playerDrinkPos(feet, air, Blocks.STONE.defaultBlockState()));
        assertEquals(feet, Formation.playerDrinkPos(feet, Blocks.WATER.defaultBlockState(), air));
    }

    @Test
    void theSplashUsesTheClosestOpenBlockWithinOneBlock(MinecraftServer server) {
        Vec3 hit = new Vec3(0.5, 1.0, 0.5);
        BlockPos above = new BlockPos(0, 1, 0);
        assertEquals(above, Formation.nearestOpen(hit, new BlockPos(0, 0, 0), Direction.UP, above::equals));
        assertNull(Formation.nearestOpen(hit, new BlockPos(0, 0, 0), Direction.UP, pos -> false));
        assertNull(Formation.nearestOpen(hit, new BlockPos(0, 0, 0), Direction.UP, pos -> pos.equals(new BlockPos(2, 0, 0))));
    }
}
