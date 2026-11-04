package com.buffspotions.stand;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

public record StandRecipeDisplay(
        SlotDisplay bottle,
        SlotDisplay ingredient,
        SlotDisplay result,
        SlotDisplay craftingStation,
        boolean anyPlayerEssence,
        int subjectKind
) implements RecipeDisplay {
    public static final MapCodec<StandRecipeDisplay> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            SlotDisplay.CODEC.fieldOf("bottle").forGetter(StandRecipeDisplay::bottle),
            SlotDisplay.CODEC.fieldOf("ingredient").forGetter(StandRecipeDisplay::ingredient),
            SlotDisplay.CODEC.fieldOf("result").forGetter(StandRecipeDisplay::result),
            SlotDisplay.CODEC.fieldOf("crafting_station").forGetter(StandRecipeDisplay::craftingStation),
            com.mojang.serialization.Codec.BOOL.optionalFieldOf("any_player_essence", false).forGetter(StandRecipeDisplay::anyPlayerEssence),
            com.mojang.serialization.Codec.INT.optionalFieldOf("subject_kind", 0).forGetter(StandRecipeDisplay::subjectKind)
    ).apply(instance, StandRecipeDisplay::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, StandRecipeDisplay> STREAM_CODEC = StreamCodec.composite(
            SlotDisplay.STREAM_CODEC,
            StandRecipeDisplay::bottle,
            SlotDisplay.STREAM_CODEC,
            StandRecipeDisplay::ingredient,
            SlotDisplay.STREAM_CODEC,
            StandRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC,
            StandRecipeDisplay::craftingStation,
            ByteBufCodecs.BOOL,
            StandRecipeDisplay::anyPlayerEssence,
            ByteBufCodecs.VAR_INT,
            StandRecipeDisplay::subjectKind,
            StandRecipeDisplay::new
    );

    public StandSubjects.Kind subjects() {
        return StandSubjects.Kind.byOrdinal(subjectKind);
    }
    public static final RecipeDisplay.Type<StandRecipeDisplay> TYPE = new RecipeDisplay.Type<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public RecipeDisplay.Type<StandRecipeDisplay> type() {
        return TYPE;
    }

    @Override
    public boolean isEnabled(FeatureFlagSet enabledFeatures) {
        return this.bottle.isEnabled(enabledFeatures)
                && this.ingredient.isEnabled(enabledFeatures)
                && RecipeDisplay.super.isEnabled(enabledFeatures);
    }
}
