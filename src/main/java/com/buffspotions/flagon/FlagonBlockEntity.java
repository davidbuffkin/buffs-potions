package com.buffspotions.flagon;

import com.buffspotions.registry.ModBlockEntities;
import com.buffspotions.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FlagonBlockEntity extends BlockEntity implements FlagonVessel {
    private FlagonContents contents = FlagonContents.EMPTY;

    public FlagonBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLAGON.get(), pos, state);
    }

    public FlagonContents contents() {
        return contents;
    }

    public void setContents(FlagonContents contents) {
        boolean changed = changed(this.contents, contents);
        this.contents = contents;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            if (changed) {
                FlagonInteractions.pulseObservers(level, worldPosition);
            }
        }
    }

    private static boolean changed(FlagonContents before, FlagonContents after) {
        if (before.doses() != after.doses()) {
            return true;
        }
        if (before.isEmpty() && after.isEmpty()) {
            return false;
        }
        return !ItemStack.isSameItemSameComponents(before.dose(), after.dose());
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!contents.isEmpty()) {
            output.store("Contents", FlagonContents.CODEC, contents);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        contents = input.read("Contents", FlagonContents.CODEC).orElse(FlagonContents.EMPTY);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        contents = components.getOrDefault(ModDataComponents.FLAGON_CONTENTS.get(), FlagonContents.EMPTY);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(ModDataComponents.FLAGON_CONTENTS.get(), contents);
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {
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
