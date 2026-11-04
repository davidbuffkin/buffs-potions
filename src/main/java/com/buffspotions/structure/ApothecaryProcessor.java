package com.buffspotions.structure;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.BoundlessNight;
import com.buffspotions.effect.EssenceSubject;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModBlocks;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModPotions;
import com.buffspotions.still.BrewingStillBlock;
import com.buffspotions.still.BrewingStillBlockEntity;
import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

/**
 * Fills the apothecary's pots, shelves, flagons, chests, and beehive, leaves the ground
 * layer's empty space as natural terrain, and unfreezes the saved villager and copper golem.
 */
public final class ApothecaryProcessor implements StructureProcessor {
    public static final MapCodec<ApothecaryProcessor> CODEC = MapCodec.unit(new ApothecaryProcessor());

    public ApothecaryProcessor() {
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
        HolderLookup.Provider registries = accessor.registryAccess();
        if (state.getBlock() instanceof ShelfBlock) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    shelfItems(registries, processedBlockInfo.nbt(), random, 3, ApothecaryLoot::shelfSlot)
            );
        }
        if (state.is(ModBlocks.FLAGON.get())) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    flagonContents(registries, processedBlockInfo.nbt(), ApothecaryLoot.flagon(random))
            );
        }
        if (state.is(Blocks.CHEST)) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    chestLoot(processedBlockInfo.nbt(), templateY, random)
            );
        }
        if (state.getBlock() instanceof BrewingStillBlock) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state.setValue(BrewingStillBlock.HAS_BOTTLE, true),
                    stillBottle(registries, processedBlockInfo.nbt())
            );
        }
        if (state.is(Blocks.FURNACE)) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    furnaceSupplies(registries, processedBlockInfo.nbt())
            );
        }
        if (state.is(Blocks.BARREL)) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    barrelContents(registries, processedBlockInfo.nbt(), random)
            );
        }
        if (state.is(Blocks.BEEHIVE)) {
            return new StructureTemplate.StructureBlockInfo(
                    processedBlockInfo.pos(),
                    state,
                    beehiveBees(registries, processedBlockInfo.nbt())
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
        nbt.remove("NoAI");
        if ("minecraft:villager".equals(nbt.getStringOr("id", ""))) {
            nbt.remove("Brain");
            nbt.remove("Offers");
            nbt.putBoolean("PersistenceRequired", true);
            // One point of experience keeps a novice from resigning and taking the barrel as a fisherman.
            nbt.putInt("Xp", 1);
            CompoundTag data = nbt.getCompound("VillagerData").orElseGet(CompoundTag::new);
            data.putString("profession", "minecraft:cleric");
            data.putInt("level", 1);
            nbt.put("VillagerData", data);
        }
        return new StructureTemplate.StructureEntityInfo(entityInfo.pos, entityInfo.blockPos, nbt);
    }

    @Override
    public MapCodec<ApothecaryProcessor> codec() {
        return CODEC;
    }

    static CompoundTag shelfItems(
            HolderLookup.Provider registries,
            @Nullable CompoundTag original,
            RandomSource random,
            int slotCount,
            Function<RandomSource, ItemStack> roll
    ) {
        List<ItemStackWithSlot> slots = new ArrayList<>();
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack stack = roll.apply(random);
            if (!stack.isEmpty()) {
                slots.add(new ItemStackWithSlot(slot, stack));
            }
        }
        CompoundTag nbt = original == null ? new CompoundTag() : original.copy();
        Tag encoded = ItemStackWithSlot.CODEC.listOf()
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), slots)
                .resultOrPartial(BuffsPotions.LOGGER::error)
                .orElse(null);
        if (encoded != null) {
            nbt.put("Items", encoded);
        }
        return nbt;
    }

    static CompoundTag flagonContents(HolderLookup.Provider registries, @Nullable CompoundTag original, FlagonContents contents) {
        CompoundTag nbt = copy(original);
        FlagonContents.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), contents)
                .resultOrPartial(BuffsPotions.LOGGER::error)
                .ifPresent(encoded -> nbt.put("Contents", encoded));
        return nbt;
    }

    static CompoundTag chestLoot(@Nullable CompoundTag original, int templateY, RandomSource random) {
        return chestLoot(original, ApothecaryLoot.chest(templateY), random);
    }

    static CompoundTag chestLoot(@Nullable CompoundTag original, ResourceKey<LootTable> table, RandomSource random) {
        CompoundTag nbt = copy(original);
        long seed = random.nextLong();
        nbt.putString("LootTable", table.identifier().toString());
        nbt.putLong("LootTableSeed", seed == 0L ? 1L : seed);
        nbt.put("Items", new ListTag());
        return nbt;
    }

    static CompoundTag furnaceSupplies(HolderLookup.Provider registries, @Nullable CompoundTag original) {
        CompoundTag nbt = copy(original);
        List<ItemStackWithSlot> slots = readItems(registries, original);
        slots.removeIf(slot -> slot.slot() == 0 || slot.slot() == 1);
        slots.add(new ItemStackWithSlot(0, new ItemStack(Items.SPRUCE_LOG, 18)));
        slots.add(new ItemStackWithSlot(1, new ItemStack(Items.COAL, 2)));
        writeItems(registries, nbt, slots);
        return nbt;
    }

    static CompoundTag barrelContents(HolderLookup.Provider registries, @Nullable CompoundTag original, RandomSource random) {
        CompoundTag nbt = splashDisguise(registries, original);
        List<ItemStackWithSlot> slots = readItems(registries, nbt);
        if (slots.stream().anyMatch(slot -> disguisePotion(slot.stack()))) {
            return nbt;
        }
        for (ItemStack supply : ApothecaryLoot.barrelSupply(random)) {
            int slot = randomFreeBarrelSlot(slots, random);
            if (slot < 0) {
                break;
            }
            slots.add(new ItemStackWithSlot(slot, supply));
        }
        writeItems(registries, nbt, slots);
        return nbt;
    }

    static CompoundTag splashDisguise(HolderLookup.Provider registries, @Nullable CompoundTag original) {
        CompoundTag nbt = copy(original);
        List<ItemStackWithSlot> slots = readItems(registries, original);
        boolean changed = false;
        for (int index = 0; index < slots.size(); index++) {
            ItemStackWithSlot slot = slots.get(index);
            if (!bigBuffDisguise(slot.stack())) {
                continue;
            }
            ItemStack boundless = slot.stack().transmuteCopy(Items.SPLASH_POTION);
            BoundlessNight.bind(boundless);
            slots.set(index, new ItemStackWithSlot(slot.slot(), boundless));
            changed = true;
        }
        if (changed) {
            writeItems(registries, nbt, slots);
        }
        return nbt;
    }

    private static int randomFreeBarrelSlot(List<ItemStackWithSlot> slots, RandomSource random) {
        boolean[] used = new boolean[27];
        for (ItemStackWithSlot slot : slots) {
            if (slot.slot() >= 0 && slot.slot() < used.length) {
                used[slot.slot()] = true;
            }
        }
        int free = 0;
        for (boolean taken : used) {
            if (!taken) {
                free++;
            }
        }
        if (free == 0) {
            return -1;
        }
        int choice = random.nextInt(free);
        for (int slot = 0; slot < used.length; slot++) {
            if (!used[slot] && choice-- == 0) {
                return slot;
            }
        }
        return -1;
    }

    private static boolean disguisePotion(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.is(ModEffects.DISGUISE)) {
                return true;
            }
        }
        return false;
    }

    private static boolean bigBuffDisguise(ItemStack stack) {
        if (!stack.is(Items.POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        EssenceSubject essence = stack.get(ModDataComponents.ESSENCE.get());
        return contents != null
                && contents.potion().filter(potion -> potion.is(ModPotions.DISGUISE)).isPresent()
                && essence != null
                && "Big_Buff".equals(essence.playerName().orElse(""));
    }

    static CompoundTag beehiveBees(HolderLookup.Provider registries, @Nullable CompoundTag original) {
        CompoundTag nbt = copy(original);
        List<BeehiveBlockEntity.Occupant> bees = List.of(
                BeehiveBlockEntity.Occupant.create(0),
                BeehiveBlockEntity.Occupant.create(0)
        );
        BeehiveBlockEntity.Occupant.LIST_CODEC
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), bees)
                .resultOrPartial(BuffsPotions.LOGGER::error)
                .ifPresent(encoded -> nbt.put("bees", encoded));
        return nbt;
    }

    static CompoundTag stillBottle(HolderLookup.Provider registries, @Nullable CompoundTag original) {
        CompoundTag nbt = copy(original);
        List<ItemStackWithSlot> slots = readItems(registries, original);
        slots.removeIf(slot -> slot.slot() == BrewingStillBlockEntity.SLOT_BOTTLE);
        ItemStack brew = PotionContents.createItemStack(Items.POTION, ModPotions.SUSPICIOUS_BREW);
        slots.add(new ItemStackWithSlot(BrewingStillBlockEntity.SLOT_BOTTLE, brew));
        writeItems(registries, nbt, slots);
        return nbt;
    }

    private static List<ItemStackWithSlot> readItems(HolderLookup.Provider registries, @Nullable CompoundTag original) {
        if (original == null || !(original.get("Items") instanceof ListTag items) || items.isEmpty()) {
            return new ArrayList<>();
        }
        List<ItemStackWithSlot> slots = new ArrayList<>();
        ItemStackWithSlot.CODEC.listOf()
                .parse(registries.createSerializationContext(NbtOps.INSTANCE), items)
                .resultOrPartial(BuffsPotions.LOGGER::error)
                .ifPresent(slots::addAll);
        return slots;
    }

    private static void writeItems(HolderLookup.Provider registries, CompoundTag nbt, List<ItemStackWithSlot> slots) {
        ItemStackWithSlot.CODEC.listOf()
                .encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), slots)
                .resultOrPartial(BuffsPotions.LOGGER::error)
                .ifPresent(encoded -> nbt.put("Items", encoded));
    }

    private static CompoundTag copy(@Nullable CompoundTag original) {
        return original == null ? new CompoundTag() : original.copy();
    }
}
