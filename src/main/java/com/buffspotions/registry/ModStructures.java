package com.buffspotions.registry;

import com.buffspotions.BuffsPotions;
import com.buffspotions.structure.CovenPiece;
import com.buffspotions.structure.CovenStructure;
import com.buffspotions.structure.EvilApothecaryStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    private static final DeferredRegister<StructureType<?>> STRUCTURES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, BuffsPotions.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, BuffsPotions.MOD_ID);

    public static final DeferredHolder<StructureType<?>, StructureType<EvilApothecaryStructure>> EVIL_APOTHECARY =
            STRUCTURES.register("evil_apothecary", () -> () -> EvilApothecaryStructure.CODEC);
    public static final DeferredHolder<StructureType<?>, StructureType<CovenStructure>> COVEN =
            STRUCTURES.register("coven", () -> () -> CovenStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> COVEN_PIECE =
            PIECES.register("coven", () -> CovenPiece::new);

    private ModStructures() {
    }

    public static void register(IEventBus modBus) {
        STRUCTURES.register(modBus);
        PIECES.register(modBus);
    }
}
