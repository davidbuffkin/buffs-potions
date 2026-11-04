package com.buffspotions.item;

import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class EnderFlagonItem extends FlagonItem {
    public EnderFlagonItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected FlagonContents contentsFor(Player player, ItemStack stack) {
        return EnderFlagons.of(player);
    }

    @Override
    protected boolean contentsArePersonal() {
        return true;
    }

    @Override
    protected FlagonContents tooltipContents(ItemStack stack) {
        return EnderFlagons.viewed();
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.buffs_potions.ender_flagon");
    }
}
