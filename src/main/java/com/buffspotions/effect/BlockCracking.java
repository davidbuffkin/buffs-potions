package com.buffspotions.effect;

import com.buffspotions.registry.ModEffects;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Breaks a hole into the face that was mined. The mined block counts toward the depth.
 * Any block can start a hole. Later blocks must be that same block. Air and every other block stop it.
 * Mining continues one block per tick. Instant blocks that drop without a tool crack with
 * any hand, including an empty one. Every other block continues only while the same hotbar
 * slot still holds the tool that started the crack. Creative mode ignores the held item.
 */
public final class BlockCracking {
    private static final List<Job> JOBS = new ArrayList<>();
    private static final java.util.Map<UUID, List<Click>> CLICKS = new java.util.HashMap<>();
    private static final ThreadLocal<Boolean> AUTO = ThreadLocal.withInitial(() -> false);

    private BlockCracking() {
    }

    public static void rememberClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide() || event.getFace() == null) {
            return;
        }
        Player player = event.getEntity();
        if (player.isShiftKeyDown() || !player.hasEffect(ModEffects.BLOCK_CRACKED)) {
            return;
        }
        List<Click> clicks = CLICKS.computeIfAbsent(player.getUUID(), id -> new ArrayList<>());
        BlockPos pos = event.getPos().immutable();
        clicks.removeIf(click -> click.pos.equals(pos));
        clicks.add(new Click(pos, event.getFace()));
        if (clicks.size() > 16) {
            clicks.removeFirst();
        }
    }

    public static void onBreak(BreakBlockEvent event) {
        if (AUTO.get() || event.getLevel().isClientSide() || event.isCanceled()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (player.isShiftKeyDown() || !player.hasEffect(ModEffects.BLOCK_CRACKED)) {
            return;
        }
        BlockState state = event.getState();
        int amplifier = amplifier(player);
        if (amplifier < 0) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!player.isCreative() && !correctTool(held, state)) {
            return;
        }
        List<Click> clicks = CLICKS.get(player.getUUID());
        if (clicks == null) {
            return;
        }
        Click click = null;
        for (int i = clicks.size() - 1; i >= 0; i--) {
            if (clicks.get(i).pos.equals(event.getPos())) {
                click = clicks.remove(i);
                break;
            }
        }
        if (click == null) {
            return;
        }
        exhaustForCrack(player, level, event.getPos(), state);
        int extra = EyedTuning.crackDepth(amplifier) - 1;
        if (extra <= 0) {
            return;
        }
        JOBS.add(new Job(
                player.getUUID(),
                level.dimension(),
                event.getPos().relative(click.face.getOpposite()),
                click.face.getOpposite(),
                extra,
                state.getBlock(),
                player.getInventory().getSelectedSlot(),
                held.getItem()
        ));
    }

    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) {
            return;
        }
        Iterator<Job> jobs = JOBS.iterator();
        while (jobs.hasNext()) {
            Job job = jobs.next();
            ServerPlayer player = server.getPlayerList().getPlayer(job.player);
            if (player == null || !player.isAlive() || player.level().dimension() != job.dimension) {
                jobs.remove();
                continue;
            }
            if (!player.hasEffect(ModEffects.BLOCK_CRACKED) || !(player.level() instanceof ServerLevel level)) {
                jobs.remove();
                continue;
            }
            if (!step(player, level, job)) {
                jobs.remove();
            }
        }
    }

    public static void forget(Player player) {
        CLICKS.remove(player.getUUID());
        JOBS.removeIf(job -> job.player.equals(player.getUUID()));
    }

    public static void forgetAll() {
        CLICKS.clear();
        JOBS.clear();
    }

    /** Later breaks in a hole have to be the block that started it. Air stops the hole. */
    static boolean sameCrack(BlockState next, Block started) {
        return next.getBlock() == started;
    }

    /** The mined block is the one being cracked. Any block works while Cracked is active. */
    private static int amplifier(Player player) {
        MobEffectInstance effect = player.getEffect(ModEffects.BLOCK_CRACKED);
        return effect == null ? -1 : effect.getAmplifier();
    }

    /** A harvested break costs 0.005 exhaustion. Cracking charges that amount again. */
    private static void exhaustForCrack(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!player.preventsBlockDrops() && state.canHarvestBlock(level, pos, player)) {
            player.causeFoodExhaustion(0.005F);
        }
    }

    /** No tool is required when the block drops anyway and breaks instantly in an empty hand. */
    private static boolean breaksByHand(BlockState state) {
        return !state.requiresCorrectToolForDrops() && state.getDestroySpeed(null, BlockPos.ZERO) == 0.0F;
    }

    private static boolean correctTool(ItemStack stack, BlockState state) {
        if (breaksByHand(state)) {
            return true;
        }
        if (stack.isEmpty()) {
            return false;
        }
        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null) {
            return false;
        }
        return tool.getMiningSpeed(state) > tool.defaultMiningSpeed() || tool.isCorrectForDrops(state);
    }

    private static boolean holdingTool(ServerPlayer player, Job job) {
        Inventory inventory = player.getInventory();
        if (inventory.getSelectedSlot() != job.slot) {
            return false;
        }
        return inventory.getItem(job.slot).is(job.tool);
    }

    private static boolean step(ServerPlayer player, ServerLevel level, Job job) {
        if (job.remaining <= 0 || level.isOutsideBuildHeight(job.next) || !level.isLoaded(job.next)) {
            return false;
        }
        BlockState state = level.getBlockState(job.next);
        if (!sameCrack(state, job.block)) {
            return false;
        }
        if (!player.isCreative()
                && !breaksByHand(state)
                && (!holdingTool(player, job) || !correctTool(player.getMainHandItem(), state))) {
            return false;
        }
        BlockPos broken = job.next;
        BlockState brokenState = state;
        boolean minedStat = !player.preventsBlockDrops() && brokenState.canHarvestBlock(level, broken, player);
        AUTO.set(true);
        boolean broke;
        try {
            broke = player.gameMode.destroyBlock(broken);
        } finally {
            AUTO.set(false);
        }
        if (!broke) {
            return false;
        }
        if (minedStat) {
            exhaustForCrack(player, level, broken, brokenState);
        }
        if (!minedStat) {
            player.awardStat(Stats.BLOCK_MINED.get(brokenState.getBlock()));
        }
        player.connection.send(new ClientboundLevelEventPacket(2001, broken, Block.getId(brokenState), false));
        job.next = job.next.relative(job.direction);
        job.remaining--;
        return job.remaining > 0;
    }

    private record Click(BlockPos pos, Direction face) {
    }

    private static final class Job {
        private final UUID player;
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        private BlockPos next;
        private final Direction direction;
        private int remaining;
        private final Block block;
        private final int slot;
        private final Item tool;

        private Job(
                UUID player,
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                BlockPos next,
                Direction direction,
                int remaining,
                Block block,
                int slot,
                Item tool
        ) {
            this.player = player;
            this.dimension = dimension;
            this.next = next;
            this.direction = direction;
            this.remaining = remaining;
            this.block = block;
            this.slot = slot;
            this.tool = tool;
        }
    }
}
