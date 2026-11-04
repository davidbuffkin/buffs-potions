package com.buffspotions.mixin;

import com.buffspotions.stack.StackRecipeCatalog;
import com.buffspotions.stack.StackRecipeUnlocks;
import com.buffspotions.stand.StandRecipeCatalog;
import com.buffspotions.stand.StandRecipeUnlocks;
import com.buffspotions.still.StillRecipeCatalog;
import com.buffspotions.still.StillRecipeUnlocks;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RecipeManager.class)
abstract class RecipeManagerMixin {
    @Shadow
    private RecipeMap recipes;

    @Inject(method = "finalizeRecipeLoading", at = @At("HEAD"))
    private void buffsPotions$addStillRecipes(FeatureFlagSet enabledFlags, CallbackInfo callback) {
        StillRecipeUnlocks.forget();
        StandRecipeUnlocks.forget();
        StackRecipeUnlocks.forget();
        this.recipes = StillRecipeCatalog.install(this.recipes, enabledFlags);
        this.recipes = StandRecipeCatalog.install(this.recipes, enabledFlags);
        this.recipes = StackRecipeCatalog.install(this.recipes, enabledFlags);
    }
}
