package com.buffspotions.still;

import com.buffspotions.registry.ModRecipes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.Level;

/** A recipe-book entry for the brewing still. The still itself still brews through {@link StillBrewing}. */
public final class StillDistillRecipe implements Recipe<RecipeInput> {
    public static final MapCodec<StillDistillRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("bottle").forGetter(StillDistillRecipe::bottle),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(StillDistillRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(StillDistillRecipe::result),
            BuiltInRegistries.RECIPE_BOOK_CATEGORY.byNameCodec().fieldOf("category").forGetter(StillDistillRecipe::recipeBookCategory),
            Codec.STRING.optionalFieldOf("group", "").forGetter(StillDistillRecipe::group),
            Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(StillDistillRecipe::showNotification)
    ).apply(instance, StillDistillRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, StillDistillRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            StillDistillRecipe::bottle,
            Ingredient.CONTENTS_STREAM_CODEC,
            StillDistillRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC,
            StillDistillRecipe::result,
            ByteBufCodecs.registry(Registries.RECIPE_BOOK_CATEGORY),
            StillDistillRecipe::recipeBookCategory,
            ByteBufCodecs.STRING_UTF8,
            StillDistillRecipe::group,
            ByteBufCodecs.BOOL,
            StillDistillRecipe::showNotification,
            StillDistillRecipe::new
    );

    private final Ingredient bottle;
    private final Ingredient ingredient;
    private final ItemStackTemplate result;
    private final RecipeBookCategory category;
    private final String group;
    private final boolean showNotification;
    private final PlacementInfo placement;

    public StillDistillRecipe(
            Ingredient bottle,
            Ingredient ingredient,
            ItemStackTemplate result,
            RecipeBookCategory category,
            String group,
            boolean showNotification
    ) {
        this.bottle = bottle;
        this.ingredient = ingredient;
        this.result = result;
        this.category = category;
        this.group = group;
        this.showNotification = showNotification;
        this.placement = PlacementInfo.create(List.of(bottle, ingredient));
    }

    public Ingredient bottle() {
        return bottle;
    }

    public Ingredient ingredient() {
        return ingredient;
    }

    public ItemStackTemplate result() {
        return result;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return result.create();
    }

    @Override
    public boolean showNotification() {
        return showNotification;
    }

    @Override
    public String group() {
        return group;
    }

    @Override
    public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return ModRecipes.STILL_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return ModRecipes.STILL_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return placement;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new StillRecipeDisplay(
                bottle.display(),
                ingredient.display(),
                new SlotDisplay.ItemSlotDisplay(Items.CHARCOAL),
                new SlotDisplay.ItemStackSlotDisplay(result),
                new SlotDisplay.ItemSlotDisplay(com.buffspotions.registry.ModBlocks.BREWING_STILL.get().asItem())
        ));
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return category;
    }
}
