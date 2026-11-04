package com.buffspotions.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EditBox.class)
public interface EditBoxAccess {
    @Accessor("value")
    String buffsPotions$value();

    @Accessor("value")
    void buffsPotions$value(String value);

    @Accessor("cursorPos")
    int buffsPotions$cursorPos();

    @Accessor("highlightPos")
    int buffsPotions$highlightPos();

    @Accessor("maxLength")
    int buffsPotions$maxLength();

    @Invoker("onValueChange")
    void buffsPotions$onValueChange(String value);
}
