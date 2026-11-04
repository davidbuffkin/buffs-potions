package com.buffspotions.recipebook;

import com.buffspotions.potion.Remedies;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class RecipeMysteryTest {
    @Test
    void oneEffectCoversEveryVariant(MinecraftServer server) {
        ItemStack swiftness = PotionContents.createItemStack(Items.POTION, Potions.SWIFTNESS);
        ItemStack splash = PotionContents.createItemStack(Items.SPLASH_POTION, Potions.STRONG_SWIFTNESS);
        Set<String> speed = RecipeMystery.effectIds(swiftness);
        assertTrue(speed.contains("minecraft:speed"));
        assertTrue(RecipeMystery.concealed(Set.of(), swiftness));
        assertTrue(RecipeMystery.concealed(Set.of(), splash));
        assertFalse(RecipeMystery.concealed(speed, swiftness));
        assertFalse(RecipeMystery.concealed(speed, splash));
    }

    @Test
    void aRemedyCountsAsTheEffectItClears(MinecraftServer server) {
        ItemStack remedy = Remedies.create(MobEffects.WEAKNESS, Items.POTION);
        Set<String> ids = RecipeMystery.effectIds(remedy);
        assertTrue(ids.contains("minecraft:weakness"));
        assertFalse(ids.contains("buffs_potions:remedy"));
        assertFalse(RecipeMystery.concealed(ids, remedy));
    }

    @Test
    void potionsWithoutAnEffectStayVisible(MinecraftServer server) {
        ItemStack water = PotionContents.createItemStack(Items.POTION, Potions.WATER);
        assertTrue(RecipeMystery.effectIds(water).isEmpty());
        assertFalse(RecipeMystery.concealed(Set.of(), water));
    }
}
