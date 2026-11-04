package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, BuffsPotions.MOD_ID);

    /** End-rod glitter that falls and fades after about a second. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FALLING_ROD =
            PARTICLES.register("falling_rod", () -> new SimpleParticleType(false));

    public static void register(IEventBus bus) {
        PARTICLES.register(bus);
    }

    private ModParticles() {
    }
}
