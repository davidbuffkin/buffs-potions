package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.structure.ApothecaryProcessor;
import com.buffspotions.structure.CovenProcessor;
import com.buffspotions.structure.EvilApothecaryProcessor;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructureProcessors {
    private static final DeferredRegister<MapCodec<? extends StructureProcessor>> PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, BuffsPotions.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends StructureProcessor>, MapCodec<ApothecaryProcessor>> APOTHECARY =
            PROCESSORS.register("apothecary", () -> ApothecaryProcessor.CODEC);
    public static final DeferredHolder<MapCodec<? extends StructureProcessor>, MapCodec<EvilApothecaryProcessor>> EVIL_APOTHECARY =
            PROCESSORS.register("evil_apothecary", () -> EvilApothecaryProcessor.CODEC);
    public static final DeferredHolder<MapCodec<? extends StructureProcessor>, MapCodec<CovenProcessor>> COVEN =
            PROCESSORS.register("coven", () -> CovenProcessor.CODEC);

    private ModStructureProcessors() {
    }

    public static void register(IEventBus modBus) {
        PROCESSORS.register(modBus);
    }
}
