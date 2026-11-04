package com.buffspotions.structure;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * An underground coven. The piece waits until caves exist, then keeps the door on a cave
 * and the rest of the building in stone when a spot like that is there.
 */
public final class CovenStructure extends Structure {
    public static final MapCodec<CovenStructure> CODEC = simpleCodec(CovenStructure::new);

    public CovenStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        ChunkPos chunk = context.chunkPos();
        int minY = context.heightAccessor().getMinY();
        int maxY = context.heightAccessor().getMaxY();
        BlockPos marker = new BlockPos(chunk.getMiddleBlockX(), minY + 40, chunk.getMiddleBlockZ());
        return Optional.of(new Structure.GenerationStub(
                marker,
                builder -> builder.addPiece(new CovenPiece(context.structureTemplateManager(), chunk, minY, maxY))
        ));
    }

    @Override
    public StructureType<?> type() {
        return com.buffspotions.registry.ModStructures.COVEN.get();
    }
}
