package com.buffspotions.belt;

import com.buffspotions.flagon.AnchoredEnderFlagonBlock;
import com.buffspotions.flagon.EnderFlagonBlock;
import com.buffspotions.flagon.FlagonBlock;
import com.buffspotions.item.PotionBeltItem;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * While Use Belt is the sneak key and the active dose can be drunk, a right-click drinks that dose.
 * Placing a block, using a block or entity, and shooting a bow or crossbow keep the click.
 * A different Use Belt key drinks before those.
 */
public final class BeltClickPriority {
    private static final Map<Class<?>, Boolean> INTERACTIVE_BLOCKS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> AIR_USE_ITEMS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> BLOCK_USE_ITEMS = new ConcurrentHashMap<>();

    /** The click does something other than drinking the belt. */
    public static boolean allows(Player player, HitResult hit, ItemStack main, ItemStack offhand) {
        if (passes(player, hit, main)) {
            return true;
        }
        return !claimsClick(main, hit) && passes(player, hit, offhand);
    }

    public static boolean passes(Player player, HitResult hit, ItemStack held) {
        if (held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem) {
            return true;
        }
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            return true;
        }
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            if (held.getItem() instanceof BlockItem) {
                return true;
            }
            return interactive(player.level().getBlockState(blockHit.getBlockPos()).getBlock());
        }
        return false;
    }

    /** Sneak normally skips a block's click. A placed flagon still wants it for pouring, bottles, and pickup. */
    public static void keepFlagonClick(PlayerInteractEvent.RightClickBlock event) {
        Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
        if (block instanceof FlagonBlock || block instanceof EnderFlagonBlock || block instanceof AnchoredEnderFlagonBlock) {
            event.setUseBlock(TriState.TRUE);
        }
    }

    public static void preferBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!BeltKey.isDown(player) || !PotionBeltItem.canDrink(player, BeltSource.shown(player))) {
            return;
        }
        ItemStack held = event.getItemStack();
        if (held.getItem() instanceof BlockItem || held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem) {
            return;
        }
        if (!interactive(event.getLevel().getBlockState(event.getPos()).getBlock())) {
            return;
        }
        event.setUseBlock(TriState.TRUE);
        event.setUseItem(TriState.FALSE);
    }

    public static boolean interactive(Block block) {
        return INTERACTIVE_BLOCKS.computeIfAbsent(block.getClass(), type -> declares(type, BlockBehaviour.class, "useWithoutItem", "useItemOn"));
    }

    private static boolean claimsClick(ItemStack held, HitResult hit) {
        if (held.isEmpty() || held.getItem() instanceof BowItem || held.getItem() instanceof CrossbowItem) {
            return false;
        }
        if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
            return false;
        }
        if (hit instanceof BlockHitResult && hit.getType() == HitResult.Type.BLOCK && held.getItem() instanceof BlockItem) {
            return false;
        }
        if (held.has(DataComponents.CONSUMABLE)) {
            return true;
        }
        Item item = held.getItem();
        return usesInAir(item) || (hit != null && hit.getType() == HitResult.Type.BLOCK && usesOnBlock(item));
    }

    private static boolean usesInAir(Item item) {
        return AIR_USE_ITEMS.computeIfAbsent(item.getClass(), type -> declares(type, Item.class, "use"));
    }

    private static boolean usesOnBlock(Item item) {
        return BLOCK_USE_ITEMS.computeIfAbsent(item.getClass(), type -> declares(type, Item.class, "useOn"));
    }

    private static boolean declares(Class<?> type, Class<?> stop, String... names) {
        Class<?> cursor = type;
        while (cursor != null && cursor != stop) {
            for (Method method : cursor.getDeclaredMethods()) {
                for (String name : names) {
                    if (method.getName().equals(name)) {
                        return true;
                    }
                }
            }
            cursor = cursor.getSuperclass();
        }
        return false;
    }

    private BeltClickPriority() {
    }
}
