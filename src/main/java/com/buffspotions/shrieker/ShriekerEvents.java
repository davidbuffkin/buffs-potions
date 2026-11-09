package com.buffspotions.shrieker;

import com.buffspotions.BuffsPotions;
import com.buffspotions.registry.ModBlocks;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.CustomClickActionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class ShriekerEvents {
    private static final Map<Class<?>, Boolean> USABLE_ITEMS = new ConcurrentHashMap<>();

    private ShriekerEvents() {
    }

    /**
     * Shift-right-click toggles listening. An item that can be placed or used, in either hand,
     * keeps the click so the player can build against the shrieker.
     */
    @SubscribeEvent
    public static void keepShiftClick(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!player.level().getBlockState(event.getPos()).is(ModBlocks.CALIBRATED_SCULK_SHRIEKER.get())) {
            return;
        }
        if (!player.isSecondaryUseActive() || heldItemCanBeUsed(player)) {
            return;
        }
        event.setUseBlock(TriState.TRUE);
        event.setUseItem(TriState.FALSE);
    }

    static boolean canUse(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof BlockItem) {
            return true;
        }
        if (stack.has(DataComponents.CONSUMABLE)
                || stack.has(DataComponents.BLOCKS_ATTACKS)
                || stack.has(DataComponents.KINETIC_WEAPON)) {
            return true;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null && equippable.swappable()) {
            return true;
        }
        return USABLE_ITEMS.computeIfAbsent(stack.getItem().getClass(), ShriekerEvents::declaresUse);
    }

    private static boolean heldItemCanBeUsed(Player player) {
        return canUse(player.getMainHandItem()) || canUse(player.getOffhandItem());
    }

    private static boolean declaresUse(Class<?> type) {
        Class<?> cursor = type;
        while (cursor != null && cursor != Item.class) {
            for (Method method : cursor.getDeclaredMethods()) {
                String name = method.getName();
                if (name.equals("use") || name.equals("useOn")) {
                    return true;
                }
            }
            cursor = cursor.getSuperclass();
        }
        return false;
    }

    @SubscribeEvent
    public static void stopListening(CustomClickActionEvent event) {
        if (!CalibratedSculkShriekerBlockEntity.STOP_LISTENING.equals(event.getIdentifier())) {
            return;
        }
        event.setCanceled(true);
        if (event.getPlayer() instanceof ServerPlayer player) {
            CalibratedSculkShriekerBlockEntity.stopFromClick(player, event.getPayload());
        }
    }
}
