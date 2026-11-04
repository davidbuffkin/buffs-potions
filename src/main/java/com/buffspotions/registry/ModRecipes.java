package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.stand.StandBrewRecipe;
import com.buffspotions.stand.StandRecipeDisplay;
import com.buffspotions.still.StillDistillRecipe;
import com.buffspotions.still.StillRecipeDisplay;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.RecipeBookType;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final RecipeBookType BREWING_STILL = RecipeBookType.valueOf("BUFFS_POTIONS_BREWING_STILL");
    public static final RecipeBookType BREWING_STAND_BOOK = RecipeBookType.valueOf("BUFFS_POTIONS_BREWING_STAND");
    public static final RecipeBookType BREWING_STACK_BOOK = RecipeBookType.valueOf("BUFFS_POTIONS_BREWING_STACK");

    private static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, BuffsPotions.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, BuffsPotions.MOD_ID);
    private static final DeferredRegister<RecipeBookCategory> CATEGORIES =
            DeferredRegister.create(Registries.RECIPE_BOOK_CATEGORY, BuffsPotions.MOD_ID);
    private static final DeferredRegister<RecipeDisplay.Type<?>> DISPLAYS =
            DeferredRegister.create(Registries.RECIPE_DISPLAY, BuffsPotions.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<StillDistillRecipe>> STILL_TYPE =
            TYPES.register("still_distilling", () -> RecipeType.simple(
                    Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "still_distilling")
            ));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StillDistillRecipe>> STILL_SERIALIZER =
            SERIALIZERS.register("still_distilling", () -> new RecipeSerializer<>(
                    StillDistillRecipe.CODEC,
                    StillDistillRecipe.STREAM_CODEC
            ));
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> MUNDANE =
            CATEGORIES.register("still_mundane", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> HARMFUL =
            CATEGORIES.register("still_mundane_harmful", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> OTHER =
            CATEGORIES.register("still_other", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> STAND_HELPFUL =
            CATEGORIES.register("stand_helpful", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> STAND_HARMFUL =
            CATEGORIES.register("stand_harmful", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> STAND_PARAMETER =
            CATEGORIES.register("stand_parameter", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> STAND_UTILITY =
            CATEGORIES.register("stand_utility", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> STACK_HELPFUL =
            CATEGORIES.register("stack_helpful", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> STACK_HARMFUL =
            CATEGORIES.register("stack_harmful", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> STACK_PARAMETER =
            CATEGORIES.register("stack_parameter", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeBookCategory, RecipeBookCategory> STACK_UTILITY =
            CATEGORIES.register("stack_utility", RecipeBookCategory::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<StandBrewRecipe>> STAND_TYPE =
            TYPES.register("stand_brewing", () -> RecipeType.simple(
                    Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "stand_brewing")
            ));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StandBrewRecipe>> STAND_SERIALIZER =
            SERIALIZERS.register("stand_brewing", () -> new RecipeSerializer<>(
                    StandBrewRecipe.CODEC,
                    StandBrewRecipe.STREAM_CODEC
            ));
    public static final DeferredHolder<RecipeDisplay.Type<?>, RecipeDisplay.Type<StillRecipeDisplay>> STILL_DISPLAY =
            DISPLAYS.register("still", () -> StillRecipeDisplay.TYPE);
    public static final DeferredHolder<RecipeDisplay.Type<?>, RecipeDisplay.Type<StandRecipeDisplay>> STAND_DISPLAY =
            DISPLAYS.register("stand", () -> StandRecipeDisplay.TYPE);

    public static void register(IEventBus bus) {
        TYPES.register(bus);
        SERIALIZERS.register(bus);
        CATEGORIES.register(bus);
        DISPLAYS.register(bus);
    }

    private ModRecipes() {
    }
}
