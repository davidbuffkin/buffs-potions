package com.buffspotions.effect;

import com.buffspotions.registry.ModEffects;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Moves matching items into the nearest chest a copper golem would use,
 * or throws them one at a time when no chest is in range.
 */
public final class ItemVomiting {
    private static final int BURST = 64;
    private static final Map<String, Long> NEXT = new HashMap<>();

    private ItemVomiting() {
    }

    public static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.hasEffect(ModEffects.ITEM_VOMITING) || !(player.level() instanceof ServerLevel level)) {
                forget(player);
                continue;
            }
            long now = level.getGameTime();
            for (ParameterDose dose : ParameterDoses.matching(player, ModEffects.ITEM_VOMITING)) {
                if (dose.subject().item().isEmpty()) {
                    continue;
                }
                TrackedItem tracked = TrackedItem.of(dose.subject().item().get());
                if (!hasSomethingToLose(player, tracked)) {
                    continue;
                }
                String key = player.getUUID() + "/" + dose.subject().item().get().unwrapKey()
                        .map(id -> id.identifier().toString())
                        .orElse("");
                Long next = NEXT.get(key);
                if (next != null && now < next) {
                    continue;
                }
                int amplifier = dose.amplifier();
                ChestBlockEntity chest = nearestChest(level, player, tracked, EyedTuning.vomitRange(amplifier));
                if (chest != null && moveInto(player, tracked, chest, BURST) > 0) {
                    NEXT.put(key, now + EyedTuning.vomitDepositInterval(amplifier));
                } else if (throwOne(player, tracked)) {
                    NEXT.put(key, now + EyedTuning.vomitThrowInterval(amplifier));
                }
            }
        }
    }

    public static void forget(Player player) {
        String prefix = player.getUUID() + "/";
        NEXT.keySet().removeIf(key -> key.equals(player.getUUID().toString()) || key.startsWith(prefix));
    }

    public static void forgetAll() {
        NEXT.clear();
    }

    private static boolean hasSomethingToLose(Player player, TrackedItem tracked) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (vomitable(inventory.getItem(slot), tracked)) {
                return true;
            }
        }
        return vomitable(player.getOffhandItem(), tracked);
    }

    private static boolean vomitable(ItemStack stack, TrackedItem tracked) {
        return !stack.isEmpty()
                && !stack.has(DataComponents.CUSTOM_NAME)
                && tracked.item().isPresent()
                && stack.is(tracked.item().get());
    }

    private static ChestBlockEntity nearestChest(ServerLevel level, Player player, TrackedItem tracked, int range) {
        int blockX = player.getBlockX();
        int blockZ = player.getBlockZ();
        int chunkRadius = (range >> 4) + 1;
        double best = (double) range * range;
        ChestBlockEntity found = null;
        for (int cx = (blockX >> 4) - chunkRadius; cx <= (blockX >> 4) + chunkRadius; cx++) {
            for (int cz = (blockZ >> 4) - chunkRadius; cz <= (blockZ >> 4) + chunkRadius; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (var entity : chunk.getBlockEntities().values()) {
                    if (!(entity instanceof ChestBlockEntity chest) || !accepts(chest, player, tracked)) {
                        continue;
                    }
                    double distance = chest.getBlockPos().distToCenterSqr(player.position());
                    if (distance <= best) {
                        best = distance;
                        found = chest;
                    }
                }
            }
        }
        return found;
    }

    private static boolean accepts(ChestBlockEntity chest, Player player, TrackedItem tracked) {
        if (chest instanceof BaseContainerBlockEntity container && container.isLocked()) {
            return false;
        }
        if (!matches(chest, tracked) || !hasRoom(chest, player, tracked)) {
            return false;
        }
        return true;
    }

    private static boolean matches(ChestBlockEntity chest, TrackedItem tracked) {
        if (chest.isEmpty()) {
            return true;
        }
        for (ItemStack stack : chest) {
            if (!stack.isEmpty() && tracked.item().isPresent() && stack.is(tracked.item().get())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRoom(Container chest, Player player, TrackedItem tracked) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (canTake(inventory.getItem(slot), tracked, chest)) {
                return true;
            }
        }
        return canTake(player.getOffhandItem(), tracked, chest);
    }

    private static boolean canTake(ItemStack source, TrackedItem tracked, Container chest) {
        if (!vomitable(source, tracked)) {
            return false;
        }
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack existing = chest.getItem(slot);
            if (existing.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameComponents(existing, source) && existing.getCount() < existing.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private static int moveInto(Player player, TrackedItem tracked, Container chest, int limit) {
        int remaining = limit;
        int moved = 0;
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE && remaining > 0; slot++) {
            int taken = take(inventory.getItem(slot), tracked, chest, remaining);
            remaining -= taken;
            moved += taken;
        }
        if (remaining > 0) {
            moved += take(player.getOffhandItem(), tracked, chest, remaining);
        }
        if (moved > 0) {
            chest.setChanged();
            inventory.setChanged();
        }
        return moved;
    }

    private static int take(ItemStack source, TrackedItem tracked, Container chest, int limit) {
        if (!vomitable(source, tracked) || limit <= 0) {
            return 0;
        }
        int moved = 0;
        for (int slot = 0; slot < chest.getContainerSize() && moved < limit && !source.isEmpty(); slot++) {
            ItemStack existing = chest.getItem(slot);
            if (existing.isEmpty() || !ItemStack.isSameItemSameComponents(existing, source)) {
                continue;
            }
            int space = existing.getMaxStackSize() - existing.getCount();
            int amount = Math.min(space, Math.min(limit - moved, source.getCount()));
            if (amount <= 0) {
                continue;
            }
            existing.grow(amount);
            source.shrink(amount);
            moved += amount;
        }
        for (int slot = 0; slot < chest.getContainerSize() && moved < limit && !source.isEmpty(); slot++) {
            if (!chest.getItem(slot).isEmpty()) {
                continue;
            }
            int amount = Math.min(source.getMaxStackSize(), Math.min(limit - moved, source.getCount()));
            chest.setItem(slot, source.split(amount));
            moved += amount;
        }
        return moved;
    }

    private static boolean throwOne(Player player, TrackedItem tracked) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (vomitable(stack, tracked)) {
                player.drop(stack.split(1), true);
                return true;
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (vomitable(offhand, tracked)) {
            player.drop(offhand.split(1), true);
            return true;
        }
        return false;
    }
}
