package com.buffspotions.client;

import com.buffspotions.BuffsPotions;
import com.buffspotions.network.BeltKeyPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public final class BeltKeys {
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(BuffsPotions.MOD_ID, "main"));
    public static final KeyMapping USE_BELT = new KeyMapping(
            "key.buffs_potions.use_belt",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LSHIFT,
            CATEGORY
    );

    private static boolean sent;

    private BeltKeys() {
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(USE_BELT);
    }

    public static boolean isDown() {
        return USE_BELT.isDown();
    }

    /** Use Belt is the sneak key, so a right-click keeps the usual block and item priority. */
    public static boolean boundToSneak(Minecraft minecraft) {
        return USE_BELT.getKey().equals(minecraft.options.keyShift.getKey());
    }

    /**
     * Pressing the shared key must crouch. NeoForge looks up a modifier key as a
     * modifier first, and that pass can leave sneak up for the press.
     */
    public static void keepSneak(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.gui.screen() != null || !boundToSneak(minecraft)) {
            return;
        }
        KeyMapping sneak = minecraft.options.keyShift;
        InputConstants.Key key = sneak.getKey();
        if (key.getType() != InputConstants.Type.KEYSYM || sneak.isUnbound()) {
            return;
        }
        boolean down = InputConstants.isKeyDown(minecraft.getWindow(), key.getValue());
        if (!minecraft.options.toggleCrouch().get() && sneak.isDown() != down) {
            sneak.setDown(down);
        }
        if (USE_BELT.isDown() != down) {
            USE_BELT.setDown(down);
        }
    }

    /** Sends the held state when it changes, before other belt packets from this frame. */
    public static void sync(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.getConnection() == null) {
            sent = false;
            return;
        }
        boolean down = isDown();
        if (down == sent) {
            return;
        }
        sent = down;
        ClientPacketDistributor.sendToServer(new BeltKeyPayload(down));
    }
}
