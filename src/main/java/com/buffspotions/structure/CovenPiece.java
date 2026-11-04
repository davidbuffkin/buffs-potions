package com.buffspotions.structure;

import com.buffspotions.BuffsPotions;
import com.buffspotions.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

/** Places the coven once the carved terrain is available to test the door and the walls. */
public final class CovenPiece extends StructurePiece {
    private static final Identifier TEMPLATE = Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "coven");

    private final StructureTemplateManager templates;
    private final int chunkX;
    private final int chunkZ;
    private boolean done;

    public CovenPiece(StructureTemplateManager templates, ChunkPos chunk, int minY, int maxY) {
        super(ModStructures.COVEN_PIECE.get(), 0, searchBox(chunk, minY, maxY));
        this.templates = templates;
        this.chunkX = chunk.x();
        this.chunkZ = chunk.z();
    }

    public CovenPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.COVEN_PIECE.get(), tag);
        this.templates = context.structureTemplateManager();
        this.chunkX = tag.getIntOr("ChunkX", 0);
        this.chunkZ = tag.getIntOr("ChunkZ", 0);
        this.done = tag.getBooleanOr("Done", false);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("ChunkX", this.chunkX);
        tag.putInt("ChunkZ", this.chunkZ);
        tag.putBoolean("Done", this.done);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox chunkBB,
            ChunkPos chunkPos,
            BlockPos referencePos
    ) {
        if (this.done || chunkPos.x() != this.chunkX || chunkPos.z() != this.chunkZ) {
            return;
        }
        this.done = true;
        CovenFit.Placement spot = CovenFit.find(level, random, chunkPos);
        if (spot == null) {
            return;
        }
        StructureTemplate template = this.templates.getOrCreate(TEMPLATE);
        BlockPos place = template.getZeroPositionWithTransform(spot.corner(), Mirror.NONE, spot.rotation());
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setRotation(spot.rotation())
                .setBoundingBox(BoundingBox.infinite())
                .setKnownShape(true)
                .setIgnoreEntities(false)
                .setFinalizeEntities(true)
                .setLiquidSettings(LiquidSettings.APPLY_WATERLOGGING)
                .addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK)
                .addProcessor(new CovenProcessor());
        template.placeInWorld(level, place, place, settings, random, 18);
        this.boundingBox = template.getBoundingBox(settings, place);
    }

    private static BoundingBox searchBox(ChunkPos chunk, int minY, int maxY) {
        int x0 = chunk.getMinBlockX() - 16;
        int z0 = chunk.getMinBlockZ() - 16;
        return new BoundingBox(x0, minY, z0, x0 + 47, maxY, z0 + 47);
    }
}
