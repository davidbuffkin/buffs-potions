package com.buffspotions.effect;

import com.buffspotions.potion.MundaneBrewing;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModPotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

/**
 * One drink of a mundane Potion of Harvesting counts the crops gathered while it lasts.
 * Another mundane Harvesting drink starts the count over. Any other Harvesting potion ends it.
 */
public final class VermillionHarvest {
    public static final int GOAL = 100;

    private VermillionHarvest() {
    }

    public static void drink(ServerPlayer player, ItemStack stack) {
        if (!stack.is(Items.POTION) || !isHarvesting(stack)) {
            return;
        }
        player.setData(ModAttachments.VERMILLION, mundaneHarvesting(stack) ? 0 : -1);
    }

    /** The Harvesting effect ended, so this drink can no longer finish the count. */
    public static void stop(ServerPlayer player) {
        if (player.getData(ModAttachments.VERMILLION) >= 0) {
            player.setData(ModAttachments.VERMILLION, -1);
        }
    }

    public static void note(ServerPlayer player) {
        if (!player.hasEffect(ModEffects.GREEN_THUMB)) {
            return;
        }
        int count = player.getData(ModAttachments.VERMILLION);
        if (count < 0) {
            return;
        }
        count++;
        player.setData(ModAttachments.VERMILLION, count >= GOAL ? -1 : count);
        if (count >= GOAL) {
            ModCriteria.VERMILLION.get().trigger(player);
        }
    }

    private static boolean mundaneHarvesting(ItemStack stack) {
        return MundaneBrewing.isMundane(stack)
                && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(ModPotions.HARVESTING);
    }

    private static boolean isHarvesting(ItemStack stack) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null) {
            return false;
        }
        for (MobEffectInstance effect : contents.getAllEffects()) {
            if (effect.getEffect().is(ModEffects.GREEN_THUMB)) {
                return true;
            }
        }
        return false;
    }
}
