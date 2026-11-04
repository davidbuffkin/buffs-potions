package com.buffspotions.belt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class BeltUseState {
    public static final Codec<BeltUseState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("drinking").forGetter(BeltUseState::isDrinking),
            Codec.INT.fieldOf("slot").forGetter(BeltUseState::slot),
            Codec.INT.fieldOf("elapsed").forGetter(BeltUseState::elapsedTicks),
            Codec.BOOL.optionalFieldOf("advance", false).forGetter(BeltUseState::pendingAdvance),
            Codec.BOOL.optionalFieldOf("gulp", false).forGetter(BeltUseState::gulp),
            Codec.INT.optionalFieldOf("advance_belt", 0).forGetter(BeltUseState::advanceBelt),
            Codec.INT.optionalFieldOf("next_advance", 1).forGetter(BeltUseState::nextAdvance),
            Codec.INT.optionalFieldOf("inventory_slot", -1).forGetter(BeltUseState::inventorySlot)
    ).apply(instance, BeltUseState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, BeltUseState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            BeltUseState::isDrinking,
            ByteBufCodecs.VAR_INT,
            BeltUseState::slot,
            ByteBufCodecs.VAR_INT,
            BeltUseState::elapsedTicks,
            ByteBufCodecs.BOOL,
            BeltUseState::pendingAdvance,
            ByteBufCodecs.BOOL,
            BeltUseState::gulp,
            ByteBufCodecs.VAR_INT,
            BeltUseState::advanceBelt,
            ByteBufCodecs.VAR_INT,
            BeltUseState::nextAdvance,
            ByteBufCodecs.VAR_INT,
            BeltUseState::inventorySlot,
            BeltUseState::new
    );

    private boolean drinking;
    private int slot = -1;
    private int elapsedTicks;
    private boolean pendingAdvance;
    private boolean gulp;
    private int advanceBelt;
    private int nextAdvance = 1;
    /** Inventory index of a gulp belt. {@code -1} is the worn belt. {@code -2} is the off hand. */
    private int inventorySlot = -1;

    public BeltUseState() {
    }

    public BeltUseState(
            boolean drinking,
            int slot,
            int elapsedTicks,
            boolean pendingAdvance,
            boolean gulp,
            int advanceBelt,
            int nextAdvance,
            int inventorySlot
    ) {
        this.drinking = drinking;
        this.slot = slot;
        this.elapsedTicks = elapsedTicks;
        this.pendingAdvance = pendingAdvance;
        this.gulp = gulp;
        this.advanceBelt = advanceBelt;
        this.nextAdvance = Math.max(1, nextAdvance);
        this.inventorySlot = inventorySlot;
    }

    public boolean isDrinking() {
        return drinking;
    }

    public int slot() {
        return slot;
    }

    public int elapsedTicks() {
        return elapsedTicks;
    }

    public boolean pendingAdvance() {
        return pendingAdvance;
    }

    public boolean gulp() {
        return gulp;
    }

    public int advanceBelt() {
        return advanceBelt;
    }

    public int nextAdvance() {
        return nextAdvance;
    }

    public int inventorySlot() {
        return inventorySlot;
    }

    public void start(int slot) {
        this.drinking = true;
        this.slot = slot;
        this.elapsedTicks = 0;
        this.gulp = false;
    }

    public void startGulp(int slot, int inventorySlot) {
        start(slot);
        this.gulp = true;
        this.inventorySlot = inventorySlot;
    }

    public int tick() {
        return ++elapsedTicks;
    }

    public void requestAdvance() {
        pendingAdvance = true;
    }

    public int markAdvance() {
        pendingAdvance = true;
        advanceBelt = nextAdvance;
        nextAdvance = nextAdvance == Integer.MAX_VALUE ? 1 : nextAdvance + 1;
        return advanceBelt;
    }

    public void clearAdvance() {
        pendingAdvance = false;
        advanceBelt = 0;
    }

    public void cancel() {
        drinking = false;
        slot = -1;
        elapsedTicks = 0;
        gulp = false;
        inventorySlot = -1;
    }
}
