package com.buffspotions.effect;

import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectCategory;

public final class ParameterizedEffect extends BuffMobEffect {
    private final Kind kind;

    public ParameterizedEffect(MobEffectCategory category, int color, Kind kind) {
        super(category, color);
        this.kind = kind;
    }

    @Override
    public Component getDisplayName() {
        Component named = supplier().get();
        return named != null ? named : super.getDisplayName();
    }

    private Supplier<Component> supplier() {
        return switch (this.kind) {
            case SENSE -> EyedDisplay.SENSE;
            case EATER -> EyedDisplay.EATER;
            case VOMITING -> EyedDisplay.VOMITING;
            case SEEKING -> EyedDisplay.SEEKING;
            case DISGUISE -> EyedDisplay.DISGUISE;
        };
    }

    public enum Kind {
        SENSE,
        EATER,
        VOMITING,
        SEEKING,
        DISGUISE
    }
}
