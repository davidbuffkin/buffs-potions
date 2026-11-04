package com.buffspotions.structure;

import com.buffspotions.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

/**
 * Fills the evil apothecary's shelves, flagons, and chest, leaves the ground layer's
 * empty space as nether terrain, and keeps the zombified cleric hostile and persistent.
 */
public final class EvilApothecaryProcessor implements StructureProcessor {
    public static final MapCodec<EvilApothecaryProcessor> CODEC = MapCodec.unit(new EvilApothecaryProcessor());

    public EvilApothecaryProcessor() {
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
        int templateY = originalBlockInfo.pos().getY();
        BlockState state = processedBlockInfo.state();
        if (templateY == 0 && state.isAir()) {
            return null;
        }
        if (!(level instanceof ServerLevelAccessor accessor)) {
            return processedBlockInfo;
        }
        RandomSource random = RandomSource.create(Mth.getSeed(processedBlockInfo.pos()));
        HolderLookup.Provider registries = accessor.registryAccess();
        if (state.getBlock() instanceof ShelfBlock) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    ApothecaryProcessor.shelfItems(
                            registries,
                            processedBlockInfo.nbt(),
                            random,
                            EvilApothecaryLoot.SHELF_SLOTS,
                            EvilApothecaryLoot::shelfSlot
                    )
            );
        }
        if (state.is(ModBlocks.FLAGON.get())) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    ApothecaryProcessor.flagonContents(registries, processedBlockInfo.nbt(), EvilApothecaryLoot.flagon(random))
            );
        }
        if (state.is(Blocks.CHEST)) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    ApothecaryProcessor.chestLoot(processedBlockInfo.nbt(), EvilApothecaryLoot.CHEST, random)
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
        CompoundTag nbt = entityInfo.nbt.copy();
        if (!"minecraft:zombie_villager".equals(nbt.getStringOr("id", ""))) {
            return entityInfo;
        }
        nbt.remove("NoAI");
        nbt.remove("Brain");
        nbt.remove("Offers");
        nbt.putBoolean("PersistenceRequired", true);
        nbt.putInt("Xp", 1);
        CompoundTag data = nbt.getCompound("VillagerData").orElseGet(CompoundTag::new);
        data.putString("type", data.getStringOr("type", "minecraft:plains"));
        data.putString("profession", "minecraft:cleric");
        data.putInt("level", 1);
        nbt.put("VillagerData", data);
        nbt.putBoolean("VillagerDataFinalized", true);
        mark(nbt);
        return new StructureTemplate.StructureEntityInfo(entityInfo.pos, entityInfo.blockPos, nbt);
    }

    @Override
    public MapCodec<EvilApothecaryProcessor> codec() {
        return CODEC;
    }

    private static void mark(CompoundTag nbt) {
        ListTag tags = nbt.getList("Tags").orElseGet(ListTag::new);
        for (Tag tag : tags) {
            if (tag.asString().orElse("").equals(EvilCleric.TAG)) {
                nbt.put("Tags", tags);
                return;
            }
        }
        tags.add(StringTag.valueOf(EvilCleric.TAG));
        nbt.put("Tags", tags);
    }
}
