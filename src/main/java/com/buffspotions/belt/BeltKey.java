package com.buffspotions.belt;

import com.buffspotions.registry.ModAttachments;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.player.Player;

/**
 * Whether the player is holding the Use Belt control. The client reads the key
 * mapping; the server reads the state the client last sent.
 */
public final class BeltKey {
    private static BooleanSupplier clientDown = () -> false;

    private BeltKey() {
    }

    public static void bindClient(BooleanSupplier down) {
        clientDown = down;
    }

    public static boolean isDown(Player player) {
        if (player.level().isClientSide()) {
            return clientDown.getAsBoolean();
        }
        return player.getData(ModAttachments.BELT_KEY);
    }
}
