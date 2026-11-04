package com.buffspotions.shrieker;

import com.buffspotions.BuffsPotions;
import com.buffspotions.network.ShriekerOpenPayload;
import com.buffspotions.registry.ModBlockEntities;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

public final class CalibratedSculkShriekerBlockEntity extends BlockEntity {
    public static final int COOLDOWN = 40;
    public static final Identifier STOP_LISTENING =
            Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "shrieker_stop");
    private static final String LISTENING = "block.buffs_potions.calibrated_sculk_shrieker.listening";
    private static final String STOPPED = "block.buffs_potions.calibrated_sculk_shrieker.stopped";
    private static final String STOP_HOVER = "block.buffs_potions.calibrated_sculk_shrieker.stop_hover";

    private String message = "";
    private boolean powered;
    /** Game time at which another rising edge may send. */
    private long readyAt;
    private final Set<UUID> listeners = new LinkedHashSet<>();

    public CalibratedSculkShriekerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CALIBRATED_SCULK_SHRIEKER.get(), pos, state);
    }

    public void open(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ShriekerOpenPayload(this.worldPosition, this.message));
    }

    public void saveMessage(ServerPlayer player, String text) {
        this.message = ShriekerText.clean(text);
        this.listeners.add(player.getUUID());
        this.setChanged();
        this.tellListening(player);
    }

    public void toggleListening(ServerPlayer player) {
        if (this.listeners.remove(player.getUUID())) {
            this.setChanged();
            player.sendSystemMessage(Component.translatable(STOPPED));
            return;
        }
        this.listeners.add(player.getUUID());
        this.setChanged();
        this.tellListening(player);
    }

    public void onSignal(ServerLevel level, boolean signal) {
        boolean send = shouldSend(this.powered, signal, level.getGameTime(), this.readyAt);
        boolean changed = signal != this.powered;
        this.powered = signal;
        if (send) {
            this.readyAt = level.getGameTime() + COOLDOWN;
            this.broadcast(level);
            changed = true;
        }
        if (changed) {
            this.setChanged();
        }
    }

    static boolean shouldSend(boolean powered, boolean signal, long gameTime, long readyAt) {
        return signal && !powered && gameTime >= readyAt;
    }

    private void broadcast(ServerLevel level) {
        Component shown = Component.empty()
                .append(ShriekerText.parse(this.message))
                .withStyle(style -> style
                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable(STOP_HOVER)))
                        .withClickEvent(new ClickEvent.Custom(STOP_LISTENING, Optional.of(locationTag(level)))));
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (this.listeners.contains(player.getUUID())) {
                player.sendSystemMessage(shown);
            }
        }
    }

    private CompoundTag locationTag(ServerLevel level) {
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", level.dimension().identifier().toString());
        tag.putInt("x", this.worldPosition.getX());
        tag.putInt("y", this.worldPosition.getY());
        tag.putInt("z", this.worldPosition.getZ());
        return tag;
    }

    private void tellListening(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable(LISTENING, ShriekerText.parse(this.message)));
    }

    public static void stopFromClick(ServerPlayer player, @Nullable Tag payload) {
        if (!(payload instanceof CompoundTag tag)) {
            return;
        }
        String dimension = tag.getString("dimension").orElse("");
        Identifier id;
        try {
            id = Identifier.parse(dimension);
        } catch (RuntimeException ignored) {
            return;
        }
        var x = tag.getInt("x");
        var y = tag.getInt("y");
        var z = tag.getInt("z");
        if (x.isEmpty() || y.isEmpty() || z.isEmpty()) {
            return;
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        ServerLevel level = player.level().getServer().getLevel(key);
        if (level != null
                && level.getBlockEntity(new BlockPos(x.get(), y.get(), z.get())) instanceof CalibratedSculkShriekerBlockEntity shrieker
                && shrieker.listeners.remove(player.getUUID())) {
            shrieker.setChanged();
        }
        player.sendSystemMessage(Component.translatable(STOPPED));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Message", this.message);
        output.putBoolean("Powered", this.powered);
        output.putLong("ReadyAt", this.readyAt);
        if (!this.listeners.isEmpty()) {
            var list = output.list("Listeners", UUIDUtil.CODEC);
            this.listeners.forEach(list::add);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.message = input.getStringOr("Message", "");
        this.powered = input.getBooleanOr("Powered", false);
        this.readyAt = input.getLongOr("ReadyAt", 0L);
        this.listeners.clear();
        input.listOrEmpty("Listeners", UUIDUtil.CODEC).forEach(this.listeners::add);
    }
}
