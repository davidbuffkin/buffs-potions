package com.buffspotions.structure;

import com.buffspotions.BuffsPotions;
import com.buffspotions.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

/**
 * Fills the coven's flagon, flower pot, and trapped chest the way the building was
 * described, wakes the witches, hangs dripstone over the nether wart, adds a bat,
 * and lights the outside of the door.
 */
public final class CovenProcessor implements StructureProcessor {
    public static final MapCodec<CovenProcessor> CODEC = MapCodec.unit(new CovenProcessor());

    public CovenProcessor() {
    }

    @Override
    public StructureTemplate.@Nullable StructureBlockInfo process(
            LevelReader level,
            BlockPos targetPosition,
            BlockPos referencePos,
            StructureTemplate.StructureBlockInfo originalBlockInfo,
            StructureTemplate.StructureBlockInfo processedBlockInfo,
            StructurePlaceSettings settings,
            @Nullable StructureTemplate template
    ) {
        BlockPos local = originalBlockInfo.pos();
        if (dripstone(local)) {
            return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), hangingDripstone(), null);
        }
        if (doorTorch(local)) {
            return new StructureTemplate.StructureBlockInfo(processedBlockInfo.pos(), outsideTorch(), null);
        }
        BlockState state = processedBlockInfo.state();
        RandomSource random = RandomSource.create(Mth.getSeed(processedBlockInfo.pos()));
        if (state.is(Blocks.FLOWER_POT)) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    ApothecaryLoot.flowerPot(random),
                    null
            );
        }
        if (!(level instanceof ServerLevelAccessor accessor)) {
            return processedBlockInfo;
        }
        if (local.getX() == CovenLayout.BAT_X && local.getY() == CovenLayout.BAT_Y && local.getZ() == CovenLayout.BAT_Z) {
            spawnBat(accessor, processedBlockInfo.pos());
        }
        HolderLookup.Provider registries = accessor.registryAccess();
        if (state.is(ModBlocks.FLAGON.get())) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    ApothecaryProcessor.flagonContents(registries, processedBlockInfo.nbt(), ApothecaryLoot.flagon(random))
            );
        }
        if (state.is(Blocks.TRAPPED_CHEST)) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    chest(registries, processedBlockInfo.nbt(), random)
            );
        }
        return processedBlockInfo;
    }

    @Override
    public StructureTemplate.StructureEntityInfo processEntity(
            LevelReader world,
            BlockPos seedPos,
            StructureTemplate.StructureEntityInfo rawEntityInfo,
            StructureTemplate.StructureEntityInfo entityInfo,
            StructurePlaceSettings placementSettings,
            StructureTemplate template
    ) {
        BlockPos local = rawEntityInfo.blockPos;
        if (local.getX() < 0
                || local.getY() < 0
                || local.getZ() < 0
                || local.getX() >= CovenLayout.SIZE_X
                || local.getY() >= CovenLayout.SIZE_Y
                || local.getZ() >= CovenLayout.SIZE_Z) {
            return null;
        }
        CompoundTag nbt = entityInfo.nbt.copy();
        if (!"minecraft:witch".equals(nbt.getStringOr("id", ""))) {
            return entityInfo;
        }
        nbt.remove("NoAI");
        nbt.remove("Brain");
        nbt.putBoolean("PersistenceRequired", true);
        return new StructureTemplate.StructureEntityInfo(entityInfo.pos, entityInfo.blockPos, nbt);
    }

    @Override
    public MapCodec<CovenProcessor> codec() {
        return CODEC;
    }

    private static CompoundTag chest(HolderLookup.Provider registries, @Nullable CompoundTag original, RandomSource random) {
        CompoundTag nbt = original == null ? new CompoundTag() : original.copy();
        List<ItemStackWithSlot> slots = new ArrayList<>();
        boolean[] used = new boolean[27];
        for (ItemStack stack : CovenLoot.chest(random)) {
            int slot = random.nextInt(used.length);
            for (int step = 0; step < used.length && used[slot]; step++) {
                slot = (slot + 1) % used.length;
            }
            used[slot] = true;
            slots.add(new ItemStackWithSlot(slot, stack));
        }
        Tag encoded = ItemStackWithSlot.CODEC.listOf()
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), slots)
                .resultOrPartial(BuffsPotions.LOGGER::error)
                .orElse(null);
        if (encoded != null) {
            nbt.put("Items", encoded);
        }
        nbt.remove("LootTable");
        nbt.remove("LootTableSeed");
        return nbt;
    }

    private static void spawnBat(ServerLevelAccessor level, BlockPos pos) {
        Bat bat = EntityTypes.BAT.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (bat == null) {
            return;
        }
        bat.snapTo(pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0.0F, 0.0F);
        bat.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.STRUCTURE, null);
        bat.setPersistenceRequired();
        level.addFreshEntity(bat);
    }

    /** Upper porch air on the north face of the mud bricks that flank the outside door. */
    private static boolean doorTorch(BlockPos pos) {
        return pos.getY() == 14 && pos.getZ() == 0 && (pos.getX() == 5 || pos.getX() == 7);
    }

    private static BlockState outsideTorch() {
        return Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH);
    }

    private static boolean dripstone(BlockPos pos) {
        int[] cells = CovenLayout.DRIPSTONE;
        for (int i = 0; i < cells.length; i += 3) {
            if (pos.getX() == cells[i] && pos.getY() == cells[i + 1] && pos.getZ() == cells[i + 2]) {
                return true;
            }
        }
        return false;
    }

    private static BlockState hangingDripstone() {
        return Blocks.POINTED_DRIPSTONE.defaultBlockState()
                .setValue(SpeleothemBlock.TIP_DIRECTION, Direction.DOWN)
                .setValue(SpeleothemBlock.THICKNESS, SpeleothemThickness.TIP)
                .setValue(SpeleothemBlock.WATERLOGGED, false);
    }
}
