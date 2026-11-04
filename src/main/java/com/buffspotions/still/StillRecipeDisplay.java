package com.buffspotions.still;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record StillRecipeDisplay(
        SlotDisplay bottle,
        SlotDisplay ingredient,
        SlotDisplay fuel,
        SlotDisplay result,
        SlotDisplay craftingStation
) implements RecipeDisplay {
    public static final MapCodec<StillRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SlotDisplay.CODEC.fieldOf("bottle").forGetter(StillRecipeDisplay::bottle),
            SlotDisplay.CODEC.fieldOf("ingredient").forGetter(StillRecipeDisplay::ingredient),
            SlotDisplay.CODEC.fieldOf("fuel").forGetter(StillRecipeDisplay::fuel),
            SlotDisplay.CODEC.fieldOf("result").forGetter(StillRecipeDisplay::result),
            SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(StillRecipeDisplay::craftingStation)
    ).apply(instance, StillRecipeDisplay::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, StillRecipeDisplay> STREAM_CODEC = StreamCodec.composite(
            SlotDisplay.STREAM_CODEC,
            StillRecipeDisplay::bottle,
            SlotDisplay.STREAM_CODEC,
            StillRecipeDisplay::ingredient,
            SlotDisplay.STREAM_CODEC,
            StillRecipeDisplay::fuel,
            SlotDisplay.STREAM_CODEC,
            StillRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC,
            StillRecipeDisplay::craftingStation,
            StillRecipeDisplay::new
    );
    public static final RecipeDisplay.Type<StillRecipeDisplay> TYPE = new RecipeDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public RecipeDisplay.Type<StillRecipeDisplay> type() {
        return TYPE;
    }

    @Override
    public boolean isEnabled(FeatureFlagSet enabledFeatures) {
        return this.bottle.isEnabled(enabledFeatures)
                && this.ingredient.isEnabled(enabledFeatures)
                && this.fuel.isEnabled(enabledFeatures)
                && RecipeDisplay.super.isEnabled(enabledFeatures);
    }
}
