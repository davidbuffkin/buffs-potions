package com.buffspotions.stand;

import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModRecipes;
import java.util.Set;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class StandRecipeBookTest {
    @Test
    void standBookListsBasePotionsAndModifiers(MinecraftServer server) {
        StandRecipeCatalog catalog = StandRecipeCatalog.get();
        ResourceKey<Recipe<?>> night = key("stand/base/minecraft/golden_carrot");
        ResourceKey<Recipe<?>> invisibility = key("stand/invert/minecraft/golden_carrot");
        ResourceKey<Recipe<?>> slowness = key("stand/invert/minecraft/sugar");
        ResourceKey<Recipe<?>> depths = key("stand/invert/minecraft/glow_berries");
        ResourceKey<Recipe<?>> heights = key("stand/base/minecraft/glow_berries");
        ResourceKey<Recipe<?>> turtle = key("stand/base/minecraft/turtle_helmet");
        ResourceKey<Recipe<?>> breeze = key("stand/base/minecraft/breeze_rod");
        ResourceKey<Recipe<?>> windCharge = key("stand/base/minecraft/wind_charge");
        ResourceKey<Recipe<?>> awkward = key("stand/awkward");
        ResourceKey<Recipe<?>> mundane = key("stand/mundane/glowstone");
        ResourceKey<Recipe<?>> mundaneRedstone = key("stand/mundane/redstone");
        ResourceKey<Recipe<?>> thicken = key("stand/thicken/minecraft/sugar");
        ResourceKey<Recipe<?>> extend = key("stand/extend/minecraft/sugar");
        ResourceKey<Recipe<?>> windThicken = key("stand/thicken/minecraft/breeze_rod");
        ResourceKey<Recipe<?>> perception = key("stand/param/block_perception");
        ResourceKey<Recipe<?>> perceptionItem = key("stand/param/block_perception/minecraft/stone");
        ResourceKey<Recipe<?>> cracking = key("stand/base/minecraft/diamond");
        ResourceKey<Recipe<?>> stacking = key("stand/base/minecraft/brick");
        ResourceKey<Recipe<?>> splash = key("stand/splash/minecraft/golden_carrot");
        ResourceKey<Recipe<?>> splashPerception = key("stand/splash/param/block_perception");
        ResourceKey<Recipe<?>> linger = key("stand/linger/minecraft/golden_carrot");
        ResourceKey<Recipe<?>> lingerPerception = key("stand/linger/param/block_perception");
        ResourceKey<Recipe<?>> incarnationLinger = key("stand/linger/param/incarnation");

        StandBrewRecipe nightRecipe = recipe(server, night);
        assertTrue(nightRecipe.bottle().test(PotionContents.createItemStack(Items.POTION, Potions.AWKWARD)));
        assertTrue(nightRecipe.ingredient().test(new ItemStack(Items.GOLDEN_CARROT)));
        assertTrue(result(nightRecipe).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.NIGHT_VISION));
        assertEquals(ModRecipes.STAND_HELPFUL.get(), nightRecipe.recipeBookCategory());
        assertEquals(ModRecipes.STAND_HELPFUL.get(), recipe(server, invisibility).recipeBookCategory());
        assertTrue(recipe(server, invisibility).ingredient().test(new ItemStack(Items.FERMENTED_SPIDER_EYE)));
        assertTrue(result(recipe(server, slowness)).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.SLOWNESS));
        assertEquals(ModRecipes.STAND_HARMFUL.get(), recipe(server, slowness).recipeBookCategory());
        assertEquals(ModRecipes.STAND_HARMFUL.get(), recipe(server, depths).recipeBookCategory());
        assertTrue(result(recipe(server, depths)).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(com.buffspotions.registry.ModPotions.DEPTHS));
        assertEquals(ModRecipes.STAND_HELPFUL.get(), recipe(server, heights).recipeBookCategory());
        assertEquals(ModRecipes.STAND_HELPFUL.get(), recipe(server, turtle).recipeBookCategory());
        assertTrue(result(recipe(server, breeze)).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WIND_CHARGED));
        assertEquals(ModRecipes.STAND_HARMFUL.get(), recipe(server, breeze).recipeBookCategory());
        assertFalse(server.getRecipeManager().byKey(windCharge).isPresent());
        assertFalse(server.getRecipeManager().byKey(windThicken).isPresent());
        assertFalse(strongSwiftnessInversion(server, catalog));

        assertTrue(result(recipe(server, awkward)).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.AWKWARD));
        assertEquals(ModRecipes.STAND_UTILITY.get(), recipe(server, awkward).recipeBookCategory());
        assertFalse(server.getRecipeManager().byKey(mundane).isPresent());
        StandBrewRecipe plainMundane = recipe(server, mundaneRedstone);
        assertEquals(ModRecipes.STAND_UTILITY.get(), plainMundane.recipeBookCategory());
        assertTrue(plainMundane.ingredient().test(new ItemStack(Items.REDSTONE)));
        assertTrue(plainMundane.bottle().test(PotionContents.createItemStack(Items.POTION, Potions.WATER)));
        assertTrue(result(plainMundane).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.MUNDANE));
        assertFalse(result(plainMundane).getOrDefault(com.buffspotions.registry.ModDataComponents.MUNDANE.get(), false));
        assertEquals(StandRecipeCatalog.THICKEN_GROUP, recipe(server, thicken).group());
        assertTrue(result(recipe(server, thicken)).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.STRONG_SWIFTNESS));
        assertEquals("swiftness", StandSubjects.potionCycleKey(result(recipe(server, thicken))));
        assertEquals("swiftness", StandSubjects.potionCycleKey(result(recipe(server, extend))));
        assertEquals("swiftness", StandSubjects.potionCycleKey(result(recipe(server, key("stand/splash/minecraft/sugar")))));
        assertEquals("swiftness", StandSubjects.potionCycleKey(result(recipe(server, key("stand/linger/minecraft/sugar")))));
        assertEquals("night_vision", StandSubjects.potionCycleKey(result(recipe(server, splash))));
        assertEquals(StandRecipeCatalog.SPLASH_GROUP, recipe(server, splash).group());
        assertEquals(StandRecipeCatalog.LINGER_GROUP, recipe(server, linger).group());
        assertTrue(result(recipe(server, splash)).is(Items.SPLASH_POTION));
        assertTrue(result(recipe(server, linger)).is(Items.LINGERING_POTION));
        assertFalse(server.getRecipeManager().byKey(incarnationLinger).isPresent());

        assertTrue(catalog.entries().size() < 800);
        assertFalse(server.getRecipeManager().byKey(perceptionItem).isPresent());
        StandBrewRecipe perceptionRecipe = recipe(server, perception);
        assertEquals("param_block_perception", perceptionRecipe.group());
        assertEquals(ModRecipes.STAND_PARAMETER.get(), perceptionRecipe.recipeBookCategory());
        assertEquals(StandSubjects.Kind.BLOCK, perceptionRecipe.subjectKind());
        ItemStack perceptionResult = result(perceptionRecipe);
        assertTrue(perceptionResult.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(com.buffspotions.registry.ModPotions.BLOCK_PERCEPTION));
        EssenceSubject perceptionSubject = perceptionResult.get(ModDataComponents.ESSENCE.get());
        assertTrue(perceptionSubject != null && perceptionSubject.item().isPresent());
        assertEquals(Items.STONE, perceptionSubject.item().get().value());
        assertFalse(server.getRecipeManager().byKey(key("stand/param/block_cracking")).isPresent());
        assertFalse(server.getRecipeManager().byKey(key("stand/param/block_stacking")).isPresent());
        assertTrue(recipe(server, cracking).ingredient().test(new ItemStack(Items.DIAMOND)));
        assertTrue(result(recipe(server, cracking)).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(com.buffspotions.registry.ModPotions.BLOCK_CRACKING));
        assertTrue(recipe(server, stacking).ingredient().test(new ItemStack(Items.BRICK)));
        assertTrue(result(recipe(server, stacking)).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(com.buffspotions.registry.ModPotions.BLOCK_STACKING));
        assertEquals(StandSubjects.Kind.NONE, recipe(server, cracking).subjectKind());
        assertEquals(StandSubjects.Kind.MOB, recipe(server, key("stand/param/incarnation")).subjectKind());
        StandBrewRecipe formation = recipe(server, key("stand/param/formation"));
        assertTrue(formation.ingredient().test(new ItemStack(Items.SCAFFOLDING)));
        assertEquals(ModRecipes.STAND_PARAMETER.get(), formation.recipeBookCategory());
        assertEquals(StandSubjects.Kind.BLOCK, formation.subjectKind());
        assertTrue(result(formation).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(com.buffspotions.registry.ModPotions.FORMATION));
        assertFalse(server.getRecipeManager().byKey(splashPerception).isPresent());
        assertFalse(server.getRecipeManager().byKey(lingerPerception).isPresent());

        Set<net.minecraft.world.item.Item> none = Set.of();
        Set<net.minecraft.world.item.Item> carrot = Set.of(Items.GOLDEN_CARROT);
        Set<net.minecraft.world.item.Item> breath = Set.of(Items.DRAGON_BREATH);
        assertTrue(catalog.shouldUnlock(entry(catalog, night), carrot, none, none));
        assertFalse(catalog.shouldUnlock(entry(catalog, night), none, none, none));
        assertFalse(catalog.shouldUnlock(entry(catalog, linger), carrot, none, carrot));
        assertTrue(catalog.shouldUnlock(entry(catalog, linger), carrot, none, breath));
        assertTrue(catalog.shouldUnlock(entry(catalog, perception), Set.of(Items.RESIN_CLUMP), none, none));
        assertTrue(catalog.shouldUnlock(entry(catalog, key("stand/param/formation")), Set.of(Items.SCAFFOLDING), none, none));
    }

    private static boolean strongSwiftnessInversion(MinecraftServer server, StandRecipeCatalog catalog) {
        ItemStack strong = PotionContents.createItemStack(Items.POTION, Potions.STRONG_SWIFTNESS);
        ItemStack eye = new ItemStack(Items.FERMENTED_SPIDER_EYE);
        for (StandRecipeCatalog.Entry entry : catalog.entries()) {
            StandBrewRecipe recipe = recipe(server, entry.id());
            if (recipe.bottle().test(strong) && recipe.ingredient().test(eye)) {
                return true;
            }
        }
        return false;
    }

    private static StandBrewRecipe recipe(MinecraftServer server, ResourceKey<Recipe<?>> id) {
        return (StandBrewRecipe) server.getRecipeManager().byKey(id).orElseThrow().value();
    }

    private static ItemStack result(StandBrewRecipe recipe) {
        return recipe.result().create();
    }

    private static StandRecipeCatalog.Entry entry(StandRecipeCatalog catalog, ResourceKey<Recipe<?>> id) {
        return catalog.entries().stream().filter(entry -> entry.id().equals(id)).findFirst().orElseThrow();
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("buffs_potions", path));
    }
}
