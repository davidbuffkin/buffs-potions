package com.buffspotions.bellows;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public final class GolemBellowsJob {
    public static final Codec<GolemBellowsJob> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.optionalFieldOf("anchor").forGetter(job -> Optional.ofNullable(job.anchor)),
            BlockPos.CODEC.optionalFieldOf("target").forGetter(job -> Optional.ofNullable(job.target)),
            BlockPos.CODEC.optionalFieldOf("stand").forGetter(job -> Optional.ofNullable(job.stand)),
            Codec.LONG.optionalFieldOf("next_use", 0L).forGetter(job -> job.nextUse),
            Codec.BOOL.optionalFieldOf("stoked", false).forGetter(job -> job.stoked),
            Codec.BOOL.optionalFieldOf("given", false).forGetter(job -> job.givenByPlayer)
    ).apply(instance, GolemBellowsJob::from));

    BlockPos anchor;
    BlockPos target;
    BlockPos stand;
    long nextUse;
    boolean stoked;
    boolean givenByPlayer;
    final Set<BlockPos> unreachable = new HashSet<>();
    long unreachableResetAt;
    long nextSearch;
    long nextWander;
    boolean controlling;
    /** Game time the current target stopped looking busy. Zero while it is busy. */
    long quietSince;

    public GolemBellowsJob() {
    }

    private static GolemBellowsJob from(
            Optional<BlockPos> anchor,
            Optional<BlockPos> target,
            Optional<BlockPos> stand,
            long nextUse,
            boolean stoked,
            boolean givenByPlayer
    ) {
        GolemBellowsJob job = new GolemBellowsJob();
        job.anchor = anchor.orElse(null);
        job.target = target.orElse(null);
        job.stand = stand.orElse(null);
        job.nextUse = nextUse;
        job.stoked = stoked;
        job.givenByPlayer = givenByPlayer;
        return job;
    }

    void clearTask() {
        target = null;
        stand = null;
        stoked = false;
        nextUse = 0;
        quietSince = 0;
    }

    void release() {
        anchor = null;
        clearTask();
        unreachable.clear();
        controlling = false;
        givenByPlayer = false;
    }
}
