package com.buffspotions.item;

import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonAnchor;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModDataComponents;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

/** An ender flagon locked to the player who crafted it, readable by everyone else. */
public final class AnchoredEnderFlagonItem extends EnderFlagonItem {
    public AnchoredEnderFlagonItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static void bind(ItemStack stack, Player player) {
        if (!(stack.getItem() instanceof AnchoredEnderFlagonItem) || player == null) {
            return;
        }
        if (stack.get(ModDataComponents.FLAGON_ANCHOR.get()) != null) {
            return;
        }
        stack.set(
                ModDataComponents.FLAGON_ANCHOR.get(),
                new FlagonAnchor(player.getUUID(), player.getName().getString())
        );
        FlagonContents contents = EnderFlagons.of(player);
        stack.set(ModDataComponents.FLAGON_CONTENTS.get(), contents);
        if (player.level() instanceof ServerLevel server) {
            EnderFlagons.set(server.getServer(), player.getUUID(), contents);
        }
    }

    /** Liquid to draw. An unbound stack, or the local player's own, follows that client live. */
    public static FlagonContents shown(ItemStack stack) {
        FlagonAnchor anchor = stack.get(ModDataComponents.FLAGON_ANCHOR.get());
        java.util.UUID local = EnderFlagons.clientPlayer();
        if (anchor == null || (local != null && local.equals(anchor.player()))) {
            return EnderFlagons.viewed();
        }
        return FlagonContents.of(stack);
    }

    public static String shownName(ItemStack stack) {
        FlagonAnchor anchor = stack.get(ModDataComponents.FLAGON_ANCHOR.get());
        if (anchor == null) {
            return EnderFlagons.clientName();
        }
        String live = EnderFlagons.clientNameFor(anchor.player());
        if (!live.isEmpty()) {
            return live;
        }
        if (!anchor.name().isEmpty()) {
            return anchor.name();
        }
        return anchor.player().toString();
    }

    @Override
    public void onCraftedBy(ItemStack stack, Player player) {
        super.onCraftedBy(stack, player);
        bind(stack, player);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        EnderFlagons.rememberName(stack, level.getServer());
        FlagonAnchor anchor = stack.get(ModDataComponents.FLAGON_ANCHOR.get());
        if (anchor == null) {
            return;
        }
        FlagonContents live = EnderFlagons.contents(level.getServer(), anchor.player());
        if (!FlagonContents.of(stack).sameFill(live)) {
            stack.set(ModDataComponents.FLAGON_CONTENTS.get(), live);
        }
    }

    @Override
    protected FlagonContents contentsFor(Player player, ItemStack stack) {
        if (!(player.level() instanceof ServerLevel server)) {
            return shown(stack);
        }
        FlagonAnchor anchor = stack.get(ModDataComponents.FLAGON_ANCHOR.get());
        if (anchor == null) {
            return FlagonContents.EMPTY;
        }
        return EnderFlagons.contents(server.getServer(), anchor.player());
    }

    @Override
    public void writeContents(Player player, ItemStack stack, FlagonContents contents) {
        stack.set(ModDataComponents.FLAGON_CONTENTS.get(), contents);
        FlagonAnchor anchor = stack.get(ModDataComponents.FLAGON_ANCHOR.get());
        if (anchor != null && player.level() instanceof ServerLevel server) {
            EnderFlagons.set(server.getServer(), anchor.player(), contents);
        }
    }

    @Override
    protected FlagonContents tooltipContents(ItemStack stack) {
        return shown(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        String name = shownName(stack);
        if (!name.isEmpty()) {
            tooltip.accept(Component.translatable(
                    "item.buffs_potions.anchored_ender_flagon.anchored",
                    Component.literal(name).withStyle(ChatFormatting.BLUE)
            ).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
