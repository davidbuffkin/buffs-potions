package com.buffspotions.equipment;

import com.buffspotions.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class BeltSlotAccess {
    private static final MethodHandle CURIOS_GET = curiosHandle("getBelt", ItemStack.class, Player.class);
    private static final MethodHandle CURIOS_SET = curiosHandle("setBelt", boolean.class, Player.class, ItemStack.class);

    public static boolean usesCurios() {
        return CURIOS_GET != null;
    }

    public static ItemStack getEquipped(Player player) {
        if (usesCurios()) {
            try {
                return (ItemStack) CURIOS_GET.invokeExact(player);
            } catch (Throwable exception) {
                throw new IllegalStateException("Could not read the Curios belt slot", exception);
            }
        }
        return player.getData(ModAttachments.BELT_SLOT).stack();
    }

    public static void setEquipped(Player player, ItemStack stack) {
        if (usesCurios()) {
            try {
                boolean updated = (boolean) CURIOS_SET.invokeExact(player, stack);
                if (!updated) {
                    throw new IllegalStateException("Curios belt slot is not available");
                }
            } catch (Throwable exception) {
                throw new IllegalStateException("Could not update the Curios belt slot", exception);
            }
            return;
        }
        player.getData(ModAttachments.BELT_SLOT).set(stack);
        player.syncData(ModAttachments.BELT_SLOT);
    }

    public static Migration migrate(ItemStack fallback, boolean curiosSlotEmpty) {
        if (fallback.isEmpty()) {
            return Migration.NONE;
        }
        return curiosSlotEmpty ? Migration.TO_CURIOS : Migration.TO_INVENTORY;
    }

    public static void migrateToCurios(Player player) {
        if (!usesCurios()) {
            return;
        }
        BeltSlotData fallback = player.getData(ModAttachments.BELT_SLOT);
        ItemStack oldBelt = fallback.stack();
        if (migrate(oldBelt, getEquipped(player).isEmpty()) == Migration.NONE) {
            return;
        }
        if (migrate(oldBelt, getEquipped(player).isEmpty()) == Migration.TO_CURIOS) {
            setEquipped(player, oldBelt.copy());
        } else if (!player.getInventory().add(oldBelt.copy())) {
            player.drop(oldBelt.copy(), false);
        }
        fallback.set(ItemStack.EMPTY);
        player.syncData(ModAttachments.BELT_SLOT);
    }

    private static MethodHandle curiosHandle(String name, Class<?> returnType, Class<?>... parameters) {
        if (!ModList.get().isLoaded("curios")) {
            return null;
        }
        try {
            Class<?> compat = Class.forName("com.buffspotions.compat.CuriosCompat");
            return MethodHandles.publicLookup().findStatic(compat, name, MethodType.methodType(returnType, parameters));
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Curios is loaded but its belt bridge is unavailable", exception);
        }
    }

    public enum Migration {
        NONE,
        TO_CURIOS,
        TO_INVENTORY
    }

    private BeltSlotAccess() {
    }
}
