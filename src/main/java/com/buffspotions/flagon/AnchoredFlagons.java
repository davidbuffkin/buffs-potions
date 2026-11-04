package com.buffspotions.flagon;

import com.buffspotions.BuffsPotions;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import org.jspecify.annotations.Nullable;

/** Placed anchored flagons, and the one-chunk tickets that keep them loaded. */
public final class AnchoredFlagons {
    public static final TicketController TICKETS = new TicketController(
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "anchored_flagon"),
            (level, helper) -> {
                for (BlockPos pos : Set.copyOf(helper.getBlockTickets().keySet())) {
                    if (level.isLoaded(pos) && !(level.getBlockState(pos).getBlock() instanceof AnchoredEnderFlagonBlock)) {
                        helper.removeAllTickets(pos);
                    }
                }
            }
    );

    private static final Set<AnchoredEnderFlagonBlockEntity> LOADED = new CopyOnWriteArraySet<>();

    private AnchoredFlagons() {
    }

    public static void registerTickets(RegisterTicketControllersEvent event) {
        event.register(TICKETS);
    }

    public static void track(AnchoredEnderFlagonBlockEntity flagon) {
        LOADED.add(flagon);
    }

    public static void untrack(AnchoredEnderFlagonBlockEntity flagon) {
        LOADED.remove(flagon);
    }

    public static void force(ServerLevel level, BlockPos pos, boolean add) {
        ChunkPos chunk = ChunkPos.containing(pos);
        TICKETS.forceChunk(level, pos, chunk.x(), chunk.z(), add, false);
    }

    public static void refreshNames(UUID player, String name) {
        for (AnchoredEnderFlagonBlockEntity flagon : LOADED) {
            if (player.equals(flagon.playerId())) {
                flagon.applyName(name);
            }
        }
    }

    /**
     * The nearest placed flagon in {@code level} whose anchor is {@code player}.
     * Distance is unlimited. Flagons anchored to anyone else are ignored.
     */
    public static @Nullable BlockPos closest(UUID player, ServerLevel level, Vec3 from) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (AnchoredEnderFlagonBlockEntity flagon : LOADED) {
            if (flagon.isRemoved() || !player.equals(flagon.playerId()) || flagon.getLevel() != level) {
                continue;
            }
            BlockPos pos = flagon.getBlockPos();
            double distance = from.distanceToSqr(Vec3.atCenterOf(pos));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }
        return best;
    }

    public static void refresh(MinecraftServer server, UUID player, FlagonContents contents) {
        for (AnchoredEnderFlagonBlockEntity flagon : LOADED) {
            if (player.equals(flagon.playerId()) && flagon.getLevel() instanceof ServerLevel) {
                flagon.applySynced(contents);
            }
        }
    }
}
