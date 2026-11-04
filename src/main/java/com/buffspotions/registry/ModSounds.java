package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    private static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(Registries.SOUND_EVENT, BuffsPotions.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> CRAB_AMBIENT = register("crab_ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAB_HURT = register("crab_hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAB_DEATH = register("crab_death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRAB_STEP = register("crab_step");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(
                Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, name)
        ));
    }

    public static void register(IEventBus bus) {
        SOUNDS.register(bus);
    }

    private ModSounds() {
    }
}
