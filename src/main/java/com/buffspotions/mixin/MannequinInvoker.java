package com.buffspotions.mixin;

import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Mannequin.class)
public interface MannequinInvoker {
    @Invoker("setProfile")
    void buffsPotions$setProfile(ResolvableProfile profile);

    @Invoker("setImmovable")
    void buffsPotions$setImmovable(boolean immovable);

    @Invoker("setHideDescription")
    void buffsPotions$setHideDescription(boolean hideDescription);
}
