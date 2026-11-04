package com.buffspotions.stand;

import com.buffspotions.registry.ModItems;
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
import net.minecraft.world.item.Item;
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

/** A recipe-book entry for the brewing stand. The stand still brews through vanilla mixing. */
public final class StandBrewRecipe implements Recipe<RecipeInput> {
    public static final MapCodec<StandBrewRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.fieldOf("bottle").forGetter(StandBrewRecipe::bottle),
            Ingredient.CODEC.fieldOf("ingredient").forGetter(StandBrewRecipe::ingredient),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(StandBrewRecipe::result),
            BuiltInRegistries.RECIPE_BOOK_CATEGORY.byNameCodec().fieldOf("category").forGetter(StandBrewRecipe::recipeBookCategory),
            Codec.STRING.optionalFieldOf("group", "").forGetter(StandBrewRecipe::group),
            Codec.BOOL.optionalFieldOf("show_notification", true).forGetter(StandBrewRecipe::showNotification),
            Codec.BOOL.optionalFieldOf("any_player_essence", false).forGetter(StandBrewRecipe::anyPlayerEssence),
            Codec.INT.xmap(StandSubjects.Kind::byOrdinal, StandSubjects.Kind::ordinal)
                    .optionalFieldOf("subject_kind", StandSubjects.Kind.NONE)
                    .forGetter(StandBrewRecipe::subjectKind)
    ).apply(instance, StandBrewRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, StandBrewRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.CONTENTS_STREAM_CODEC,
            StandBrewRecipe::bottle,
            Ingredient.CONTENTS_STREAM_CODEC,
            StandBrewRecipe::ingredient,
            ItemStackTemplate.STREAM_CODEC,
            StandBrewRecipe::result,
            ByteBufCodecs.registry(Registries.RECIPE_BOOK_CATEGORY),
            StandBrewRecipe::recipeBookCategory,
            ByteBufCodecs.STRING_UTF8,
            StandBrewRecipe::group,
            ByteBufCodecs.BOOL,
            StandBrewRecipe::showNotification,
            ByteBufCodecs.BOOL,
            StandBrewRecipe::anyPlayerEssence,
            ByteBufCodecs.VAR_INT.map(StandSubjects.Kind::byOrdinal, StandSubjects.Kind::ordinal),
            StandBrewRecipe::subjectKind,
            StandBrewRecipe::new
    );

    private final Ingredient bottle;
    private final Ingredient ingredient;
    private final ItemStackTemplate result;
    private final RecipeBookCategory category;
    private final String group;
    private final boolean showNotification;
    private final boolean anyPlayerEssence;
    private final StandSubjects.Kind subjectKind;
    private final PlacementInfo placement;

    public StandBrewRecipe(
            Ingredient bottle,
            Ingredient ingredient,
            ItemStackTemplate result,
            RecipeBookCategory category,
            String group,
            boolean showNotification,
            boolean anyPlayerEssence,
            StandSubjects.Kind subjectKind
    ) {
        this.bottle = bottle;
        this.ingredient = ingredient;
        this.result = result;
        this.category = category;
        this.group = group;
        this.showNotification = showNotification;
        this.anyPlayerEssence = anyPlayerEssence;
        this.subjectKind = subjectKind == null ? StandSubjects.Kind.NONE : subjectKind;
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

    public boolean anyPlayerEssence() {
        return anyPlayerEssence;
    }

    public StandSubjects.Kind subjectKind() {
        return subjectKind;
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
        return ModRecipes.STAND_SERIALIZER.get();
    }

    @Override
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return ModRecipes.STAND_TYPE.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        return placement;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new StandRecipeDisplay(
                bottle.display(),
                ingredient.display(),
                new SlotDisplay.ItemStackSlotDisplay(result),
                new SlotDisplay.ItemSlotDisplay(stationItem()),
                anyPlayerEssence,
                subjectKind.ordinal()
        ));
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return category;
    }

    private Item stationItem() {
        net.minecraft.resources.Identifier id = BuiltInRegistries.RECIPE_BOOK_CATEGORY.getKey(this.category);
        if (id != null && id.getPath().startsWith("stack_")) {
            return ModItems.BREWING_STACK.get();
        }
        return Items.BREWING_STAND;
    }
}
