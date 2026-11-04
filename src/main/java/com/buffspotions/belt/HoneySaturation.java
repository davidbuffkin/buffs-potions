package com.buffspotions.belt;

import com.buffspotions.item.FlagonItem;
import com.buffspotions.registry.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Honey's vanilla saturation is doubled. A drink from a protraction belt, or from the hand while
 * one is worn, multiplies that doubled amount by 2, 3, or 4 at protraction I, II, and III.
 */
public final class HoneySaturation {
    private HoneySaturation() {
    }

    /** 1 with no protraction, otherwise 2, 3, or 4. */
    public static int factor(Player player) {
        return multiplier(BeltEnchantments.level(source(player), BeltEnchantments.PROTRACTION));
    }

    public static int multiplier(int protractionLevel) {
        int level = Math.min(Math.max(protractionLevel, 0), 3);
        return level <= 0 ? 1 : level + 1;
    }

    /** Saturation to add on top of the vanilla honey gain so the total is vanilla × 2 × factor. */
    public static float extra(float vanillaSaturation, int factor) {
        return vanillaSaturation * (2.0F * factor - 1.0F);
    }

    public static void afterEat(Player player, float vanillaSaturation) {
        float extra = extra(vanillaSaturation, factor(player));
        if (extra == 0.0F) {
            return;
        }
        FoodData food = player.getFoodData();
        food.setSaturation(Math.min(food.getFoodLevel(), food.getSaturationLevel() + extra));
    }

    private static ItemStack source(Player player) {
        return sourceBelt(
                BeltDrinkContext.explicitSource(),
                player.getData(ModAttachments.BELT_USE).gulp(),
                BeltSource.drinking(player),
                fromHand(player),
                BeltSource.equipped(player)
        );
    }

    static ItemStack sourceBelt(
            ItemStack explicit, boolean gulp, ItemStack gulpBelt, boolean fromHand, ItemStack equipped
    ) {
        if (!explicit.isEmpty()) {
            return explicit;
        }
        if (gulp) {
            return gulpBelt;
        }
        if (fromHand) {
            return equipped;
        }
        return ItemStack.EMPTY;
    }

    private static boolean fromHand(Player player) {
        if (!player.isUsingItem()) {
            return false;
        }
        ItemStack using = player.getUseItem();
        if (using.is(Items.HONEY_BOTTLE)) {
            return true;
        }
        return using.getItem() instanceof FlagonItem flagon && flagon.playerContents(player, using).isHoney();
    }
}
