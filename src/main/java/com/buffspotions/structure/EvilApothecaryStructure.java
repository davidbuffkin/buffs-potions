package com.buffspotions.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

/**
 * Places the evil apothecary on the nether floor. The world-surface heightmap is the
 * bedrock ceiling, so the column is scanned for the highest open ground instead.
 */
public final class EvilApothecaryStructure extends Structure {
    public static final MapCodec<EvilApothecaryStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(structure -> structure.startPool),
            Codec.intRange(0, 20).fieldOf("size").forGetter(structure -> structure.maxDepth),
            Codec.BOOL.fieldOf("use_expansion_hack").forGetter(structure -> structure.useExpansionHack),
            JigsawStructure.MaxDistance.CODEC.fieldOf("max_distance_from_center").forGetter(structure -> structure.maxDistanceFromCenter)
    ).apply(instance, EvilApothecaryStructure::new));
    /** Below the bedrock roof, and above the bedrock floor. */
    private static final int SCAN_TOP = 120;
    private static final int SCAN_BOTTOM = 5;
    private static final int[][] COLUMN_OFFSETS = {{7, 7}, {2, 2}, {12, 2}, {2, 12}, {12, 12}};

    private final Holder<StructureTemplatePool> startPool;
    private final int maxDepth;
    private final boolean useExpansionHack;
    private final JigsawStructure.MaxDistance maxDistanceFromCenter;

    public EvilApothecaryStructure(
            Structure.StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            int maxDepth,
            boolean useExpansionHack,
            JigsawStructure.MaxDistance maxDistanceFromCenter
    ) {
        super(settings);
        this.startPool = startPool;
        this.maxDepth = maxDepth;
        this.useExpansionHack = useExpansionHack;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        int originX = context.chunkPos().getMinBlockX();
        int originZ = context.chunkPos().getMinBlockZ();
        int floor = -1;
        for (int[] offset : COLUMN_OFFSETS) {
            NoiseColumn column = context.chunkGenerator().getBaseColumn(
                    originX + offset[0],
                    originZ + offset[1],
                    context.heightAccessor(),
                    context.randomState()
            );
            floor = floorY(column);
            if (floor >= 0) {
                break;
            }
        }
        if (floor < 0) {
            return Optional.empty();
        }
        BlockPos start = new BlockPos(originX, floor, originZ);
        return JigsawPlacement.addPieces(
                context,
                this.startPool,
                Optional.<Identifier>empty(),
                this.maxDepth,
                start,
                this.useExpansionHack,
                Optional.<Heightmap.Types>empty(),
                this.maxDistanceFromCenter,
                PoolAliasLookup.create(List.of(), start, context.seed()),
                JigsawStructure.DEFAULT_DIMENSION_PADDING,
                LiquidSettings.APPLY_WATERLOGGING
        );
    }

    /**
     * The y of the highest nether ground with open space above it.
     * A lava lake hides that ground, and the column is skipped.
     */
    public static int floorY(NoiseColumn column) {
        boolean lava = false;
        for (int y = SCAN_TOP; y >= SCAN_BOTTOM; y--) {
            BlockState here = column.getBlock(y);
            if (here.is(Blocks.LAVA)) {
                lava = true;
                continue;
            }
            if (lava) {
                return -1;
            }
            if (ground(here) && open(column.getBlock(y + 1))) {
                return y;
            }
        }
        return -1;
    }

    private static boolean ground(BlockState state) {
        return state.is(Blocks.NETHERRACK)
                || state.is(Blocks.CRIMSON_NYLIUM)
                || state.is(Blocks.WARPED_NYLIUM)
                || state.is(Blocks.SOUL_SAND)
                || state.is(Blocks.SOUL_SOIL)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.BASALT)
                || state.is(Blocks.SMOOTH_BASALT)
                || state.is(Blocks.MAGMA_BLOCK);
    }

    private static boolean open(BlockState state) {
        return !state.is(Blocks.LAVA) && (state.isAir() || state.canBeReplaced());
    }

    @Override
    public StructureType<?> type() {
        return com.buffspotions.registry.ModStructures.EVIL_APOTHECARY.get();
    }
}
