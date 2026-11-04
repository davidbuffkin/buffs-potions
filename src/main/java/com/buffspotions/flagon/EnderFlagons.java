package com.buffspotions.flagon;

import com.buffspotions.registry.ModAttachments;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * One flagon's worth of liquid, stored on the player the way an ender chest stores items.
 * Every ender flagon that player holds or places reads and writes this same store.
 */
public final class EnderFlagons {
    private static Supplier<FlagonContents> clientView = () -> FlagonContents.EMPTY;
    private static Supplier<UUID> clientPlayer = () -> null;
    private static Supplier<String> clientName = () -> "";
    private static Function<UUID, String> clientNames = uuid -> "";

    private EnderFlagons() {
    }

    public static FlagonContents of(Player player) {
        return player.getData(ModAttachments.ENDER_FLAGON);
    }

    public static void set(Player player, FlagonContents contents) {
        player.setData(ModAttachments.ENDER_FLAGON, contents);
        if (player.level() instanceof ServerLevel server) {
            remember(server.getServer(), player.getUUID(), contents);
        }
    }

    public static FlagonContents contents(MinecraftServer server, UUID player) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            return of(online);
        }
        return EnderFlagonStores.get(server).get(player);
    }

    public static void set(MinecraftServer server, UUID player, FlagonContents contents) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            set(online, contents);
            return;
        }
        remember(server, player, contents);
    }

    /** The world copy catches up with a player who just joined, or the other way around. */
    public static void adopt(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        EnderFlagonStores stores = EnderFlagonStores.get(server);
        if (stores.has(player.getUUID())) {
            player.setData(ModAttachments.ENDER_FLAGON, stores.get(player.getUUID()));
            AnchoredFlagons.refresh(server, player.getUUID(), of(player));
            return;
        }
        remember(server, player.getUUID(), of(player));
    }

    public static FlagonContents live(ServerLevel level, ItemStack stack) {
        FlagonAnchor anchor = anchor(stack);
        if (anchor == null) {
            return FlagonContents.of(stack);
        }
        return contents(level.getServer(), anchor.player());
    }

    public static void store(ServerLevel level, ItemStack stack, FlagonContents contents) {
        FlagonAnchor anchor = anchor(stack);
        stack.set(com.buffspotions.registry.ModDataComponents.FLAGON_CONTENTS.get(), contents);
        if (anchor != null) {
            set(level.getServer(), anchor.player(), contents);
        }
    }

    /** The player who owns this stack's liquid, when the stack is an ender flagon. */
    public static UUID storeId(Player player, ItemStack stack) {
        if (stack.getItem() instanceof com.buffspotions.item.AnchoredEnderFlagonItem) {
            FlagonAnchor anchor = stack.get(com.buffspotions.registry.ModDataComponents.FLAGON_ANCHOR.get());
            return anchor == null ? null : anchor.player();
        }
        if (stack.getItem() instanceof com.buffspotions.item.EnderFlagonItem) {
            return player.getUUID();
        }
        return null;
    }

    public static FlagonAnchor anchor(ItemStack stack) {
        if (!(stack.getItem() instanceof com.buffspotions.item.AnchoredEnderFlagonItem)) {
            return null;
        }
        return stack.get(com.buffspotions.registry.ModDataComponents.FLAGON_ANCHOR.get());
    }

    private static void remember(MinecraftServer server, UUID player, FlagonContents contents) {
        EnderFlagonStores.get(server).put(player, contents);
        AnchoredFlagons.refresh(server, player, contents);
    }

    /**
     * The local client's own store, used for tooltips, item icons, and the placed block.
     * Other players' stores are not synced, because each client draws its own liquid.
     */
    public static FlagonContents viewed() {
        return clientView.get();
    }

    public static void setClientView(Supplier<FlagonContents> clientView) {
        EnderFlagons.clientView = clientView;
    }

    public static UUID clientPlayer() {
        return clientPlayer.get();
    }

    public static String clientName() {
        return clientName.get();
    }

    public static void setClientIdentity(Supplier<UUID> clientPlayer, Supplier<String> clientName) {
        EnderFlagons.clientPlayer = clientPlayer;
        EnderFlagons.clientName = clientName;
    }

    public static void setClientNames(Function<UUID, String> clientNames) {
        EnderFlagons.clientNames = clientNames;
    }

    /** The current account name for this UUID, when this client can see that player. */
    public static String clientNameFor(UUID player) {
        String name = clientNames.apply(player);
        return name == null ? "" : name;
    }

    /** The current account name, or blank when this server has never seen that player. */
    public static String knownName(MinecraftServer server, UUID player) {
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (online != null) {
            String name = online.getGameProfile().name();
            if (name != null && !name.isBlank()) {
                return name;
            }
        }
        return server.services().nameToIdCache().get(player).map(NameAndId::name).filter(name -> !name.isBlank()).orElse("");
    }

    public static void rememberName(ItemStack stack, MinecraftServer server) {
        FlagonAnchor anchor = anchor(stack);
        if (anchor == null) {
            return;
        }
        String known = knownName(server, anchor.player());
        if (known.equals(anchor.name())) {
            return;
        }
        stack.set(com.buffspotions.registry.ModDataComponents.FLAGON_ANCHOR.get(), new FlagonAnchor(anchor.player(), known));
    }

    public static void refreshPlayerName(MinecraftServer server, UUID player) {
        String known = knownName(server, player);
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            net.minecraft.world.entity.player.Inventory inventory = online.getInventory();
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                rememberName(inventory.getItem(slot), server);
            }
            rememberName(online.containerMenu.getCarried(), server);
            net.minecraft.world.inventory.PlayerEnderChestContainer enderChest = online.getEnderChestInventory();
            for (int slot = 0; slot < enderChest.getContainerSize(); slot++) {
                rememberName(enderChest.getItem(slot), server);
            }
        }
        AnchoredFlagons.refreshNames(player, known);
    }
}
