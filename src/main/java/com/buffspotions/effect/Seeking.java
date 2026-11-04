package com.buffspotions.effect;

import com.buffspotions.flagon.AnchoredEnderFlagonBlock;
import com.buffspotions.flagon.AnchoredFlagons;
import com.buffspotions.mixin.ChunkMapLevelAccess;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Seeking {
    private static final int READS_IN_FLIGHT = 12;
    private static final Map<UUID, Job> JOBS = new java.util.HashMap<>();

    private Seeking() {
    }

    public static void begin(Player player) {
        if (!(player instanceof ServerPlayer server) || server.level().isClientSide()) {
            return;
        }
        ParameterDose dose = active(server);
        if (dose == null || !(server.level() instanceof ServerLevel level)) {
            return;
        }
        JOBS.put(server.getUUID(), Job.start(level, server, dose));
    }

    public static void forget(Player player) {
        JOBS.remove(player.getUUID());
        if (player.hasData(ModAttachments.SEEK_AIM.get())) {
            player.removeData(ModAttachments.SEEK_AIM);
        }
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.hasEffect(ModEffects.SEEKING)) {
                forget(player);
                continue;
            }
            Job job = JOBS.get(player.getUUID());
            if (job == null) {
                resume(player);
                job = JOBS.get(player.getUUID());
                if (job == null) {
                    continue;
                }
            }
            job.tick(player);
        }
    }

    private static void resume(ServerPlayer player) {
        ParameterDose dose = active(player);
        if (dose == null) {
            forget(player);
            return;
        }
        if (anchoredFlagon(dose)) {
            begin(player);
            return;
        }
        SeekAim aim = aim(player);
        if (dose.subject().block().isPresent()) {
            if (aim.found()) {
                JOBS.put(player.getUUID(), Job.watch(player, dose, aim));
            } else if (aim.searching()) {
                begin(player);
            }
            return;
        }
        begin(player);
    }

    private static ParameterDose active(ServerPlayer player) {
        List<ParameterDose> doses = ParameterDoses.matching(player, ModEffects.SEEKING);
        if (doses.isEmpty()) {
            return null;
        }
        return doses.get(doses.size() - 1);
    }

    private static SeekAim aim(ServerPlayer player) {
        return player.hasData(ModAttachments.SEEK_AIM.get())
                ? player.getData(ModAttachments.SEEK_AIM.get())
                : SeekAim.NONE;
    }

    private static void setAim(ServerPlayer player, SeekAim next) {
        if (!player.hasData(ModAttachments.SEEK_AIM.get()) || !player.getData(ModAttachments.SEEK_AIM.get()).equals(next)) {
            player.setData(ModAttachments.SEEK_AIM.get(), next);
        }
    }

    private static boolean withinChunks(ServerPlayer player, int chunkX, int chunkZ, int chunks) {
        int dx = Math.abs(chunkX - player.chunkPosition().x());
        int dz = Math.abs(chunkZ - player.chunkPosition().z());
        return Math.max(dx, dz) <= chunks;
    }

    private static boolean anchoredFlagon(ParameterDose dose) {
        return dose.subject().block().map(holder -> holder.value() instanceof AnchoredEnderFlagonBlock).orElse(false);
    }

    private enum Kind {
        PLAYER,
        MOB,
        BLOCK,
        FLAGON
    }

    private static final class Job {
        private final Kind kind;
        private final ServerLevel level;
        private final int chunks;
        private final Block block;
        private final String blockId;
        private final EntityType<?> mob;
        private final String name;
        private final int originX;
        private final int originZ;
        private int ring = -1;
        private int cursor;
        private final List<Long> columns = new ArrayList<>();
        private final List<Read> inflight = new ArrayList<>();
        private final List<Hit> hits = new ArrayList<>();
        private boolean watching;
        private BlockPos watchPos;

        private Job(
                Kind kind,
                ServerLevel level,
                int chunks,
                Block block,
                EntityType<?> mob,
                String name,
                int originX,
                int originZ
        ) {
            this.kind = kind;
            this.level = level;
            this.chunks = chunks;
            this.block = block;
            this.blockId = block == null ? "" : BuiltInRegistries.BLOCK.getKey(block).toString();
            this.mob = mob;
            this.name = name;
            this.originX = originX;
            this.originZ = originZ;
        }

        static Job start(ServerLevel level, ServerPlayer player, ParameterDose dose) {
            int chunks = EyedTuning.seekChunks(dose.amplifier());
            EssenceSubject subject = dose.subject();
            if (subject.playerName().isPresent()) {
                Job job = new Job(Kind.PLAYER, level, chunks, null, null, subject.playerName().get(), 0, 0);
                job.trackPlayer(player);
                return job;
            }
            if (anchoredFlagon(dose)) {
                Job job = new Job(Kind.FLAGON, level, chunks, null, null, "", 0, 0);
                job.trackFlagon(player);
                return job;
            }
            if (subject.block().isPresent()) {
                Block block = subject.block().get().value();
                Job job = new Job(
                        Kind.BLOCK,
                        level,
                        chunks,
                        block,
                        null,
                        "",
                        player.chunkPosition().x(),
                        player.chunkPosition().z()
                );
                setAim(player, SeekAim.SEARCH);
                job.nextRing();
                return job;
            }
            EntityType<?> type = subject.item().flatMap(MobDrops::type).orElse(null);
            Job job = new Job(Kind.MOB, level, chunks, null, type, "", 0, 0);
            job.trackMob(player);
            return job;
        }

        static Job watch(ServerPlayer player, ParameterDose dose, SeekAim aim) {
            Block block = dose.subject().block().map(holder -> holder.value()).orElse(null);
            ServerLevel level = player.level() instanceof ServerLevel server ? server : null;
            Job job = new Job(Kind.BLOCK, level, EyedTuning.seekChunks(dose.amplifier()), block, null, "", 0, 0);
            job.watching = true;
            job.watchPos = BlockPos.containing(aim.x(), aim.y(), aim.z());
            return job;
        }

        void tick(ServerPlayer player) {
            if (kind == Kind.PLAYER) {
                trackPlayer(player);
                return;
            }
            if (kind == Kind.MOB) {
                trackMob(player);
                return;
            }
            if (kind == Kind.FLAGON) {
                trackFlagon(player);
                return;
            }
            if (watching) {
                verify(player);
                return;
            }
            if (level == null) {
                giveUp(player);
                return;
            }
            poll();
            while (inflight.size() < READS_IN_FLIGHT && cursor < columns.size()) {
                long packed = columns.get(cursor++);
                int x = ChunkPos.getX(packed);
                int z = ChunkPos.getZ(packed);
                LevelChunk loaded = level.getChunkSource().getChunkNow(x, z);
                if (loaded != null) {
                    BlockPos pos = closestIn(loaded, player);
                    if (pos != null) {
                        hits.add(Hit.known(x, z, pos));
                    }
                } else {
                    ChunkPos pos = new ChunkPos(x, z);
                    inflight.add(new Read(x, z, ((ChunkMapLevelAccess) level.getChunkSource().chunkMap).buffsPotions$readChunk(pos)));
                }
            }
            if (cursor < columns.size() || !inflight.isEmpty()) {
                return;
            }
            for (Hit hit : hits) {
                if (hit.resolved) {
                    continue;
                }
                try {
                    hit.pos = closestIn(level.getChunk(hit.x, hit.z), player);
                } catch (RuntimeException ignored) {
                    hit.pos = null;
                }
                hit.resolved = true;
                return;
            }
            finishRing(player);
        }

        private void trackPlayer(ServerPlayer seeker) {
            ServerPlayer found = null;
            Vec3 aim = null;
            double best = Double.MAX_VALUE;
            for (ServerPlayer player : seeker.level().getServer().getPlayerList().getPlayers()) {
                if (player == seeker || !player.isAlive() || !player.getScoreboardName().equalsIgnoreCase(name)) {
                    continue;
                }
                if (player.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)) {
                    continue;
                }
                Located located = located(player);
                if (located.level() != seeker.level()) {
                    continue;
                }
                ChunkPos chunk = ChunkPos.containing(BlockPos.containing(located.position()));
                if (!withinChunks(seeker, chunk.x(), chunk.z(), chunks)) {
                    continue;
                }
                double distance = seeker.distanceToSqr(located.position());
                if (distance < best) {
                    best = distance;
                    found = player;
                    aim = located.position();
                }
            }
            if (found == null) {
                setAim(seeker, SeekAim.NONE);
            } else {
                setAim(seeker, SeekAim.found(seeker.level().dimension(), aim));
            }
        }

        /** A dislocated player is sought at the shadow. Invisibility does not hide anyone. */
        private static Located located(ServerPlayer player) {
            if (player.hasData(ModAttachments.DISLOCATION) && player.level().getServer() != null) {
                DislocationAnchor anchor = player.getData(ModAttachments.DISLOCATION);
                ServerLevel origin = player.level().getServer().getLevel(anchor.dimension());
                if (origin != null) {
                    Vec3 position = anchor.position().add(0.0, player.getEyeHeight(), 0.0);
                    if (origin.getEntity(anchor.shadowId()) instanceof LivingEntity shadow) {
                        position = shadow.getEyePosition();
                    }
                    return new Located(origin, position);
                }
            }
            return new Located(player.level(), player.getEyePosition());
        }

        /** Any distance, any level, and only flagons anchored to the seeker. */
        private void trackFlagon(ServerPlayer player) {
            if (!(player.level() instanceof ServerLevel world)) {
                setAim(player, SeekAim.NONE);
                return;
            }
            BlockPos pos = AnchoredFlagons.closest(player.getUUID(), world, player.position());
            if (pos == null) {
                setAim(player, SeekAim.NONE);
            } else {
                setAim(player, SeekAim.found(world.dimension(), Vec3.atCenterOf(pos)));
            }
        }

        private void trackMob(ServerPlayer player) {
            if (mob == null || !(player.level() instanceof ServerLevel world)) {
                setAim(player, SeekAim.NONE);
                return;
            }
            double reach = (chunks + 1) * 16.0;
            AABB area = new AABB(
                    player.getX() - reach,
                    world.getMinY(),
                    player.getZ() - reach,
                    player.getX() + reach,
                    world.getMaxY() + 1.0,
                    player.getZ() + reach
            );
            LivingEntity found = null;
            double best = Double.MAX_VALUE;
            for (LivingEntity entity : world.getEntitiesOfClass(
                    LivingEntity.class,
                    area,
                    candidate -> candidate.getType() == mob
                            && candidate.isAlive()
                            && withinChunks(player, candidate.chunkPosition().x(), candidate.chunkPosition().z(), chunks)
            )) {
                double distance = player.distanceToSqr(entity);
                if (distance < best) {
                    best = distance;
                    found = entity;
                }
            }
            if (found == null) {
                setAim(player, SeekAim.NONE);
            } else {
                setAim(player, SeekAim.found(world.dimension(), found.getEyePosition()));
            }
        }

        private void verify(ServerPlayer player) {
            if (block == null || watchPos == null || !(player.level() instanceof ServerLevel world)) {
                return;
            }
            if (!aim(player).dimension().equals(world.dimension().identifier().toString())) {
                return;
            }
            LevelChunk chunk = world.getChunkSource().getChunkNow(watchPos.getX() >> 4, watchPos.getZ() >> 4);
            if (chunk != null && !chunk.getBlockState(watchPos).is(block)) {
                giveUp(player);
            }
        }

        private void poll() {
            inflight.removeIf(read -> {
                if (!read.future.isDone()) {
                    return false;
                }
                Optional<CompoundTag> tag = Optional.empty();
                try {
                    tag = read.future.join();
                } catch (CompletionException | CancellationException ignored) {
                    tag = Optional.empty();
                }
                if (tag.isPresent() && paletteHas(tag.get())) {
                    hits.add(Hit.pending(read.x, read.z));
                }
                return true;
            });
        }

        private boolean paletteHas(CompoundTag tag) {
            String status = tag.getStringOr("Status", "");
            if (!status.equals("minecraft:full") && !status.equals("full")) {
                return false;
            }
            ListTag sections = tag.getListOrEmpty("sections");
            for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
                CompoundTag states = sections.getCompoundOrEmpty(sectionIndex).getCompound("block_states").orElse(null);
                if (states == null) {
                    continue;
                }
                ListTag palette = states.getListOrEmpty("palette");
                for (int entry = 0; entry < palette.size(); entry++) {
                    if (blockId.equals(palette.getCompoundOrEmpty(entry).getStringOr("Name", ""))) {
                        return true;
                    }
                }
            }
            return false;
        }

        private void finishRing(ServerPlayer player) {
            Hit best = null;
            double bestDistance = Double.MAX_VALUE;
            for (Hit hit : hits) {
                if (hit.pos == null) {
                    continue;
                }
                double distance = player.distanceToSqr(Vec3.atCenterOf(hit.pos));
                if (distance < bestDistance) {
                    bestDistance = distance;
                    best = hit;
                }
            }
            if (best != null && best.pos != null) {
                setAim(player, SeekAim.found(level.dimension(), Vec3.atCenterOf(best.pos)));
                watching = true;
                watchPos = best.pos;
                columns.clear();
                hits.clear();
                inflight.clear();
                return;
            }
            if (ring >= chunks) {
                giveUp(player);
                return;
            }
            nextRing();
        }

        private void nextRing() {
            ring++;
            cursor = 0;
            hits.clear();
            columns.clear();
            int span = ring == 0 ? 0 : ring;
            for (int dx = -span; dx <= span; dx++) {
                for (int dz = -span; dz <= span; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == ring) {
                        columns.add(ChunkPos.pack(originX + dx, originZ + dz));
                    }
                }
            }
        }

        private void giveUp(ServerPlayer player) {
            setAim(player, SeekAim.NONE);
            JOBS.remove(player.getUUID());
        }

        private BlockPos closestIn(LevelChunk chunk, ServerPlayer player) {
            BlockPos found = null;
            double best = Double.MAX_VALUE;
            LevelChunkSection[] sections = chunk.getSections();
            int minSection = chunk.getMinSectionY();
            int baseX = chunk.getPos().getMinBlockX();
            int baseZ = chunk.getPos().getMinBlockZ();
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
            for (int index = 0; index < sections.length; index++) {
                LevelChunkSection section = sections[index];
                if (section.hasOnlyAir() || !section.maybeHas(state -> state.is(block))) {
                    continue;
                }
                int baseY = (minSection + index) << 4;
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            BlockState state = section.getBlockState(x, y, z);
                            if (!state.is(block)) {
                                continue;
                            }
                            cursor.set(baseX + x, baseY + y, baseZ + z);
                            double distance = player.distanceToSqr(Vec3.atCenterOf(cursor));
                            if (distance < best) {
                                best = distance;
                                found = cursor.immutable();
                            }
                        }
                    }
                }
            }
            return found;
        }
    }

    private record Read(int x, int z, CompletableFuture<Optional<CompoundTag>> future) {
    }

    private record Located(net.minecraft.world.level.Level level, Vec3 position) {
    }

    private static final class Hit {
        private final int x;
        private final int z;
        private BlockPos pos;
        private boolean resolved;

        private Hit(int x, int z, BlockPos pos, boolean resolved) {
            this.x = x;
            this.z = z;
            this.pos = pos;
            this.resolved = resolved;
        }

        private static Hit known(int x, int z, BlockPos pos) {
            return new Hit(x, z, pos, true);
        }

        private static Hit pending(int x, int z) {
            return new Hit(x, z, null, false);
        }
    }
}
