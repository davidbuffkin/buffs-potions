package com.buffspotions.compat;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

public final class CuriosCompat {
    public static ItemStack getBelt(Player player) {
        return CuriosApi.getCuriosInventory(player)
                .flatMap(handler -> handler.findCurio("belt", 0))
                .map(result -> result.stack())
                .orElse(ItemStack.EMPTY);
    }

    public static boolean setBelt(Player player, ItemStack stack) {
        var inventory = CuriosApi.getCuriosInventory(player);
        inventory.ifPresent(handler -> handler.setEquippedCurio("belt", 0, stack));
        return inventory.isPresent();
    }

    private CuriosCompat() {
    }
}
