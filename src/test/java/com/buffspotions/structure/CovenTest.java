package com.buffspotions.structure;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SpeleothemBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.testframework.junit.EphemeralTestServerProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(EphemeralTestServerProvider.class)
class CovenTest {
    @Test
    void chestIsLapisABundleAndDistinctIngredients(MinecraftServer server) {
        Set<Item> allowed = Set.of(
                Items.SPIDER_EYE,
                com.buffspotions.registry.ModItems.WING.get(),
                Items.RABBIT_HIDE,
                Items.GLOW_BERRIES,
                Items.CHORUS_FLOWER,
                Items.RABBIT_FOOT,
                Items.SLIME_BALL,
                Items.POISONOUS_POTATO,
                Items.PHANTOM_MEMBRANE
        );
        RandomSource random = RandomSource.create(11);
        for (int i = 0; i < 40; i++) {
            List<ItemStack> stacks = CovenLoot.chest(random);
            assertTrue(stacks.get(0).is(Items.LAPIS_LAZULI));
            assertTrue(stacks.get(0).getCount() >= 5 && stacks.get(0).getCount() <= 8);
            assertTrue(stacks.get(1).is(Items.BUNDLE));
            assertEquals(1, stacks.get(1).getCount());
            assertTrue(stacks.size() >= 5 && stacks.size() <= 7);
            Set<Item> extras = new HashSet<>();
            for (int index = 2; index < stacks.size(); index++) {
                ItemStack stack = stacks.get(index);
                assertTrue(allowed.contains(stack.getItem()));
                assertTrue(stack.getCount() >= 1 && stack.getCount() <= 4);
                assertTrue(extras.add(stack.getItem()));
            }
        }
    }

    @Test
    void doorBuriedInStoneIsUnexposedAndALeakCounts(MinecraftServer server) {
        BlockPos corner = new BlockPos(0, 10, 0);
        Stones stone = new Stones();
        carveDoor(stone, corner, Rotation.NONE);
        assertTrue(CovenFit.doorOpens(corner, Rotation.NONE, stone));
        assertTrue(CovenFit.ceilingsHold(corner, Rotation.NONE, stone));
        assertEquals(0, CovenFit.exposed(corner, Rotation.NONE, stone, Integer.MAX_VALUE));

        stone.air(CovenFit.world(6, 17, 5, corner, Rotation.NONE));
        assertTrue(CovenFit.exposed(corner, Rotation.NONE, stone, Integer.MAX_VALUE) > 0);

        stone.fill(CovenFit.world(6, 13, -1, corner, Rotation.NONE));
        assertFalse(CovenFit.doorOpens(corner, Rotation.NONE, stone));
    }

    @Test
    void rotationKeepsTheDoorOutsideTheBuilding(MinecraftServer server) {
        BlockPos corner = new BlockPos(100, 20, 200);
        assertEquals(new BlockPos(111, 20, 216), CovenFit.world(0, 0, 0, corner, Rotation.CLOCKWISE_180));
        BlockPos mouth = CovenFit.world(6, 13, -1, corner, Rotation.NONE);
        assertEquals(corner.offset(6, 13, -1), mouth);
        assertTrue(CovenLayout.contains(CovenLayout.DRIPSTONE[0], CovenLayout.DRIPSTONE[1], CovenLayout.DRIPSTONE[2]));
        assertFalse(CovenLayout.contains(6, 8, 11));
        assertTrue(CovenLayout.contains(CovenLayout.BAT_X, CovenLayout.BAT_Y, CovenLayout.BAT_Z));
    }

    @Test
    void generationWakesWitchesAndHangsDripstone(MinecraftServer server) {
        CompoundTag saved = new CompoundTag();
        saved.putString("id", "minecraft:witch");
        saved.putBoolean("NoAI", true);
        saved.put("Brain", new CompoundTag());
        StructureTemplate.StructureEntityInfo info = new StructureTemplate.StructureEntityInfo(
                Vec3.ZERO, BlockPos.ZERO, saved
        );
        StructureTemplate.StructureEntityInfo woken = new CovenProcessor().processEntity(
                null, BlockPos.ZERO, info, info, new StructurePlaceSettings(), null
        );
        assertTrue(woken.nbt.getBoolean("NoAI").isEmpty());
        assertTrue(woken.nbt.getCompound("Brain").isEmpty());
        assertTrue(woken.nbt.getBooleanOr("PersistenceRequired", false));

        StructureTemplate.StructureBlockInfo air = new StructureTemplate.StructureBlockInfo(
                new BlockPos(6, 7, 11), Blocks.AIR.defaultBlockState(), null
        );
        StructureTemplate.StructureBlockInfo dripstone = new CovenProcessor().process(
                null, BlockPos.ZERO, BlockPos.ZERO, air, air, new StructurePlaceSettings(), null
        );
        assertTrue(dripstone.state().is(Blocks.POINTED_DRIPSTONE));
        assertEquals(Direction.DOWN, dripstone.state().getValue(SpeleothemBlock.TIP_DIRECTION));

        StructureTemplate.StructureBlockInfo pot = new StructureTemplate.StructureBlockInfo(
                new BlockPos(9, 14, 5), Blocks.FLOWER_POT.defaultBlockState(), null
        );
        StructureTemplate.StructureBlockInfo planted = new CovenProcessor().process(
                null, BlockPos.ZERO, BlockPos.ZERO, pot, pot, new StructurePlaceSettings(), null
        );
        assertFalse(planted.state().is(Blocks.FLOWER_POT));
        assertFalse(planted.state().isAir());

        for (int x : new int[]{5, 7}) {
            StructureTemplate.StructureBlockInfo porch = new StructureTemplate.StructureBlockInfo(
                    new BlockPos(x, 14, 0), Blocks.AIR.defaultBlockState(), null
            );
            StructureTemplate.StructureBlockInfo torch = new CovenProcessor().process(
                    null, BlockPos.ZERO, BlockPos.ZERO, porch, porch, new StructurePlaceSettings(), null
            );
            assertTrue(torch.state().is(Blocks.WALL_TORCH));
            assertEquals(Direction.NORTH, torch.state().getValue(WallTorchBlock.FACING));
        }
    }

    private static void carveDoor(Stones stone, BlockPos corner, Rotation rotation) {
        int[] door = CovenLayout.DOOR_AIR;
        for (int i = 0; i < door.length; i += 3) {
            stone.air(CovenFit.world(door[i], door[i + 1], door[i + 2], corner, rotation));
        }
    }

    private static final class Stones implements CovenFit.Terrain {
        private final Set<Long> holes = new HashSet<>();

        void air(BlockPos pos) {
            this.holes.add(pos.asLong());
        }

        void fill(BlockPos pos) {
            this.holes.remove(pos.asLong());
        }

        @Override
        public boolean air(int x, int y, int z) {
            return this.holes.contains(BlockPos.asLong(x, y, z));
        }

        @Override
        public boolean covers(int x, int y, int z) {
            return !this.air(x, y, z);
        }

        @Override
        public boolean sturdyDown(int x, int y, int z) {
            return this.covers(x, y, z);
        }
    }
}
