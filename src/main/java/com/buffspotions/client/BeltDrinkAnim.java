package com.buffspotions.client;

import com.buffspotions.belt.BeltSource;
import com.buffspotions.belt.BeltUseState;
import com.buffspotions.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * First-person belt drinks advance here, one tick at a time.
 * The synced elapsed count arrives in bursts, and the drink pose treats those bursts as a sped-up sip.
 */
public final class BeltDrinkAnim {
    private static int elapsed = -1;
    private static int slot = Integer.MIN_VALUE;

    private BeltDrinkAnim() {
    }

    public static void tick(Player player) {
        BeltUseState state = player.getData(ModAttachments.BELT_USE.get());
        if (!state.isDrinking() || state.gulp() || BeltSource.drinking(player).isEmpty()) {
            clear();
            return;
        }
        if (elapsed < 0 || state.slot() != slot) {
            elapsed = 0;
            slot = state.slot();
            return;
        }
        elapsed++;
    }

    public static void clear() {
        elapsed = -1;
        slot = Integer.MIN_VALUE;
    }

    public static int remaining(Player player) {
        if (elapsed < 0) {
            return player.getUseItemRemainingTicks();
        }
        ItemStack belt = BeltSource.drinking(player);
        int duration = Math.max(1, belt.getUseDuration(player));
        return Math.max(1, duration - elapsed);
    }
}
