package com.buffspotions.bellows;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BellowsClaims {
    private static final Map<ResourceKey<Level>, Map<BlockPos, UUID>> CLAIMS = new HashMap<>();

    private BellowsClaims() {
    }

    public static boolean claimedByOther(ServerLevel level, BlockPos pos, UUID self) {
        UUID owner = owner(level, pos);
        return owner != null && !owner.equals(self);
    }

    public static boolean owns(ServerLevel level, BlockPos pos, UUID self) {
        return self.equals(owner(level, pos));
    }

    private static UUID owner(ServerLevel level, BlockPos pos) {
        Map<BlockPos, UUID> claims = CLAIMS.get(level.dimension());
        return claims == null ? null : claims.get(pos);
    }

    public static void claim(ServerLevel level, BlockPos pos, UUID self) {
        CLAIMS.computeIfAbsent(level.dimension(), key -> new HashMap<>()).put(pos.immutable(), self);
    }

    public static void release(ServerLevel level, BlockPos pos, UUID self) {
        Map<BlockPos, UUID> claims = CLAIMS.get(level.dimension());
        if (claims != null) {
            claims.remove(pos, self);
        }
    }

    public static void releaseAll(ServerLevel level, UUID self) {
        Map<BlockPos, UUID> claims = CLAIMS.get(level.dimension());
        if (claims != null) {
            claims.entrySet().removeIf(entry -> self.equals(entry.getValue()));
        }
    }
}
