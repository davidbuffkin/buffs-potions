package com.buffspotions.still;

import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.registry.ModPotions;
import com.buffspotions.registry.ModRecipes;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class StillRecipeBookTest {
    @Test
    void stillBookGroupsMundaneSplashAndEssence(MinecraftServer server) {
        StillRecipeCatalog catalog = StillRecipeCatalog.get();
        ResourceKey<Recipe<?>> night = key("still/mundane/minecraft/golden_carrot");
        ResourceKey<Recipe<?>> splashNight = key("still/splash/mundane/minecraft/golden_carrot");
        ResourceKey<Recipe<?>> splashMilk = key("still/splash/milk");
        ResourceKey<Recipe<?>> suspicious = key("still/suspicious/beetroot");
        ResourceKey<Recipe<?>> remedy = key("still/remedy/minecraft/blaze_powder");
        ResourceKey<Recipe<?>> splashRemedy = key("still/splash/remedy/minecraft/blaze_powder");
        ResourceKey<Recipe<?>> healingRemedy = key("still/remedy/minecraft/glistering_melon_slice");
        ResourceKey<Recipe<?>> essence = key("still/essence/minecraft/stone");

        assertTrue(server.getRecipeManager().byKey(night).isPresent());
        assertEquals(StillRecipeCatalog.Kind.MUNDANE, kind(catalog, night));
        assertEquals("", server.getRecipeManager().byKey(night).orElseThrow().value().group());
        assertEquals(StillRecipeCatalog.SPLASH_GROUP, server.getRecipeManager().byKey(splashNight).orElseThrow().value().group());
        assertEquals(StillRecipeCatalog.SPLASH_GROUP, server.getRecipeManager().byKey(splashMilk).orElseThrow().value().group());
        assertEquals(StillRecipeCatalog.SPLASH_GROUP, server.getRecipeManager().byKey(splashRemedy).orElseThrow().value().group());
        assertEquals(StillRecipeCatalog.ESSENCE_GROUP, server.getRecipeManager().byKey(essence).orElseThrow().value().group());
        assertEquals(StillRecipeCatalog.REMEDY_GROUP, server.getRecipeManager().byKey(remedy).orElseThrow().value().group());
        assertTrue(server.getRecipeManager().byKey(suspicious).isPresent());
        assertTrue(server.getRecipeManager().byKey(remedy).isPresent());
        assertFalse(server.getRecipeManager().byKey(healingRemedy).isPresent());

        Set<net.minecraft.world.item.Item> carrot = Set.of(Items.GOLDEN_CARROT);
        Set<net.minecraft.world.item.Item> none = Set.of();
        StillRecipeCatalog.Entry nightEntry = entry(catalog, night);
        StillRecipeCatalog.Entry splashEntry = entry(catalog, splashNight);
        StillRecipeCatalog.Entry milkEntry = entry(catalog, splashMilk);
        StillRecipeCatalog.Entry essenceEntry = entry(catalog, essence);
        assertTrue(catalog.shouldUnlock(nightEntry, carrot, none, none, false, false));
        assertTrue(catalog.shouldUnlock(nightEntry, none, carrot, none, false, false));
        assertFalse(catalog.shouldUnlock(nightEntry, none, none, none, false, false));
        assertTrue(catalog.shouldUnlock(splashEntry, carrot, none, none, false, false));
        assertTrue(catalog.shouldUnlock(milkEntry, none, none, none, false, true));
        assertFalse(catalog.shouldUnlock(milkEntry, none, none, none, false, false));
        assertFalse(catalog.shouldUnlock(essenceEntry, Set.of(Items.STONE), none, Set.of(Items.STONE), false, false));
        assertTrue(catalog.shouldUnlock(essenceEntry, Set.of(Items.STONE), none, Set.of(Items.STONE), true, false));
        assertFalse(catalog.shouldUnlock(essenceEntry, none, Set.of(Items.STONE), none, true, false));
        assertTrue(catalog.shouldUnlock(essenceEntry, none, none, Set.of(Items.STONE), true, false));

        ItemStack mundane = result(server, night);
        assertTrue(MundaneBrewing.isMundane(mundane));
        assertTrue(mundane.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.NIGHT_VISION));
        StillDistillRecipe nightRecipe = (StillDistillRecipe) server.getRecipeManager().byKey(night).orElseThrow().value();
        assertTrue(nightRecipe.bottle().test(PotionContents.createItemStack(Items.POTION, Potions.MUNDANE)));
        assertFalse(nightRecipe.bottle().test(PotionContents.createItemStack(Items.POTION, Potions.WATER)));
        ResourceKey<Recipe<?>> plain = key("still/mundane_potion");
        assertEquals(ModRecipes.OTHER.get(), server.getRecipeManager().byKey(plain).orElseThrow().value().recipeBookCategory());
        ItemStack plainMundane = result(server, plain);
        assertTrue(plainMundane.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.MUNDANE));
        assertFalse(MundaneBrewing.isMundane(plainMundane));
        assertTrue(((StillDistillRecipe) server.getRecipeManager().byKey(plain).orElseThrow().value()).ingredient().test(new ItemStack(Items.REDSTONE)));
        assertTrue(((StillDistillRecipe) server.getRecipeManager().byKey(plain).orElseThrow().value()).bottle().test(PotionContents.createItemStack(Items.POTION, Potions.WATER)));
        assertEquals(ModRecipes.MUNDANE.get(), server.getRecipeManager().byKey(night).orElseThrow().value().recipeBookCategory());
        assertTrue(result(server, splashMilk).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.MILK));

        ResourceKey<Recipe<?>> lingeringNight = key("still/lingering/mundane/minecraft/golden_carrot");
        ResourceKey<Recipe<?>> lingeringMilk = key("still/lingering/milk");
        ResourceKey<Recipe<?>> lingeringRemedy = key("still/lingering/remedy/minecraft/blaze_powder");
        ResourceKey<Recipe<?>> lingeringSlowness = key("still/lingering/mundane/invert/minecraft/sugar");
        assertEquals(StillRecipeCatalog.LINGERING_GROUP, server.getRecipeManager().byKey(lingeringNight).orElseThrow().value().group());
        assertEquals(StillRecipeCatalog.LINGERING_GROUP, server.getRecipeManager().byKey(lingeringMilk).orElseThrow().value().group());
        assertEquals(StillRecipeCatalog.LINGERING_GROUP, server.getRecipeManager().byKey(lingeringRemedy).orElseThrow().value().group());
        assertEquals(StillRecipeCatalog.LINGERING_GROUP, server.getRecipeManager().byKey(lingeringSlowness).orElseThrow().value().group());
        assertEquals(ModRecipes.OTHER.get(), server.getRecipeManager().byKey(lingeringNight).orElseThrow().value().recipeBookCategory());
        ItemStack lingering = result(server, lingeringNight);
        assertTrue(lingering.is(Items.LINGERING_POTION));
        assertTrue(lingering.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.NIGHT_VISION));
        assertTrue(MundaneBrewing.isMundane(lingering));
        StillDistillRecipe lingeringRecipe = (StillDistillRecipe) server.getRecipeManager().byKey(lingeringNight).orElseThrow().value();
        assertTrue(lingeringRecipe.ingredient().test(new ItemStack(Items.DRAGON_BREATH)));
        assertTrue(lingeringRecipe.bottle().test(result(server, splashNight)));
        assertTrue(result(server, lingeringMilk).is(Items.LINGERING_POTION));
        assertTrue(result(server, lingeringMilk).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.MILK));
        Set<net.minecraft.world.item.Item> breath = Set.of(Items.DRAGON_BREATH);
        StillRecipeCatalog.Entry lingeringEntry = entry(catalog, lingeringNight);
        StillRecipeCatalog.Entry lingeringMilkEntry = entry(catalog, lingeringMilk);
        assertFalse(catalog.shouldUnlock(lingeringEntry, carrot, none, none, false, false));
        assertFalse(catalog.shouldUnlock(lingeringEntry, none, none, breath, false, false));
        assertTrue(catalog.shouldUnlock(lingeringEntry, carrot, none, breath, false, false));
        assertFalse(catalog.shouldUnlock(lingeringMilkEntry, breath, none, breath, false, false));
        assertFalse(catalog.shouldUnlock(lingeringMilkEntry, none, none, none, false, true));
        assertTrue(catalog.shouldUnlock(lingeringMilkEntry, none, none, breath, false, true));

        ResourceKey<Recipe<?>> breeze = key("still/mundane/minecraft/breeze_rod");
        ResourceKey<Recipe<?>> windCharge = key("still/mundane/minecraft/wind_charge");
        assertTrue(server.getRecipeManager().byKey(breeze).isPresent());
        assertFalse(server.getRecipeManager().byKey(windCharge).isPresent());
        assertTrue(result(server, breeze).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WIND_CHARGED));
        assertEquals(ModRecipes.HARMFUL.get(), server.getRecipeManager().byKey(breeze).orElseThrow().value().recipeBookCategory());

        ResourceKey<Recipe<?>> slowness = key("still/mundane/invert/minecraft/sugar");
        ResourceKey<Recipe<?>> invisibility = key("still/mundane/invert/minecraft/golden_carrot");
        ResourceKey<Recipe<?>> depths = key("still/mundane/invert/minecraft/glow_berries");
        ResourceKey<Recipe<?>> confusion = key("still/mundane/invert/minecraft/experience_bottle");
        ResourceKey<Recipe<?>> turtle = key("still/mundane/minecraft/turtle_helmet");
        assertTrue(server.getRecipeManager().byKey(slowness).isPresent());
        assertTrue(server.getRecipeManager().byKey(invisibility).isPresent());
        assertTrue(server.getRecipeManager().byKey(depths).isPresent());
        assertTrue(server.getRecipeManager().byKey(confusion).isPresent());
        assertTrue(result(server, slowness).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.SLOWNESS));
        assertTrue(MundaneBrewing.isMundane(result(server, slowness)));
        assertEquals(ModRecipes.HARMFUL.get(), server.getRecipeManager().byKey(slowness).orElseThrow().value().recipeBookCategory());
        assertEquals(ModRecipes.MUNDANE.get(), server.getRecipeManager().byKey(invisibility).orElseThrow().value().recipeBookCategory());
        assertEquals(ModRecipes.HARMFUL.get(), server.getRecipeManager().byKey(depths).orElseThrow().value().recipeBookCategory());
        assertEquals(ModRecipes.MUNDANE.get(), server.getRecipeManager().byKey(turtle).orElseThrow().value().recipeBookCategory());
        assertFalse(catalog.shouldUnlock(entry(catalog, slowness), Set.of(Items.SUGAR), none, none, false, false, Set.of()));
        assertTrue(catalog.shouldUnlock(entry(catalog, slowness), Set.of(Items.SUGAR), none, none, false, false, Set.of("minecraft:speed")));
        assertTrue(entry(catalog, slowness).requiresKnown().contains("minecraft:speed"));
        RecipeHolder<?> slownessRecipe = server.getRecipeManager().byKey(slowness).orElseThrow();
        assertTrue(((StillDistillRecipe) slownessRecipe.value()).ingredient().test(new ItemStack(Items.FERMENTED_SPIDER_EYE)));
    }

    @Test
    void stillSplashesMilk(MinecraftServer server) {
        PotionBrewing brewing = server.potionBrewing();
        ItemStack splash = StillBrewing.mix(brewing, new ItemStack(Items.GUNPOWDER), FlagonContents.milkTemplate());
        assertTrue(splash.is(Items.SPLASH_POTION));
        assertTrue(splash.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.MILK));
        assertTrue(StillBrewing.isBottle(splash));
        ItemStack lingering = StillBrewing.mix(brewing, new ItemStack(Items.DRAGON_BREATH), splash);
        assertTrue(lingering.is(Items.LINGERING_POTION));
        assertTrue(lingering.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.MILK));

        ItemStack mundane = ((StillDistillRecipe) server.getRecipeManager()
                .byKey(key("still/mundane/minecraft/golden_carrot")).orElseThrow().value()).result().create();
        ItemStack splashMundane = StillBrewing.mix(brewing, new ItemStack(Items.GUNPOWDER), mundane);
        ItemStack lingeringMundane = StillBrewing.mix(brewing, new ItemStack(Items.DRAGON_BREATH), splashMundane);
        assertTrue(lingeringMundane.is(Items.LINGERING_POTION));
        assertTrue(MundaneBrewing.isMundane(lingeringMundane));
        assertTrue(lingeringMundane.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.NIGHT_VISION));
    }

    private static StillRecipeCatalog.Kind kind(StillRecipeCatalog catalog, ResourceKey<Recipe<?>> id) {
        return entry(catalog, id).kind();
    }

    private static StillRecipeCatalog.Entry entry(StillRecipeCatalog catalog, ResourceKey<Recipe<?>> id) {
        return catalog.entries().stream().filter(entry -> entry.id().equals(id)).findFirst().orElseThrow();
    }

    private static ItemStack result(MinecraftServer server, ResourceKey<Recipe<?>> id) {
        return ((StillDistillRecipe) server.getRecipeManager().byKey(id).orElseThrow().value()).result().create();
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("buffs_potions", path));
    }
}
