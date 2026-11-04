package com.buffspotions.mixin.client;

import java.util.Set;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeCollection.class)
public interface RecipeCollectionAccess {
    @Accessor("craftable")
    Set<RecipeDisplayId> buffsPotions$craftable();

    @Accessor("selected")
    Set<RecipeDisplayId> buffsPotions$selected();
}
