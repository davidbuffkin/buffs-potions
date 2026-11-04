package com.buffspotions.bellows;

import com.buffspotions.item.BellowsItem;
import com.buffspotions.mixin.CopperGolemWeatherAccess;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModCriteria;
import net.minecraft.server.level.ServerPlayer;
import com.buffspotions.stack.BrewingStackBlock;
import com.buffspotions.still.BrewingStillBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.minecraft.world.entity.animal.golem.CopperGolemState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.minecraft.world.entity.Entity;

public final class CopperGolemBellows {
    /** Same box the copper golem uses when it looks for chests. */
    private static final int HORIZONTAL_SEARCH = 32;
    private static final int VERTICAL_SEARCH = 8;
    private static final int WANDER_RADIUS = 10;
    /** How close the golem must be to the machine block, in blocks. */
    private static final double REACH = 0.75;
    private static final int USE_INTERVAL = 40;
    private static final int SEARCH_INTERVAL = 20;
    private static final int UNREACHABLE_RETRY = 600;
    /** Each bellows use brings the golem's next oxidation stage forward by this many ticks. */
    static final int OXIDATION_HURRY = 320;

    private CopperGolemBellows() {
    }

    public static void give(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof CopperGolem golem)) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof BellowsItem)) {
            return;
        }
        if (event.getEntity().isSecondaryUseActive()) {
            advanceOxidation(event, golem, held);
            return;
        }
        if (!golem.getMainHandItem().isEmpty()) {
            return;
        }
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Player player) {
            golem.setItemSlot(EquipmentSlot.MAINHAND, held.copyWithCount(1));
            golem.setGuaranteedDrop(EquipmentSlot.MAINHAND);
            if (!player.hasInfiniteMaterials()) {
                held.shrink(1);
            }
            golem.setState(CopperGolemState.IDLE);
            golem.clearOpenedChestPos();
            GolemBellowsJob job = golem.getData(ModAttachments.GOLEM_BELLOWS.get());
            BellowsClaims.releaseAll((ServerLevel) event.getLevel(), golem.getUUID());
            job.release();
            job.givenByPlayer = true;
            golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 100);
            golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            golem.getNavigation().stop();
            golem.playSound(SoundEvents.COPPER_GOLEM_ITEM_GET, 1.0F, 1.0F);
            if (player instanceof ServerPlayer server) {
                ModCriteria.BELLOWS_GOLEM.get().trigger(server);
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Sneak-use pushes the golem one oxidation stage. A fully oxidized golem is left alone. */
    private static void advanceOxidation(PlayerInteractEvent.EntityInteract event, CopperGolem golem, ItemStack held) {
        Player player = event.getEntity();
        if (player.getCooldowns().isOnCooldown(held) || golem.getWeatherState() == WeatheringCopper.WeatherState.OXIDIZED) {
            return;
        }
        if (!event.getLevel().isClientSide()) {
            golem.setWeatherState(golem.getWeatherState().next());
            player.getCooldowns().addCooldown(held, BellowsBoosts.COOLDOWN_TICKS);
            player.causeFoodExhaustion(BellowsBoosts.USE_EXHAUSTION);
            if (event.getLevel() instanceof ServerLevel level) {
                level.playSound(null, golem.getX(), golem.getY(), golem.getZ(), SoundEvents.BREEZE_SHOOT, SoundSource.PLAYERS, 0.4F, 1.35F);
                level.sendParticles(
                        ParticleTypes.POOF,
                        golem.getX(),
                        golem.getY() + golem.getBbHeight() * 0.6,
                        golem.getZ(),
                        7,
                        0.2,
                        0.15,
                        0.2,
                        0.01
                );
            }
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /**
     * An empty hand takes the bellows. Wax, an axe, or shears in either hand leave them in place.
     * On the client, wax and axes claim the click so the empty hand is not tried next.
     */
    public static @Nullable InteractionResult keepBellows(CopperGolem golem, Player player, InteractionHand hand) {
        if (!(golem.getMainHandItem().getItem() instanceof BellowsItem)) {
            return null;
        }
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        return bellowsInteract(
                player.getItemInHand(hand),
                player.getItemInHand(other),
                golem.level().isClientSide()
        );
    }

    static @Nullable InteractionResult bellowsInteract(ItemStack used, ItemStack other, boolean client) {
        if (used.isEmpty() && copperTool(other)) {
            return InteractionResult.PASS;
        }
        if (client && (used.is(Items.HONEYCOMB) || used.is(ItemTags.AXES))) {
            return InteractionResult.SUCCESS_SERVER;
        }
        return null;
    }

    private static boolean copperTool(ItemStack stack) {
        return stack.is(Items.HONEYCOMB) || stack.is(ItemTags.AXES) || stack.is(Items.SHEARS);
    }

    public static void tick(Entity entity) {
        if (!(entity instanceof CopperGolem golem) || !(golem.level() instanceof ServerLevel level) || golem.isNoAi()) {
            return;
        }
        GolemBellowsJob job = golem.getData(ModAttachments.GOLEM_BELLOWS.get());
        boolean holdingBellows = golem.getMainHandItem().getItem() instanceof BellowsItem;
        if (!holdingBellows || !job.givenByPlayer) {
            if (job.givenByPlayer || job.controlling) {
                BellowsClaims.releaseAll(level, golem.getUUID());
                job.release();
                golem.getBrain().eraseMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS);
                golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                golem.getNavigation().stop();
            }
            return;
        }
        job.controlling = true;
        golem.getBrain().setMemory(MemoryModuleType.TRANSPORT_ITEMS_COOLDOWN_TICKS, 100);
        if (golem.isPanicking()) {
            return;
        }
        if (level.getGameTime() >= job.unreachableResetAt) {
            job.unreachable.clear();
            job.unreachableResetAt = level.getGameTime() + UNREACHABLE_RETRY;
        }
        abandonUnreachable(level, golem, job);
        if (job.target != null) {
            if (BellowsClaims.claimedByOther(level, job.target, golem.getUUID())) {
                forgetTarget(level, golem, job, false);
            } else if (!BellowsClaims.owns(level, job.target, golem.getUUID())) {
                BellowsClaims.claim(level, job.target, golem.getUUID());
            }
        }
        if (job.target != null && !stillBusy(level, golem, job)) {
            forgetTarget(level, golem, job, true);
        }
        if (job.target == null && level.getGameTime() >= job.nextSearch) {
            job.nextSearch = level.getGameTime() + SEARCH_INTERVAL;
            chooseTarget(level, golem, job);
        }
        if (job.target != null && job.stand != null) {
            work(level, golem, job);
            return;
        }
        wander(level, golem, job);
    }

    private static void abandonUnreachable(ServerLevel level, CopperGolem golem, GolemBellowsJob job) {
        if (job.target == null) {
            return;
        }
        if (withinReach(golem, job.target) || golem.blockPosition().distManhattan(job.target) <= 2) {
            golem.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            return;
        }
        long since = golem.getBrain().getMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE).orElse(0L);
        if (since == 0L || level.getGameTime() - since < 60) {
            return;
        }
        job.unreachable.add(job.target.immutable());
        forgetTarget(level, golem, job, false);
        golem.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
    }

    /**
     * A machine that just finished one item is idle for a tick before the next
     * starts. Leaving immediately makes the golem turn away from a live furnace or stack.
     */
    private static boolean stillBusy(ServerLevel level, CopperGolem golem, GolemBellowsJob job) {
        BlockState state = level.getBlockState(job.target);
        if (golemMayOxidize(golem, state) || BellowsBoosts.isWorking(level.getBlockEntity(job.target))) {
            job.quietSince = 0;
            return true;
        }
        if (job.quietSince == 0) {
            job.quietSince = level.getGameTime();
        }
        return level.getGameTime() - job.quietSince < 30;
    }

    private static void forgetTarget(ServerLevel level, CopperGolem golem, GolemBellowsJob job, boolean anchorIfStoked) {
        if (job.target != null) {
            if (anchorIfStoked && job.stoked) {
                job.anchor = job.target.immutable();
            }
            BellowsClaims.release(level, job.target, golem.getUUID());
        }
        job.clearTask();
        job.nextSearch = 0;
        golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        golem.getNavigation().stop();
    }

    private static void chooseTarget(ServerLevel level, CopperGolem golem, GolemBellowsJob job) {
        int horizontal = golem.isPassenger() ? 1 : HORIZONTAL_SEARCH;
        int vertical = golem.isPassenger() ? 1 : VERTICAL_SEARCH;
        BlockPos origin = golem.blockPosition();
        BlockPos bestMachine = null;
        BlockPos bestStand = null;
        double bestDistance = Double.MAX_VALUE;
        int chunkRadius = Math.floorDiv(horizontal, 16) + 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (ChunkPos chunkPos : ChunkPos.rangeClosed(ChunkPos.containing(origin), chunkRadius).toList()) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(chunkPos.x(), chunkPos.z());
            if (chunk == null) {
                continue;
            }
            LevelChunkSection[] sections = chunk.getSections();
            int minSection = chunk.getMinSectionY();
            int baseX = chunk.getPos().getMinBlockX();
            int baseZ = chunk.getPos().getMinBlockZ();
            for (int index = 0; index < sections.length; index++) {
                LevelChunkSection section = sections[index];
                if (section.hasOnlyAir() || !section.maybeHas(CopperGolemBellows::isBellowsCandidate)) {
                    continue;
                }
                int baseY = (minSection + index) << 4;
                for (int y = 0; y < 16; y++) {
                    int worldY = baseY + y;
                    if (Math.abs(worldY - origin.getY()) > vertical) {
                        continue;
                    }
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            BlockState state = section.getBlockState(x, y, z);
                            if (!isBellowsCandidate(state)) {
                                continue;
                            }
                            cursor.set(baseX + x, worldY, baseZ + z);
                            if (Math.abs(cursor.getX() - origin.getX()) > horizontal
                                    || Math.abs(cursor.getZ() - origin.getZ()) > horizontal
                                    || job.unreachable.contains(cursor)) {
                                continue;
                            }
                            if (!readyToBellow(level, golem, cursor, state)
                                    || someoneBellowing(level, cursor)
                                    || !closestAvailable(level, golem, cursor)) {
                                continue;
                            }
                            BlockPos stand = approach(level, golem, cursor);
                            if (stand == null) {
                                continue;
                            }
                            double distance = cursor.distToCenterSqr(golem.position());
                            if (distance < bestDistance) {
                                bestDistance = distance;
                                bestMachine = cursor.immutable();
                                bestStand = stand.immutable();
                            }
                        }
                    }
                }
            }
        }
        if (job.target != null && (bestMachine == null || !job.target.equals(bestMachine))) {
            BellowsClaims.release(level, job.target, golem.getUUID());
        }
        job.target = bestMachine;
        job.stand = bestStand;
        job.stoked = false;
        job.nextUse = 0;
        if (bestMachine != null) {
            BellowsClaims.claim(level, bestMachine, golem.getUUID());
        }
    }

    private static boolean isBellowsCandidate(BlockState state) {
        return isBellowsMachine(state) || BellowsBoosts.canOxidize(state);
    }

    private static boolean isBellowsMachine(BlockState state) {
        return state.getBlock() instanceof BrewingStandBlock
                || state.getBlock() instanceof AbstractFurnaceBlock
                || state.getBlock() instanceof BrewingStillBlock
                || state.getBlock() instanceof BrewingStackBlock;
    }

    /** A working machine, or copper that is less oxidized than this golem. */
    private static boolean readyToBellow(ServerLevel level, CopperGolem golem, BlockPos pos, BlockState state) {
        if (golemMayOxidize(golem, state)) {
            return true;
        }
        return isBellowsMachine(state) && BellowsBoosts.isWorking(level.getBlockEntity(pos));
    }

    private static boolean golemMayOxidize(CopperGolem golem, BlockState state) {
        return BellowsBoosts.lessOxidizedThan(state, golem.getWeatherState());
    }

    /** A golem already pressed against the machine keeps it. */
    private static boolean someoneBellowing(ServerLevel level, BlockPos machine) {
        AABB box = new AABB(machine).inflate(2.0);
        for (CopperGolem other : level.getEntitiesOfClass(CopperGolem.class, box)) {
            GolemBellowsJob job = other.getData(ModAttachments.GOLEM_BELLOWS.get());
            if (machine.equals(job.target) && withinReach(other, machine)) {
                return true;
            }
        }
        return false;
    }

    /** The closest free bellows golem is the one that should start on a machine. */
    private static boolean closestAvailable(ServerLevel level, CopperGolem golem, BlockPos machine) {
        double mine = machine.distToCenterSqr(golem.position());
        AABB box = new AABB(machine).inflate(HORIZONTAL_SEARCH, VERTICAL_SEARCH, HORIZONTAL_SEARCH);
        for (CopperGolem other : level.getEntitiesOfClass(CopperGolem.class, box, candidate -> candidate != golem && canTake(candidate, machine))) {
            double theirs = machine.distToCenterSqr(other.position());
            if (theirs < mine || (theirs == mine && other.getUUID().compareTo(golem.getUUID()) < 0)) {
                return false;
            }
        }
        return true;
    }

    private static boolean canTake(CopperGolem golem, BlockPos machine) {
        if (!(golem.getMainHandItem().getItem() instanceof BellowsItem) || golem.isPanicking()) {
            return false;
        }
        GolemBellowsJob job = golem.getData(ModAttachments.GOLEM_BELLOWS.get());
        if (!job.givenByPlayer) {
            return false;
        }
        if (job.target == null) {
            return true;
        }
        return machine.equals(job.target) && !withinReach(golem, machine);
    }

    private static void work(ServerLevel level, CopperGolem golem, GolemBellowsJob job) {
        golem.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(job.target));
        boolean arrived = withinReach(golem, job.target);
        if (!arrived) {
            if (!golem.isLeashed()) {
                pressCloser(golem, job);
            }
            return;
        }
        golem.getNavigation().stop();
        // A missing walk target lets the idle stroll start and walk him off the machine.
        golem.getBrain().setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(new BlockPosTracker(golem.blockPosition()), 0.0F, 0)
        );
        golem.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        if (level.getGameTime() < job.nextUse) {
            return;
        }
        BlockState state = level.getBlockState(job.target);
        boolean oxidizable = golemMayOxidize(golem, state);
        if (!oxidizable && !BellowsBoosts.isWorking(level.getBlockEntity(job.target))) {
            return;
        }
        boolean used = oxidizable
                ? BellowsBoosts.oxidize(level, job.target, 0.25F)
                : BellowsBoosts.stoke(level, job.target, 0.25F);
        if (!used) {
            return;
        }
        job.stoked = true;
        job.anchor = job.target.immutable();
        job.nextUse = level.getGameTime() + USE_INTERVAL;
        golem.setData(ModAttachments.GOLEM_HEAD_SPIN.get(), golem.getData(ModAttachments.GOLEM_HEAD_SPIN.get()) + 1L);
        hurryOxidation(golem);
    }

    /** Waxed golems, and golems with no countdown yet, keep their weathering tick. */
    static long hurriedDeadline(long nextWeatheringTick) {
        if (nextWeatheringTick <= 0L) {
            return nextWeatheringTick;
        }
        return nextWeatheringTick - OXIDATION_HURRY;
    }

    private static void hurryOxidation(CopperGolem golem) {
        CopperGolemWeatherAccess access = (CopperGolemWeatherAccess) (Object) golem;
        access.buffsPotions$setNextWeatheringTick(hurriedDeadline(access.buffsPotions$getNextWeatheringTick()));
    }

    /**
     * Path to a floor beside the machine. A walk target on the block he is
     * already standing on makes the pathfinder spin in place.
     */
    private static void pressCloser(CopperGolem golem, GolemBellowsJob job) {
        if (job.stand == null || job.stand.equals(golem.blockPosition())) {
            return;
        }
        golem.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        BehaviorUtils.setWalkAndLookTargetMemories(golem, job.stand, 1.0F, 0);
    }

    private static void wander(ServerLevel level, CopperGolem golem, GolemBellowsJob job) {
        if (job.anchor == null || golem.isLeashed()) {
            return;
        }
        if (golem.blockPosition().distSqr(job.anchor) > (long) WANDER_RADIUS * WANDER_RADIUS) {
            BlockPos home = approach(level, golem, job.anchor);
            if (home != null) {
                BehaviorUtils.setWalkAndLookTargetMemories(golem, home, 1.0F, 0);
            }
            return;
        }
        if (level.getGameTime() >= job.nextWander) {
            BlockPos spot = wanderSpot(level, golem, job.anchor);
            job.nextWander = level.getGameTime() + golem.getRandom().nextInt(40, 90);
            if (spot != null) {
                BehaviorUtils.setWalkAndLookTargetMemories(golem, spot, 1.0F, 0);
            }
            return;
        }
        if (golem.getNavigation().isDone()) {
            golem.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    private static BlockPos wanderSpot(ServerLevel level, CopperGolem golem, BlockPos anchor) {
        if (golem.blockPosition().distSqr(anchor) > (long) WANDER_RADIUS * WANDER_RADIUS) {
            return approach(level, golem, anchor);
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = golem.getRandom().nextInt(-WANDER_RADIUS, WANDER_RADIUS + 1);
            int dz = golem.getRandom().nextInt(-WANDER_RADIUS, WANDER_RADIUS + 1);
            int dy = golem.getRandom().nextInt(-2, 3);
            if (dx * dx + dz * dz + dy * dy > WANDER_RADIUS * WANDER_RADIUS) {
                continue;
            }
            BlockPos spot = anchor.offset(dx, dy, dz);
            if (canStand(level, spot)) {
                return spot;
            }
        }
        return approach(level, golem, anchor);
    }

    /**
     * Standing on a side of the machine counts, including one block above that side.
     * The old sphere stopped him on the near edge of the neighboring block.
     */
    private static boolean withinReach(CopperGolem golem, BlockPos machine) {
        BlockPos feet = golem.blockPosition();
        int dx = Math.abs(feet.getX() - machine.getX());
        int dz = Math.abs(feet.getZ() - machine.getZ());
        int dy = feet.getY() - machine.getY();
        if (Math.max(dx, dz) == 1 && Math.min(dx, dz) == 0 && dy >= -1 && dy <= 1) {
            return true;
        }
        double gapX = axisGap(golem.getX(), machine.getX());
        double gapY = Math.max(0.0, axisGap(golem.getY(), machine.getY()) - 1.0);
        double gapZ = axisGap(golem.getZ(), machine.getZ());
        return gapX * gapX + gapY * gapY + gapZ * gapZ <= REACH * REACH;
    }

    private static double axisGap(double coord, int block) {
        if (coord < block) {
            return block - coord;
        }
        if (coord > block + 1.0) {
            return coord - (block + 1.0);
        }
        return 0.0;
    }

    private static BlockPos approach(ServerLevel level, CopperGolem golem, BlockPos machine) {
        BlockPos best = nearestStand(level, golem, machine, 1);
        return best != null ? best : nearestStand(level, golem, machine, 2);
    }

    private static BlockPos nearestStand(ServerLevel level, CopperGolem golem, BlockPos machine, int radius) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (Math.abs(dx) != 0 && Math.abs(dz) != 0) {
                    continue;
                }
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos stand = machine.offset(dx, dy, dz);
                    if (!canStand(level, stand) || !sideReaches(stand, machine)) {
                        continue;
                    }
                    double distance = stand.distToCenterSqr(golem.position());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = stand;
                    }
                }
            }
        }
        return best;
    }

    private static boolean sideReaches(BlockPos stand, BlockPos machine) {
        int dx = Math.abs(stand.getX() - machine.getX());
        int dz = Math.abs(stand.getZ() - machine.getZ());
        int dy = stand.getY() - machine.getY();
        return Math.max(dx, dz) == 1 && Math.min(dx, dz) == 0 && dy >= -1 && dy <= 1;
    }

    private static boolean canStand(ServerLevel level, BlockPos pos) {
        if (!level.getWorldBorder().isWithinBounds(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.getCollisionShape(level, pos).isEmpty() || state.getFluidState().is(FluidTags.LAVA)) {
            return false;
        }
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
