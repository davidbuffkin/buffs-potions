package com.buffspotions.stack;

import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModItems;
import com.buffspotions.registry.ModPotions;
import com.buffspotions.registry.ModRecipes;
import com.buffspotions.stand.StandBrewRecipe;
import com.buffspotions.stand.StandRecipeDisplay;
import com.buffspotions.stand.StandSubjects;
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
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class StackRecipeBookTest {
    @Test
    void stackBookUsesFlagonsAndSecondGrades(MinecraftServer server) {
        StackRecipeCatalog catalog = StackRecipeCatalog.get();
        ResourceKey<Recipe<?>> night = key("stack/base/minecraft/golden_carrot");
        ResourceKey<Recipe<?>> slowness = key("stack/invert/minecraft/sugar");
        ResourceKey<Recipe<?>> awkward = key("stack/awkward");
        ResourceKey<Recipe<?>> thicken = key("stack/thicken/minecraft/sugar");
        ResourceKey<Recipe<?>> extend = key("stack/extend/minecraft/sugar");
        ResourceKey<Recipe<?>> torchflower = key("stack/torchflower/minecraft/sugar");
        ResourceKey<Recipe<?>> pitcher = key("stack/pitcher/minecraft/sugar");
        ResourceKey<Recipe<?>> clarifyTorch = key("stack/clarify/torchflower/minecraft/sugar");
        ResourceKey<Recipe<?>> clarifyPitch = key("stack/clarify/pitcher/minecraft/sugar");
        ResourceKey<Recipe<?>> nightTorch = key("stack/torchflower/minecraft/golden_carrot");
        ResourceKey<Recipe<?>> splash = key("stack/splash/minecraft/sugar");
        ResourceKey<Recipe<?>> linger = key("stack/linger/minecraft/sugar");
        ResourceKey<Recipe<?>> perception = key("stack/param/block_perception");
        ResourceKey<Recipe<?>> perceptionItem = key("stack/param/block_perception/minecraft/stone");
        ResourceKey<Recipe<?>> disguise = key("stack/param/disguise");

        StandBrewRecipe nightRecipe = recipe(server, night);
        assertTrue(StackFlagons.sameFluid(
                bottle(nightRecipe),
                StackFlagons.filled(PotionContents.createItemStack(Items.POTION, Potions.AWKWARD))
        ));
        assertTrue(nightRecipe.ingredient().test(new ItemStack(Items.GOLDEN_CARROT)));
        assertTrue(dose(result(nightRecipe)).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.NIGHT_VISION));
        assertTrue(result(nightRecipe).is(ModItems.FLAGON.get()));
        assertEquals(FlagonContents.MAX_DOSES, FlagonContents.of(result(nightRecipe)).doses());
        assertEquals(ModRecipes.STACK_HELPFUL.get(), nightRecipe.recipeBookCategory());
        assertEquals(ModRecipes.STACK_HARMFUL.get(), recipe(server, slowness).recipeBookCategory());
        assertTrue(dose(result(recipe(server, slowness))).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.SLOWNESS));

        assertEquals(ModRecipes.STACK_UTILITY.get(), recipe(server, awkward).recipeBookCategory());
        assertTrue(dose(result(recipe(server, awkward))).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.AWKWARD));
        StandBrewRecipe plainMundane = recipe(server, key("stack/mundane/redstone"));
        assertEquals(ModRecipes.STACK_UTILITY.get(), plainMundane.recipeBookCategory());
        assertTrue(plainMundane.ingredient().test(new ItemStack(Items.REDSTONE)));
        assertTrue(StackFlagons.sameFluid(
                bottle(plainMundane),
                StackFlagons.filled(PotionContents.createItemStack(Items.POTION, Potions.WATER))
        ));
        assertTrue(dose(result(plainMundane)).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.MUNDANE));
        assertFalse(dose(result(plainMundane)).getOrDefault(ModDataComponents.MUNDANE.get(), false));
        assertFalse(server.getRecipeManager().byKey(splash).isPresent());
        assertFalse(server.getRecipeManager().byKey(linger).isPresent());
        assertFalse(server.getRecipeManager().byKey(nightTorch).isPresent());

        assertEquals(StackRecipeCatalog.THICKEN_GROUP, recipe(server, thicken).group());
        assertEquals(StackRecipeCatalog.EXTEND_GROUP, recipe(server, extend).group());
        assertEquals(StackRecipeCatalog.TORCHFLOWER_GROUP, recipe(server, torchflower).group());
        assertEquals(StackRecipeCatalog.PITCHER_GROUP, recipe(server, pitcher).group());
        assertEquals(StackRecipeCatalog.CLARIFY_GROUP, recipe(server, clarifyTorch).group());
        assertEquals(StackRecipeCatalog.CLARIFY_GROUP, recipe(server, clarifyPitch).group());
        assertTrue(dose(result(recipe(server, thicken))).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.STRONG_SWIFTNESS));
        assertTrue(StackFlagons.sameFluid(
                bottle(recipe(server, torchflower)),
                StackFlagons.filled(PotionContents.createItemStack(Items.POTION, Potions.STRONG_SWIFTNESS))
        ));
        assertTrue(recipe(server, torchflower).ingredient().test(new ItemStack(Items.TORCHFLOWER)));
        assertTrue(StackFlagons.sameFluid(
                bottle(recipe(server, pitcher)),
                StackFlagons.filled(PotionContents.createItemStack(Items.POTION, Potions.LONG_SWIFTNESS))
        ));
        assertTrue(recipe(server, pitcher).ingredient().test(new ItemStack(Items.PITCHER_PLANT)));
        assertTrue(recipe(server, clarifyTorch).ingredient().test(new ItemStack(Items.TORCHFLOWER)));
        assertTrue(recipe(server, clarifyPitch).ingredient().test(new ItemStack(Items.PITCHER_PLANT)));
        assertEquals("swiftness", StandSubjects.potionCycleKey(result(recipe(server, thicken))));
        assertEquals("swiftness", StandSubjects.potionCycleKey(result(recipe(server, extend))));
        assertEquals("swiftness", StandSubjects.potionCycleKey(result(recipe(server, torchflower))));
        assertEquals("swiftness", StandSubjects.potionCycleKey(result(recipe(server, pitcher))));
        assertEquals("swiftness", StandSubjects.potionCycleKey(result(recipe(server, clarifyTorch))));
        assertEquals("swiftness", StandSubjects.potionCycleKey(result(recipe(server, clarifyPitch))));

        assertTrue(catalog.entries().size() < 2000);
        assertFalse(server.getRecipeManager().byKey(perceptionItem).isPresent());
        StandBrewRecipe perceptionRecipe = recipe(server, perception);
        assertEquals(ModRecipes.STACK_PARAMETER.get(), perceptionRecipe.recipeBookCategory());
        assertEquals(StandSubjects.Kind.BLOCK, perceptionRecipe.subjectKind());
        ItemStack perceptionDose = dose(result(perceptionRecipe));
        assertTrue(perceptionDose.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.BLOCK_PERCEPTION));
        EssenceSubject subject = perceptionDose.get(ModDataComponents.ESSENCE.get());
        assertTrue(subject != null && subject.item().isPresent());
        assertEquals(Items.STONE, subject.item().get().value());
        assertTrue(recipe(server, disguise).anyPlayerEssence());
        StandBrewRecipe formation = recipe(server, key("stack/param/formation"));
        assertTrue(formation.ingredient().test(new ItemStack(Items.SCAFFOLDING)));
        assertEquals(ModRecipes.STACK_PARAMETER.get(), formation.recipeBookCategory());
        assertEquals(StandSubjects.Kind.BLOCK, formation.subjectKind());
        assertTrue(dose(result(formation)).getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.FORMATION));

        Set<net.minecraft.world.item.Item> none = Set.of();
        Set<net.minecraft.world.item.Item> sugar = Set.of(Items.SUGAR);
        assertTrue(catalog.shouldUnlock(entry(catalog, torchflower), sugar, none, none));
        assertFalse(catalog.shouldUnlock(entry(catalog, torchflower), none, none, none));
        assertTrue(catalog.shouldUnlock(entry(catalog, clarifyPitch), sugar, none, none));
        assertTrue(catalog.shouldUnlock(entry(catalog, key("stack/param/formation")), Set.of(Items.SCAFFOLDING), none, none));
    }

    private static StandBrewRecipe recipe(MinecraftServer server, ResourceKey<Recipe<?>> id) {
        return (StandBrewRecipe) server.getRecipeManager().byKey(id).orElseThrow().value();
    }

    private static ItemStack result(StandBrewRecipe recipe) {
        return recipe.result().create();
    }

    private static ItemStack bottle(StandBrewRecipe recipe) {
        return shown(((StandRecipeDisplay) recipe.display().getFirst()).bottle());
    }

    private static ItemStack shown(SlotDisplay display) {
        if (display instanceof SlotDisplay.ItemStackSlotDisplay stack) {
            return stack.stack().create();
        }
        if (display instanceof SlotDisplay.Composite composite) {
            return shown(composite.contents().getFirst());
        }
        if (display instanceof SlotDisplay.WithRemainder remainder) {
            return shown(remainder.input());
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack dose(ItemStack flagon) {
        return FlagonContents.of(flagon).dose();
    }

    private static StackRecipeCatalog.Entry entry(StackRecipeCatalog catalog, ResourceKey<Recipe<?>> id) {
        return catalog.entries().stream().filter(entry -> entry.id().equals(id)).findFirst().orElseThrow();
    }

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath("buffs_potions", path));
    }
}
