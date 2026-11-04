package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.crab.Crab;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    private static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, BuffsPotions.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<Crab>> CRAB = ENTITIES.register(
            "crab",
            () -> EntityType.Builder.of(Crab::new, MobCategory.CREATURE)
                    .sized(0.9F, 0.45F)
                    .eyeHeight(0.28F)
                    .clientTrackingRange(8)
                    .build(ResourceKey.create(
                            Registries.ENTITY_TYPE,
                            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "crab")
                    ))
    );

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        bus.addListener(ModEntities::attributes);
        bus.addListener(ModEntities::spawnPlacements);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(CRAB.get(), Crab.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(
                CRAB.get(),
                SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Crab::checkSpawn,
                RegisterSpawnPlacementsEvent.Operation.REPLACE
        );
    }

    private ModEntities() {
    }
}
