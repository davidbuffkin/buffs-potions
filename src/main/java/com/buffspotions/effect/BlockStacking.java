package com.buffspotions.effect;

import com.buffspotions.advancement.StacksOnStacks;
import com.buffspotions.registry.ModEffects;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Places a pillar of the placed block away from the clicked face, one block per tick.
 * The block the player just placed counts toward the height. Any block works.
 * Air and water do not stop the pillar.
 */
public final class BlockStacking {
    private static final List<Job> JOBS = new ArrayList<>();
    private static final java.util.Map<UUID, Click> CLICKS = new java.util.HashMap<>();

    private BlockStacking() {
    }

    public static void rememberClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || event.getFace() == null) {
            return;
        }
        Player player = event.getEntity();
        if (player.isShiftKeyDown() || !player.hasEffect(ModEffects.BLOCK_STACKED)) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof BlockItem blockItem)) {
            return;
        }
        if (amplifier(player) < 0) {
            return;
        }
        CLICKS.put(player.getUUID(), new Click(event.getPos().immutable(), event.getFace(), event.getLevel().getGameTime()));
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer placing) {
            StacksOnStacks.note(placing, 1);
        }
        if (event instanceof BlockEvent.EntityMultiPlaceEvent || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (player.isShiftKeyDown() || !player.hasEffect(ModEffects.BLOCK_STACKED)) {
            return;
        }
        BlockState placed = event.getPlacedBlock();
        int amplifier = amplifier(player);
        if (amplifier < 0) {
            return;
        }
        Click click = CLICKS.get(player.getUUID());
        if (click == null || click.gameTime != level.getGameTime()) {
            return;
        }
        BlockPos expected = click.pos.relative(click.face);
        if (!event.getPos().equals(expected) && !event.getPos().equals(click.pos)) {
            return;
        }
        int extra = EyedTuning.pillarHeight(amplifier) - 1;
        if (extra <= 0) {
            return;
        }
        Item item = placed.getBlock().asItem();
        if (item == net.minecraft.world.item.Items.AIR) {
            return;
        }
        JOBS.add(new Job(
                player.getUUID(),
                level.dimension(),
                event.getPos().relative(click.face),
                click.face,
                extra,
                placed,
                item
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
            if (!player.hasEffect(ModEffects.BLOCK_STACKED)) {
                jobs.remove();
                continue;
            }
            if (!(player.level() instanceof ServerLevel level) || !step(player, level, job)) {
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

    private static boolean step(ServerPlayer player, ServerLevel level, Job job) {
        if (job.remaining <= 0 || level.isOutsideBuildHeight(job.next) || !level.isLoaded(job.next)) {
            return false;
        }
        if (!continuesThrough(level.getBlockState(job.next)) || blockedByEntity(level, job.next, job.state)) {
            return false;
        }
        if (!job.state.canSurvive(level, job.next)) {
            return false;
        }
        if (!consume(player, job.item)) {
            return false;
        }
        if (!level.setBlock(job.next, job.state, Block.UPDATE_ALL)) {
            return false;
        }
        player.awardStat(Stats.ITEM_USED.get(job.item));
        CriteriaTriggers.PLACED_BLOCK.trigger(player, job.next, new ItemStack(job.item));
        level.gameEvent(GameEvent.BLOCK_PLACE, job.next, GameEvent.Context.of(player, job.state));
        StacksOnStacks.note(player, 1);
        SoundType soundType = job.state.getSoundType(level, job.next, player);
        CrowdTuning.beginPlayerSound(player);
        try {
            level.playSound(
                    null,
                    job.next,
                    soundType.getPlaceSound(),
                    SoundSource.BLOCKS,
                    (soundType.getVolume() + 1.0F) / 2.0F,
                    soundType.getPitch() * 0.8F
            );
        } finally {
            CrowdTuning.endPlayerSound(player);
        }
        job.next = job.next.relative(job.direction);
        job.remaining--;
        return job.remaining > 0;
    }

    /** The placed block is the one being stacked. Any block works while Stacked is active. */
    private static int amplifier(Player player) {
        MobEffectInstance effect = player.getEffect(ModEffects.BLOCK_STACKED);
        return effect == null ? -1 : effect.getAmplifier();
    }

    /** Air and water are open. Source and flowing water both count. Any other block stops the pillar. */
    public static boolean continuesThrough(BlockState state) {
        return state.isAir() || state.is(Blocks.WATER);
    }

    /** Players, mobs, boats, and other entities that block building stop the pillar. */
    private static boolean blockedByEntity(ServerLevel level, BlockPos pos, BlockState state) {
        VoxelShape shape = state.getCollisionShape(level, pos, CollisionContext.empty());
        if (shape.isEmpty()) {
            shape = Shapes.block();
        }
        return !level.isUnobstructed(null, shape.move(pos));
    }

    private static boolean consume(ServerPlayer player, Item item) {
        if (player.hasInfiniteMaterials()) {
            return true;
        }
        Inventory inventory = player.getInventory();
        if (take(inventory, inventory.getSelectedSlot(), item)) {
            return true;
        }
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (slot != inventory.getSelectedSlot() && take(inventory, slot, item)) {
                return true;
            }
        }
        return take(inventory, Inventory.SLOT_OFFHAND, item);
    }

    private static boolean take(Inventory inventory, int slot, Item item) {
        ItemStack stack = inventory.getItem(slot);
        if (!stack.is(item)) {
            return false;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            inventory.setItem(slot, ItemStack.EMPTY);
        }
        return true;
    }

    private record Click(BlockPos pos, Direction face, long gameTime) {
    }

    private static final class Job {
        private final UUID player;
        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
        private BlockPos next;
        private final Direction direction;
        private int remaining;
        private final BlockState state;
        private final Item item;

        private Job(
                UUID player,
                net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                BlockPos next,
                Direction direction,
                int remaining,
                BlockState state,
                Item item
        ) {
            this.player = player;
            this.dimension = dimension;
            this.next = next;
            this.direction = direction;
            this.remaining = remaining;
            this.state = state;
            this.item = item;
        }
    }
}
