package com.buffspotions.structure;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.jspecify.annotations.Nullable;

/**
 * Picks a spot where the coven's door opens into a cave and the rest of the building
 * is buried. A fully buried spot wins as soon as one is found.
 */
final class CovenFit {
    static final int MARGIN = 2;
    private static final int STEP = 2;
    private static final int[] PROBE_X;
    private static final int[] PROBE_Y;
    private static final int[] PROBE_Z;
    private static final int[] DX = {1, -1, 0, 0, 0, 0};
    private static final int[] DY = {0, 0, 1, -1, 0, 0};
    private static final int[] DZ = {0, 0, 0, 0, 1, -1};

    static {
        List<Integer> xs = new ArrayList<>();
        List<Integer> ys = new ArrayList<>();
        List<Integer> zs = new ArrayList<>();
        for (int z = 0; z < CovenLayout.SIZE_Z; z++) {
            for (int y = 0; y < CovenLayout.SIZE_Y; y++) {
                for (int x = 0; x < CovenLayout.SIZE_X; x++) {
                    if (!CovenLayout.contains(x, y, z)) {
                        continue;
                    }
                    for (int dir = 0; dir < 6; dir++) {
                        int nx = x + DX[dir];
                        int ny = y + DY[dir];
                        int nz = z + DZ[dir];
                        if (CovenLayout.contains(nx, ny, nz) || doorCell(nx, ny, nz)) {
                            continue;
                        }
                        xs.add(nx);
                        ys.add(ny);
                        zs.add(nz);
                    }
                }
            }
        }
        PROBE_X = copy(xs);
        PROBE_Y = copy(ys);
        PROBE_Z = copy(zs);
    }

    private CovenFit() {
    }

    record Placement(BlockPos corner, Rotation rotation, int exposed) {
    }

    interface Terrain {
        boolean air(int x, int y, int z);

        boolean covers(int x, int y, int z);

        boolean sturdyDown(int x, int y, int z);
    }

    static @Nullable Placement find(LevelReader level, RandomSource random, ChunkPos chunk) {
        Terrain terrain = reader(level);
        int readMinX = chunk.getMinBlockX() - 16;
        int readMaxX = chunk.getMinBlockX() + 31;
        int readMinZ = chunk.getMinBlockZ() - 16;
        int readMaxZ = chunk.getMinBlockZ() + 31;
        int minY = level.getMinY() + 5;
        Rotation[] rotations = Rotation.values();
        shuffle(rotations, random);
        Placement best = null;
        for (Rotation rotation : rotations) {
            int sizeX = spanX(rotation);
            int sizeZ = spanZ(rotation);
            int xStart = readMinX + MARGIN;
            int xEnd = readMaxX - sizeX;
            int zStart = readMinZ + MARGIN;
            int zEnd = readMaxZ - sizeZ;
            if (xStart > xEnd || zStart > zEnd) {
                continue;
            }
            int xCount = (xEnd - xStart) / STEP + 1;
            int zCount = (zEnd - zStart) / STEP + 1;
            int xPhase = random.nextInt(xCount);
            int zPhase = random.nextInt(zCount);
            for (int xi = 0; xi < xCount; xi++) {
                int x = xStart + ((xPhase + xi) % xCount) * STEP;
                for (int zi = 0; zi < zCount; zi++) {
                    int z = zStart + ((zPhase + zi) % zCount) * STEP;
                    int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x + sizeX / 2, z + sizeZ / 2);
                    int yEnd = Math.min(surface - CovenLayout.SIZE_Y - 1, level.getMaxY() - CovenLayout.SIZE_Y);
                    if (minY > yEnd) {
                        continue;
                    }
                    int yCount = (yEnd - minY) / STEP + 1;
                    int yPhase = random.nextInt(yCount);
                    for (int yi = 0; yi < yCount; yi++) {
                        int y = minY + ((yPhase + yi) % yCount) * STEP;
                        BlockPos corner = new BlockPos(x, y, z);
                        if (!doorOpens(corner, rotation, terrain) || !ceilingsHold(corner, rotation, terrain)) {
                            continue;
                        }
                        int limit = best == null ? Integer.MAX_VALUE : best.exposed;
                        int exposed = exposed(corner, rotation, terrain, limit);
                        if (exposed == 0) {
                            return new Placement(corner, rotation, 0);
                        }
                        if (best == null || exposed < best.exposed) {
                            best = new Placement(corner, rotation, exposed);
                        }
                    }
                }
            }
        }
        return best;
    }

    static boolean doorOpens(BlockPos corner, Rotation rotation, Terrain terrain) {
        int[] door = CovenLayout.DOOR_AIR;
        for (int i = 0; i < door.length; i += 3) {
            BlockPos pos = world(door[i], door[i + 1], door[i + 2], corner, rotation);
            if (!terrain.air(pos.getX(), pos.getY(), pos.getZ())) {
                return false;
            }
        }
        return true;
    }

    static boolean ceilingsHold(BlockPos corner, Rotation rotation, Terrain terrain) {
        int[] dripstone = CovenLayout.DRIPSTONE;
        for (int i = 0; i < dripstone.length; i += 3) {
            BlockPos pos = world(dripstone[i], dripstone[i + 1] + 1, dripstone[i + 2], corner, rotation);
            if (!terrain.sturdyDown(pos.getX(), pos.getY(), pos.getZ())) {
                return false;
            }
        }
        return true;
    }

    /** Faces of the template that look out of the saved blocks, other than the door. */
    static int exposed(BlockPos corner, Rotation rotation, Terrain terrain, int stopAfter) {
        int count = 0;
        for (int i = 0; i < PROBE_X.length; i++) {
            BlockPos pos = world(PROBE_X[i], PROBE_Y[i], PROBE_Z[i], corner, rotation);
            if (!terrain.covers(pos.getX(), pos.getY(), pos.getZ())) {
                count++;
                if (count > stopAfter) {
                    return count;
                }
            }
        }
        return count;
    }

    static BlockPos world(int x, int y, int z, BlockPos corner, Rotation rotation) {
        BlockPos place = StructureTemplate.getZeroPositionWithTransform(
                corner, Mirror.NONE, rotation, CovenLayout.SIZE_X, CovenLayout.SIZE_Z
        );
        return StructureTemplate.transform(new BlockPos(x, y, z), Mirror.NONE, rotation, BlockPos.ZERO).offset(place);
    }

    private static Terrain reader(LevelReader level) {
        return new Terrain() {
            @Override
            public boolean air(int x, int y, int z) {
                return level.getBlockState(new BlockPos(x, y, z)).isAir();
            }

            @Override
            public boolean covers(int x, int y, int z) {
                BlockState state = level.getBlockState(new BlockPos(x, y, z));
                return state.isSolid() && state.getFluidState().isEmpty();
            }

            @Override
            public boolean sturdyDown(int x, int y, int z) {
                BlockPos pos = new BlockPos(x, y, z);
                return level.getBlockState(pos).isFaceSturdy(level, pos, Direction.DOWN);
            }
        };
    }

    private static int spanX(Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? CovenLayout.SIZE_Z
                : CovenLayout.SIZE_X;
    }

    private static int spanZ(Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? CovenLayout.SIZE_X
                : CovenLayout.SIZE_Z;
    }

    private static boolean doorCell(int x, int y, int z) {
        int[] door = CovenLayout.DOOR_AIR;
        for (int i = 0; i < door.length; i += 3) {
            if (door[i] == x && door[i + 1] == y && door[i + 2] == z) {
                return true;
            }
        }
        return false;
    }

    private static void shuffle(Rotation[] rotations, RandomSource random) {
        for (int i = rotations.length - 1; i > 0; i--) {
            int swap = random.nextInt(i + 1);
            Rotation rotation = rotations[i];
            rotations[i] = rotations[swap];
            rotations[swap] = rotation;
        }
    }

    private static int[] copy(List<Integer> values) {
        int[] array = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }
}
