package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.effect.EyedDisplay;
import com.buffspotions.mixin.client.BlockModelRenderStateAccessor;
import com.buffspotions.mixin.client.ChestSpecialRendererAccess;
import com.buffspotions.effect.EyedTuning;
import com.buffspotions.effect.ParameterDose;
import com.buffspotions.effect.ParameterDoses;
import com.buffspotions.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ChestSpecialRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.FluidTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.extensions.IRenderStateExtension;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID, value = Dist.CLIENT)
public final class BlockSenseClient {
    public static final ContextKey<long[]> BLOCKS = new ContextKey<>(
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "block_sense")
    );
    private static final int MAX_BLOCKS = 12000;
    private static final int STEPS_PER_TICK = 50000;
    private static final ArrayDeque<Long> PENDING = new ArrayDeque<>();
    private static final LongOpenHashSet PENDING_SET = new LongOpenHashSet();
    private static final LongOpenHashSet SCANNED = new LongOpenHashSet();
    private static final LongOpenHashSet VISIBLE = new LongOpenHashSet();
    private static final Long2ObjectOpenHashMap<LongOpenHashSet> BY_SECTION = new Long2ObjectOpenHashMap<>();
    private static final LongOpenHashSet SECTION_HITS = new LongOpenHashSet();
    private static final BlockPos.MutableBlockPos CURSOR = new BlockPos.MutableBlockPos();
    private static final BlockModelRenderState MODEL = new BlockModelRenderState();
    private static final BlockDisplayContext DISPLAY = BlockDisplayContext.create();
    private static final int OUTLINE_COLOR = 0xFFFFD53D;
    private static final int FULL_BRIGHT = 15728880;
    private static final long[] EMPTY = new long[0];
    private static Block[] senseBlocks = new Block[0];
    private static long[] senseRadiusSq = new long[0];
    private static int[] senseAir = new int[0];
    private static final BlockPos.MutableBlockPos AIR_CURSOR = new BlockPos.MutableBlockPos();
    private static final Direction[] FACES = Direction.values();
    private static String senseSignature = "";
    private static ResourceKey<Level> dimension;
    private static int radius;
    private static int centerX;
    private static int centerY;
    private static int centerZ;
    private static long radiusSquared;
    private static boolean moved;
    private static boolean dirty;
    private static long[] renderBlocks = EMPTY;
    private static boolean readingSection;
    private static long readingKey;
    private static int readingIndex;
    private static boolean readingStale;
    private static int admitted;
    private static int budget;
    private static boolean applied;
    private static int appliedX;
    private static int appliedY;
    private static int appliedZ;
    private static boolean sweeping;
    private static int originX;
    private static int originY;
    private static int originZ;
    private static int destX;
    private static int destY;
    private static int destZ;
    private static int cursorY;
    private static int cursorX;
    private static int cursorZ;
    private static boolean cursorReady;
    private static long sweepSection = Long.MIN_VALUE;
    private static boolean sweepSectionEmpty;
    private static int readingCenterX;
    private static int readingCenterY;
    private static int readingCenterZ;

    static {
        EyedDisplay.SENSE = () -> ParameterNames.join(ModEffects.BLOCK_SENSE, "effect.buffs_potions.block_sense.named", true);
        EyedDisplay.EATER = () -> ParameterNames.join(ModEffects.ITEM_EATER, "effect.buffs_potions.item_eater.named", false);
    }

    private BlockSenseClient() {
    }

    public static void markChanged(BlockPos pos) {
        if (senseBlocks.length == 0) {
            return;
        }
        int pad = 1;
        int sx = SectionPos.blockToSectionCoord(pos.getX());
        int sy = SectionPos.blockToSectionCoord(pos.getY());
        int sz = SectionPos.blockToSectionCoord(pos.getZ());
        for (int dx = -pad; dx <= pad; dx++) {
            for (int dy = -pad; dy <= pad; dy++) {
                for (int dz = -pad; dz <= pad; dz++) {
                    invalidate(SectionPos.asLong(sx + dx, sy + dy, sz + dz));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (senseBlocks.length == 0 || !event.getLevel().isClientSide() || !(event.getLevel() instanceof ClientLevel level)) {
            return;
        }
        if (level.dimension() != dimension) {
            return;
        }
        int chunkX = event.getChunk().getPos().x();
        int chunkZ = event.getChunk().getPos().z();
        for (int sectionY = level.getMinSectionY(); sectionY <= level.getMaxSectionY(); sectionY++) {
            invalidate(SectionPos.asLong(chunkX, sectionY, chunkZ));
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (senseBlocks.length == 0 || !event.getLevel().isClientSide()) {
            return;
        }
        int chunkX = event.getChunk().getPos().x();
        int chunkZ = event.getChunk().getPos().z();
        LongIterator sections = SCANNED.iterator();
        while (sections.hasNext()) {
            long key = sections.nextLong();
            if (SectionPos.x(key) == chunkX && SectionPos.z(key) == chunkZ) {
                sections.remove();
                forgetResults(key);
            }
        }
        PENDING.removeIf(key -> SectionPos.x(key) == chunkX && SectionPos.z(key) == chunkZ);
        PENDING_SET.removeIf((long key) -> SectionPos.x(key) == chunkX && SectionPos.z(key) == chunkZ);
        if (readingSection && SectionPos.x(readingKey) == chunkX && SectionPos.z(readingKey) == chunkZ) {
            abandonRead();
        }
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !(minecraft.level instanceof ClientLevel level) || !player.hasEffect(ModEffects.BLOCK_SENSE)) {
            clearScan();
            return;
        }
        var doses = ParameterDoses.matching(player, ModEffects.BLOCK_SENSE);
        if (doses.isEmpty()) {
            clearScan();
            return;
        }
        StringBuilder signature = new StringBuilder();
        int counted = 0;
        int maxRadius = 0;
        for (ParameterDose dose : doses) {
            if (dose.subject().block().isEmpty()) {
                continue;
            }
            int doseRadius = EyedTuning.radius(dose.amplifier());
            maxRadius = Math.max(maxRadius, doseRadius);
            signature.append(dose.subject().block().get().unwrapKey().map(key -> key.identifier().toString()).orElse(""))
                    .append('@')
                    .append(doseRadius)
                    .append('/')
                    .append(EyedTuning.airDistance(dose.amplifier()))
                    .append(';');
            counted++;
        }
        if (counted == 0) {
            clearScan();
            return;
        }
        String nextSignature = signature.toString();
        if (!nextSignature.equals(senseSignature) || dimension != level.dimension()) {
            clearScan();
            Block[] blocks = new Block[counted];
            long[] radii = new long[counted];
            int[] air = new int[counted];
            int index = 0;
            for (ParameterDose dose : doses) {
                if (dose.subject().block().isEmpty()) {
                    continue;
                }
                int doseRadius = EyedTuning.radius(dose.amplifier());
                blocks[index] = dose.subject().block().get().value();
                radii[index] = (long) doseRadius * doseRadius;
                air[index] = EyedTuning.airDistance(dose.amplifier());
                index++;
            }
            senseBlocks = blocks;
            senseRadiusSq = radii;
            senseAir = air;
            senseSignature = nextSignature;
            dimension = level.dimension();
        }
        radius = maxRadius;
        BlockPos pos = player.blockPosition();
        moved = pos.getX() != centerX || pos.getY() != centerY || pos.getZ() != centerZ || radiusSquared == 0;
        centerX = pos.getX();
        centerY = pos.getY();
        centerZ = pos.getZ();
        radiusSquared = (long) radius * radius;
        cover(level);
        if (moved) {
            prune(level);
        }
        budget = STEPS_PER_TICK;
        follow(level);
        scan(level);
        if (dirty || moved) {
            publish();
        }
    }

    @SubscribeEvent
    public static void extract(ExtractLevelRenderStateEvent event) {
        ((IRenderStateExtension) event.getRenderState()).setRenderData(BLOCKS, renderBlocks.length == 0 ? null : renderBlocks);
    }

    @SubscribeEvent
    public static void submit(SubmitCustomGeometryEvent event) {
        long[] blocks = ((IRenderStateExtension) event.getLevelRenderState()).getRenderData(BLOCKS);
        if (blocks == null || blocks.length == 0) {
            return;
        }
        HashSet<Long> selected = new HashSet<>(blocks.length * 2);
        for (long packed : blocks) {
            selected.add(packed);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Vec3 camera = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack poseStack = event.getPoseStack();
        for (long packed : blocks) {
            if (buried(packed, selected)) {
                continue;
            }
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);
            BlockPos pos = BlockPos.of(packed);
            BlockState outlineState = minecraft.level.getBlockState(pos);
            poseStack.pushPose();
            poseStack.translate(x - camera.x, y - camera.y, z - camera.z);
            minecraft.getBlockModelResolver().update(MODEL, outlineState, DISPLAY);
            if (!submitChestOutline(minecraft.level, pos, poseStack, event.getSubmitNodeCollector())) {
                if (outlineNeedsStandIn()) {
                    minecraft.getBlockModelResolver().update(MODEL, Blocks.OAK_PLANKS.defaultBlockState(), DISPLAY);
                }
                if (!MODEL.isEmpty()) {
                    MODEL.submitOnlyOutline(
                            poseStack,
                            event.getSubmitNodeCollector(),
                            FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY,
                            OUTLINE_COLOR
                    );
                }
            }
            poseStack.popPose();
        }
    }

    /**
     * Chests are drawn by a special model with no block cubes. Submit that model to the outline
     * buffer so the silhouette matches the chest, shows through terrain, and merges with neighbors.
     */
    private static boolean submitChestOutline(ClientLevel level, BlockPos pos, PoseStack poseStack, SubmitNodeCollector collector) {
        BlockModelRenderStateAccessor state = (BlockModelRenderStateAccessor) MODEL;
        if (!(state.buffsPotions$specialRenderer() instanceof ChestSpecialRenderer)) {
            return false;
        }
        ChestSpecialRendererAccess chest = (ChestSpecialRendererAccess) state.buffsPotions$specialRenderer();
        Matrix4fc transform = state.buffsPotions$specialRendererTransformation();
        poseStack.pushPose();
        if (transform != null) {
            poseStack.mulPose(transform);
        }
        collector.submitModel(
                chest.buffsPotions$model(),
                lidOpenness(level, pos),
                poseStack,
                RenderTypes.outline(chest.buffsPotions$sprite().atlasLocation()),
                FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                -1,
                chest.buffsPotions$sprites().get(chest.buffsPotions$sprite()),
                OUTLINE_COLOR,
                (ModelFeatureRenderer.CrumblingOverlay) null
        );
        poseStack.popPose();
        return true;
    }

    /** Same lid curve the chest renderer uses, so an open chest's outline follows the lid. */
    private static float lidOpenness(ClientLevel level, BlockPos pos) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (!(entity instanceof LidBlockEntity lid)) {
            return 0.0F;
        }
        float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float open = 1.0F - lid.getOpenNess(partial);
        return 1.0F - open * open * open;
    }

    /** A particle-only model has no cubes to outline, so a solid block stands in. */
    private static boolean outlineNeedsStandIn() {
        if (MODEL.isEmpty()) {
            return true;
        }
        List<?> parts = ((BlockModelRenderStateAccessor) MODEL).buffsPotions$modelParts();
        return parts == null || parts.isEmpty();
    }

    private static boolean buried(long packed, HashSet<Long> selected) {
        int x = BlockPos.getX(packed);
        int y = BlockPos.getY(packed);
        int z = BlockPos.getZ(packed);
        return selected.contains(BlockPos.asLong(x + 1, y, z))
                && selected.contains(BlockPos.asLong(x - 1, y, z))
                && selected.contains(BlockPos.asLong(x, y + 1, z))
                && selected.contains(BlockPos.asLong(x, y - 1, z))
                && selected.contains(BlockPos.asLong(x, y, z + 1))
                && selected.contains(BlockPos.asLong(x, y, z - 1));
    }

    private static void clearScan() {
        senseBlocks = new Block[0];
        senseRadiusSq = new long[0];
        senseAir = new int[0];
        senseSignature = "";
        dimension = null;
        radius = 0;
        centerX = 0;
        centerY = 0;
        centerZ = 0;
        radiusSquared = 0;
        moved = false;
        readingSection = false;
        readingStale = false;
        readingIndex = 0;
        PENDING.clear();
        PENDING_SET.clear();
        SCANNED.clear();
        VISIBLE.clear();
        BY_SECTION.clear();
        SECTION_HITS.clear();
        if (renderBlocks.length != 0) {
            renderBlocks = EMPTY;
        }
        dirty = false;
        applied = false;
        sweeping = false;
        sweepSection = Long.MIN_VALUE;
    }

    private static void cover(ClientLevel level) {
        if (readingSection && !intersects(readingKey)) {
            abandonRead();
        }
        LongIterator scanned = SCANNED.iterator();
        while (scanned.hasNext()) {
            long key = scanned.nextLong();
            if (!intersects(key)) {
                scanned.remove();
                forgetResults(key);
            }
        }
        LongIterator pending = PENDING_SET.iterator();
        while (pending.hasNext()) {
            long key = pending.nextLong();
            if (!intersects(key)) {
                pending.remove();
                PENDING.remove(key);
            }
        }
        int sectionRadius = (radius >> 4) + 1;
        int centerSectionX = SectionPos.blockToSectionCoord(centerX);
        int centerSectionY = SectionPos.blockToSectionCoord(centerY);
        int centerSectionZ = SectionPos.blockToSectionCoord(centerZ);
        int minSectionY = level.getMinSectionY();
        int maxSectionY = level.getMaxSectionY();
        for (int ring = 0; ring <= sectionRadius; ring++) {
            for (int sx = centerSectionX - ring; sx <= centerSectionX + ring; sx++) {
                for (int sy = Math.max(minSectionY, centerSectionY - ring); sy <= Math.min(maxSectionY, centerSectionY + ring); sy++) {
                    for (int sz = centerSectionZ - ring; sz <= centerSectionZ + ring; sz++) {
                        int shell = Math.max(
                                Math.abs(sx - centerSectionX),
                                Math.max(Math.abs(sy - centerSectionY), Math.abs(sz - centerSectionZ))
                        );
                        if (shell != ring || level.getChunkSource().getChunkNow(sx, sz) == null) {
                            continue;
                        }
                        enqueue(SectionPos.asLong(sx, sy, sz));
                    }
                }
            }
        }
    }

    private static void scan(ClientLevel level) {
        int misses = 0;
        int missLimit = PENDING.size() + 1;
        while (budget > 0 && misses < missLimit) {
            if (!readingSection) {
                if (PENDING.isEmpty()) {
                    return;
                }
                long key = PENDING.removeFirst();
                PENDING_SET.remove(key);
                if (!intersects(key)) {
                    continue;
                }
                LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.x(key), SectionPos.z(key));
                if (chunk == null) {
                    enqueue(key);
                    misses++;
                    continue;
                }
                readingKey = key;
                readingIndex = 0;
                readingSection = true;
                readingStale = false;
                readingCenterX = centerX;
                readingCenterY = centerY;
                readingCenterZ = centerZ;
                admitted = 0;
                SECTION_HITS.clear();
                if (emptyOfTargets(chunk, key)) {
                    commitRead();
                }
            }
            if (readingSection) {
                if (!readSection(level)) {
                    return;
                }
            }
        }
    }

    private static boolean emptyOfTargets(LevelChunk chunk, long key) {
        int sectionIndex = chunk.getSectionIndexFromSectionY(SectionPos.y(key));
        LevelChunkSection[] sections = chunk.getSections();
        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            return true;
        }
        LevelChunkSection section = sections[sectionIndex];
        if (section.hasOnlyAir()) {
            return true;
        }
        for (Block block : senseBlocks) {
            if (section.maybeHas(state -> sameFamily(state, block))) {
                return false;
            }
        }
        return true;
    }

    /** A chest, cauldron, or flower pot matches the whole family, including what it holds. */
    private static boolean sameFamily(BlockState state, Block wanted) {
        Block found = state.getBlock();
        if (found == wanted) {
            return true;
        }
        if (wanted instanceof FlowerPotBlock && found instanceof FlowerPotBlock) {
            return true;
        }
        if (wanted instanceof AbstractChestBlock && found instanceof AbstractChestBlock) {
            return true;
        }
        if (wanted instanceof EnderChestBlock && found instanceof EnderChestBlock) {
            return true;
        }
        return wanted instanceof AbstractCauldronBlock && found instanceof AbstractCauldronBlock;
    }

    /** Inside that dose's radius, and within its air distance of air or water. */
    private static boolean matches(ClientLevel level, int x, int y, int z, BlockState state, long distanceSquared) {
        for (int i = 0; i < senseBlocks.length; i++) {
            if (distanceSquared <= senseRadiusSq[i]
                    && sameFamily(state, senseBlocks[i])
                    && exposed(level, x, y, z, senseBlocks[i])
                    && nearAir(level, x, y, z, senseAir[i])) {
                return true;
            }
        }
        return false;
    }

    /** A solid mass only needs its surface. Interior blocks would fill the cap and never be drawn. */
    private static boolean exposed(ClientLevel level, int x, int y, int z, Block wanted) {
        for (Direction direction : FACES) {
            AIR_CURSOR.set(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
            if (!sameFamily(level.getBlockState(AIR_CURSOR), wanted)) {
                return true;
            }
        }
        return false;
    }

    private static boolean nearAir(ClientLevel level, int x, int y, int z, int max) {
        for (int radius = 1; radius <= max; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dz = -radius; dz <= radius; dz++) {
                        if (Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz))) != radius) {
                            continue;
                        }
                        AIR_CURSOR.set(x + dx, y + dy, z + dz);
                        BlockState neighbor = level.getBlockState(AIR_CURSOR);
                        if (neighbor.isAir() || neighbor.getFluidState().is(FluidTags.WATER)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean readSection(ClientLevel level) {
        if (readingStale) {
            abandonRead();
            enqueue(readingKey);
            return true;
        }
        int sx = SectionPos.x(readingKey);
        int sy = SectionPos.y(readingKey);
        int sz = SectionPos.z(readingKey);
        LevelChunk chunk = level.getChunkSource().getChunkNow(sx, sz);
        if (chunk == null) {
            abandonRead();
            enqueue(readingKey);
            return true;
        }
        int sectionIndex = chunk.getSectionIndexFromSectionY(sy);
        LevelChunkSection[] sections = chunk.getSections();
        if (sectionIndex < 0 || sectionIndex >= sections.length) {
            commitRead();
            return true;
        }
        LevelChunkSection section = sections[sectionIndex];
        int baseX = sx << 4;
        int baseY = sy << 4;
        int baseZ = sz << 4;
        while (readingIndex < 4096 && budget > 0) {
            int local = readingIndex;
            int lx = local & 15;
            int lz = (local >> 4) & 15;
            int ly = (local >> 8) & 15;
            int x = baseX + lx;
            int y = baseY + ly;
            int z = baseZ + lz;
            long dx = (long) x - centerX;
            long dy = (long) y - centerY;
            long dz = (long) z - centerZ;
            long distanceSquared = dx * dx + dy * dy + dz * dz;
            if (distanceSquared > radiusSquared) {
                readingIndex++;
                continue;
            }
            budget--;
            if (!matches(level, x, y, z, section.getBlockState(lx, ly, lz), distanceSquared)) {
                readingIndex++;
                continue;
            }
            readingIndex++;
            long packed = BlockPos.asLong(x, y, z);
            boolean keep = VISIBLE.contains(packed) || VISIBLE.size() + admitted < MAX_BLOCKS;
            if (keep && SECTION_HITS.add(packed) && !VISIBLE.contains(packed)) {
                admitted++;
            }
        }
        if (readingStale) {
            abandonRead();
            enqueue(readingKey);
            return true;
        }
        if (readingIndex >= 4096) {
            commitRead();
        }
        return readingIndex >= 4096;
    }

    private static void commitRead() {
        boolean stable = !readingStale
                && centerX == readingCenterX
                && centerY == readingCenterY
                && centerZ == readingCenterZ;
        LongOpenHashSet previous = BY_SECTION.get(readingKey);
        if (previous != null) {
            LongIterator blocks = previous.iterator();
            while (blocks.hasNext()) {
                long packed = blocks.nextLong();
                if (SECTION_HITS.contains(packed)) {
                    continue;
                }
                if (stable || !inRange(packed)) {
                    blocks.remove();
                    if (VISIBLE.remove(packed)) {
                        dirty = true;
                    }
                }
            }
            if (previous.isEmpty()) {
                BY_SECTION.remove(readingKey);
            }
        }
        if (!SECTION_HITS.isEmpty()) {
            LongOpenHashSet hits = BY_SECTION.get(readingKey);
            if (hits == null) {
                hits = new LongOpenHashSet();
                BY_SECTION.put(readingKey, hits);
            }
            LongIterator blocks = SECTION_HITS.iterator();
            while (blocks.hasNext()) {
                long packed = blocks.nextLong();
                hits.add(packed);
                if (VISIBLE.add(packed)) {
                    dirty = true;
                }
            }
            SECTION_HITS.clear();
        }
        SCANNED.add(readingKey);
        readingSection = false;
        readingIndex = 0;
        admitted = 0;
    }

    private static void abandonRead() {
        SECTION_HITS.clear();
        readingSection = false;
        readingStale = false;
        readingIndex = 0;
        admitted = 0;
    }

    private static void invalidate(long key) {
        if (readingSection && key == readingKey) {
            readingStale = true;
        }
        SCANNED.remove(key);
        enqueue(key);
    }

    private static void enqueue(long key) {
        if (SCANNED.contains(key) || PENDING_SET.contains(key) || !intersects(key)) {
            return;
        }
        PENDING.add(key);
        PENDING_SET.add(key);
    }

    private static void forgetResults(long key) {
        LongOpenHashSet blocks = BY_SECTION.remove(key);
        if (blocks == null) {
            return;
        }
        LongIterator iterator = blocks.iterator();
        while (iterator.hasNext()) {
            if (VISIBLE.remove(iterator.nextLong())) {
                dirty = true;
            }
        }
    }

    private static boolean intersects(long key) {
        int minX = SectionPos.x(key) << 4;
        int minY = SectionPos.y(key) << 4;
        int minZ = SectionPos.z(key) << 4;
        long dx = (long) Math.clamp(centerX, minX, minX + 15) - centerX;
        long dy = (long) Math.clamp(centerY, minY, minY + 15) - centerY;
        long dz = (long) Math.clamp(centerZ, minZ, minZ + 15) - centerZ;
        return dx * dx + dy * dy + dz * dz <= radiusSquared;
    }

    private static void follow(ClientLevel level) {
        if (!applied) {
            appliedX = centerX;
            appliedY = centerY;
            appliedZ = centerZ;
            applied = true;
            return;
        }
        if (!sweeping && movedFromApplied()) {
            beginSweep();
        }
        while (sweeping && budget > 0) {
            if (!stepSweep(level)) {
                return;
            }
            appliedX = destX;
            appliedY = destY;
            appliedZ = destZ;
            sweeping = false;
            if (movedFromApplied()) {
                beginSweep();
            }
        }
    }

    private static boolean movedFromApplied() {
        return centerX != appliedX || centerY != appliedY || centerZ != appliedZ;
    }

    private static void beginSweep() {
        originX = appliedX;
        originY = appliedY;
        originZ = appliedZ;
        destX = centerX;
        destY = centerY;
        destZ = centerZ;
        cursorY = destY - radius;
        cursorReady = false;
        sweepSection = Long.MIN_VALUE;
        sweeping = true;
    }

    private static boolean stepSweep(ClientLevel level) {
        long radiusSq = (long) radius * radius;
        int yLimit = destY + radius;
        while (cursorY <= yLimit) {
            long dy = (long) cursorY - destY;
            long remaining = radiusSq - dy * dy;
            if (remaining < 0) {
                cursorY++;
                cursorReady = false;
                continue;
            }
            int reachX = (int) Math.sqrt(remaining);
            if (!cursorReady) {
                cursorX = destX - reachX;
                cursorZ = Integer.MIN_VALUE;
                cursorReady = true;
            }
            int xEnd = destX + reachX;
            while (cursorX <= xEnd) {
                long dx = (long) cursorX - destX;
                long row = remaining - dx * dx;
                if (row < 0) {
                    cursorX++;
                    cursorZ = Integer.MIN_VALUE;
                    continue;
                }
                int reachZ = (int) Math.sqrt(row);
                int zStart = destZ - reachZ;
                int zEnd = destZ + reachZ;
                if (cursorZ == Integer.MIN_VALUE) {
                    cursorZ = zStart;
                }
                long oldDx = (long) cursorX - originX;
                long oldDy = (long) cursorY - originY;
                long oldRemaining = radiusSq - oldDx * oldDx - oldDy * oldDy;
                int holeStart = 1;
                int holeEnd = 0;
                if (oldRemaining >= 0) {
                    int oldReach = (int) Math.sqrt(oldRemaining);
                    holeStart = originZ - oldReach;
                    holeEnd = originZ + oldReach;
                }
                while (cursorZ <= zEnd) {
                    if (cursorZ >= holeStart && cursorZ <= holeEnd) {
                        cursorZ = holeEnd + 1;
                        continue;
                    }
                    if (budget <= 0 || !accept(level, cursorX, cursorY, cursorZ)) {
                        return false;
                    }
                    cursorZ++;
                }
                cursorX++;
                cursorZ = Integer.MIN_VALUE;
            }
            cursorY++;
            cursorReady = false;
        }
        return true;
    }

    private static boolean accept(ClientLevel level, int x, int y, int z) {
        if (!inRange(x, y, z) || insideOrigin(x, y, z) || sectionMisses(level, x, y, z)) {
            return true;
        }
        if (budget <= 0) {
            return false;
        }
        CURSOR.set(x, y, z);
        if (!level.hasChunkAt(CURSOR)) {
            return true;
        }
        budget--;
        long dx = (long) x - centerX;
        long dy = (long) y - centerY;
        long dz = (long) z - centerZ;
        if (matches(level, x, y, z, level.getBlockState(CURSOR), dx * dx + dy * dy + dz * dz)) {
            admit(x, y, z);
        }
        return true;
    }

    private static boolean sectionMisses(ClientLevel level, int x, int y, int z) {
        long key = SectionPos.asLong(
                SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(y),
                SectionPos.blockToSectionCoord(z)
        );
        if (key != sweepSection) {
            sweepSection = key;
            LevelChunk chunk = level.getChunkSource().getChunkNow(SectionPos.x(key), SectionPos.z(key));
            sweepSectionEmpty = chunk == null || emptyOfTargets(chunk, key);
        }
        return sweepSectionEmpty;
    }

    private static void admit(int x, int y, int z) {
        long packed = BlockPos.asLong(x, y, z);
        if (VISIBLE.contains(packed) || VISIBLE.size() >= MAX_BLOCKS) {
            return;
        }
        long key = SectionPos.asLong(
                SectionPos.blockToSectionCoord(x),
                SectionPos.blockToSectionCoord(y),
                SectionPos.blockToSectionCoord(z)
        );
        LongOpenHashSet hits = BY_SECTION.get(key);
        if (hits == null) {
            hits = new LongOpenHashSet();
            BY_SECTION.put(key, hits);
        }
        hits.add(packed);
        if (readingSection && key == readingKey) {
            SECTION_HITS.add(packed);
        }
        if (VISIBLE.add(packed)) {
            dirty = true;
        }
    }

    private static void prune(ClientLevel level) {
        LongIterator blocks = VISIBLE.iterator();
        while (blocks.hasNext()) {
            long packed = blocks.nextLong();
            int x = BlockPos.getX(packed);
            int y = BlockPos.getY(packed);
            int z = BlockPos.getZ(packed);
            if (stillSensed(level, x, y, z)) {
                continue;
            }
            blocks.remove();
            dirty = true;
            long key = SectionPos.asLong(
                    SectionPos.blockToSectionCoord(BlockPos.getX(packed)),
                    SectionPos.blockToSectionCoord(BlockPos.getY(packed)),
                    SectionPos.blockToSectionCoord(BlockPos.getZ(packed))
            );
            LongOpenHashSet hits = BY_SECTION.get(key);
            if (hits != null && hits.remove(packed) && hits.isEmpty()) {
                BY_SECTION.remove(key);
            }
        }
    }

    /** Drop outlines that left the radius or changed block. Do not rerun the air search while walking. */
    private static boolean stillSensed(ClientLevel level, int x, int y, int z) {
        long dx = (long) x - centerX;
        long dy = (long) y - centerY;
        long dz = (long) z - centerZ;
        long distanceSquared = dx * dx + dy * dy + dz * dz;
        if (distanceSquared > radiusSquared) {
            return false;
        }
        CURSOR.set(x, y, z);
        if (!level.hasChunkAt(CURSOR)) {
            return true;
        }
        BlockState state = level.getBlockState(CURSOR);
        for (int i = 0; i < senseBlocks.length; i++) {
            if (distanceSquared <= senseRadiusSq[i] && sameFamily(state, senseBlocks[i])) {
                return true;
            }
        }
        return false;
    }

    private static boolean inRange(long packed) {
        return inRange(BlockPos.getX(packed), BlockPos.getY(packed), BlockPos.getZ(packed));
    }

    private static boolean inRange(int x, int y, int z) {
        long dx = (long) x - centerX;
        long dy = (long) y - centerY;
        long dz = (long) z - centerZ;
        return dx * dx + dy * dy + dz * dz <= radiusSquared;
    }

    private static boolean insideOrigin(int x, int y, int z) {
        long dx = (long) x - originX;
        long dy = (long) y - originY;
        long dz = (long) z - originZ;
        return dx * dx + dy * dy + dz * dz <= (long) radius * radius;
    }

    private static void publish() {
        if (VISIBLE.isEmpty()) {
            renderBlocks = EMPTY;
            dirty = false;
            return;
        }
        long[] next = new long[VISIBLE.size()];
        int count = 0;
        LongIterator blocks = VISIBLE.iterator();
        while (blocks.hasNext()) {
            long packed = blocks.nextLong();
            long dx = (long) BlockPos.getX(packed) - centerX;
            long dy = (long) BlockPos.getY(packed) - centerY;
            long dz = (long) BlockPos.getZ(packed) - centerZ;
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) {
                next[count++] = packed;
            }
        }
        if (count == next.length) {
            renderBlocks = next;
        } else if (count == 0) {
            renderBlocks = EMPTY;
        } else {
            long[] trimmed = new long[count];
            System.arraycopy(next, 0, trimmed, 0, count);
            renderBlocks = trimmed;
        }
        dirty = false;
    }
}
