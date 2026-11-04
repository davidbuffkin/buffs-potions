package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.flagon.AnchoredEnderFlagonBlockEntity;
import com.buffspotions.flagon.EnderFlagonBlockEntity;
import com.buffspotions.flagon.FlagonBlockEntity;
import com.buffspotions.shrieker.CalibratedSculkShriekerBlockEntity;
import com.buffspotions.stack.BrewingStackBlockEntity;
import com.buffspotions.still.BrewingStillBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BuffsPotions.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FlagonBlockEntity>> FLAGON =
            BLOCK_ENTITIES.register("flagon", () -> new BlockEntityType<>(FlagonBlockEntity::new, ModBlocks.FLAGON.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnderFlagonBlockEntity>> ENDER_FLAGON =
            BLOCK_ENTITIES.register(
                    "ender_flagon",
                    () -> new BlockEntityType<>(EnderFlagonBlockEntity::new, ModBlocks.ENDER_FLAGON.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AnchoredEnderFlagonBlockEntity>> ANCHORED_ENDER_FLAGON =
            BLOCK_ENTITIES.register(
                    "anchored_ender_flagon",
                    () -> new BlockEntityType<>(AnchoredEnderFlagonBlockEntity::new, ModBlocks.ANCHORED_ENDER_FLAGON.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrewingStillBlockEntity>> BREWING_STILL =
            BLOCK_ENTITIES.register(
                    "brewing_still",
                    () -> new BlockEntityType<>(BrewingStillBlockEntity::new, ModBlocks.BREWING_STILL.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CalibratedSculkShriekerBlockEntity>> CALIBRATED_SCULK_SHRIEKER =
            BLOCK_ENTITIES.register(
                    "calibrated_sculk_shrieker",
                    () -> new BlockEntityType<>(CalibratedSculkShriekerBlockEntity::new, ModBlocks.CALIBRATED_SCULK_SHRIEKER.get())
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrewingStackBlockEntity>> BREWING_STACK =
            BLOCK_ENTITIES.register(
                    "brewing_stack",
                    () -> new BlockEntityType<>(BrewingStackBlockEntity::new, ModBlocks.BREWING_STACK.get())
            );

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }

    private ModBlockEntities() {
    }
}
