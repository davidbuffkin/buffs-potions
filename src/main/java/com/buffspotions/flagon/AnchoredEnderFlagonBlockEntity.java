package com.buffspotions.flagon;

import com.buffspotions.registry.ModBlockEntities;
import com.buffspotions.registry.ModDataComponents;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** A placed anchored flagon. The liquid belongs to {@link #anchor}, and every client can see it. */
public final class AnchoredEnderFlagonBlockEntity extends BlockEntity implements FlagonVessel {
    private @Nullable FlagonAnchor anchor;
    private FlagonContents contents = FlagonContents.EMPTY;

    public AnchoredEnderFlagonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANCHORED_ENDER_FLAGON.get(), pos, state);
    }

    public @Nullable FlagonAnchor anchor() {
        return anchor;
    }

    public @Nullable UUID playerId() {
        return anchor == null ? null : anchor.player();
    }

    @Override
    public FlagonContents contents() {
        return contents;
    }

    @Override
    public void setContents(FlagonContents contents) {
        if (anchor != null && level instanceof ServerLevel server) {
            EnderFlagons.set(server.getServer(), anchor.player(), contents);
            return;
        }
        applySynced(contents);
    }

    public void applySynced(FlagonContents contents) {
        if (this.contents.sameFill(contents)) {
            return;
        }
        this.contents = contents;
        setChanged();
        if (level == null) {
            return;
        }
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        FlagonInteractions.pulseObservers(level, worldPosition);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        AnchoredFlagons.track(this);
        AnchoredFlagons.force(server, worldPosition, true);
        if (anchor != null) {
            applyName(EnderFlagons.knownName(server.getServer(), anchor.player()));
            FlagonContents live = EnderFlagons.contents(server.getServer(), anchor.player());
            if (!contents.sameFill(live)) {
                applySynced(live);
            }
        }
    }

    public void applyName(String name) {
        if (anchor == null || name.equals(anchor.name())) {
            return;
        }
        anchor = new FlagonAnchor(anchor.player(), name);
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setRemoved() {
        AnchoredFlagons.untrack(this);
        super.setRemoved();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel server) {
            AnchoredFlagons.force(server, pos, false);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (anchor != null) {
            output.store("Anchor", FlagonAnchor.CODEC, anchor);
        }
        if (!contents.isEmpty()) {
            output.store("Contents", FlagonContents.CODEC, contents);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        anchor = input.read("Anchor", FlagonAnchor.CODEC).orElse(null);
        contents = input.read("Contents", FlagonContents.CODEC).orElse(FlagonContents.EMPTY);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        FlagonAnchor stored = components.get(ModDataComponents.FLAGON_ANCHOR.get());
        if (stored != null) {
            anchor = stored;
        }
        FlagonContents storedContents = components.get(ModDataComponents.FLAGON_CONTENTS.get());
        if (storedContents != null) {
            contents = storedContents;
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (anchor != null) {
            components.set(ModDataComponents.FLAGON_ANCHOR.get(), anchor);
        }
        components.set(ModDataComponents.FLAGON_CONTENTS.get(), contents);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
        output.discard("Anchor");
        output.discard("Contents");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }
}
