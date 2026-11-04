package com.buffspotions.item;

import com.buffspotions.effect.EyedDisplay;
import com.buffspotions.potion.Remedies;
import com.buffspotions.potion.SuspiciousBrew;
import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.belt.BeltDrinkContext;
import com.buffspotions.flagon.FlagonInteractions;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.registry.ModPotions;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class FlagonItem extends BlockItem {
    private static final Consumable HONEY_DRINK = new Consumable(
            Consumables.DEFAULT_DRINK.consumeSeconds(),
            Consumables.DEFAULT_DRINK.animation(),
            SoundEvents.HONEY_DRINK,
            Consumables.DEFAULT_DRINK.hasConsumeParticles(),
            Consumables.DEFAULT_DRINK.onConsumeEffects()
    );

    public FlagonItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() != null) {
            syncDrinkSound(context.getPlayer(), context.getItemInHand());
        }
        InteractionResult filled = fillFromWater(context.getLevel(), context.getPlayer(), context.getHand(), context.getClickedPos());
        if (filled != null) {
            return filled;
        }
        Player player = context.getPlayer();
        if (player != null && !player.isSecondaryUseActive() && hasDrinkableDose(player, context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        syncDrinkSound(player, player.getItemInHand(hand));
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() == HitResult.Type.BLOCK) {
            InteractionResult filled = fillFromWater(level, player, hand, hit.getBlockPos());
            if (filled != null) {
                return filled;
            }
        }
        if (contentsFor(player, player.getItemInHand(hand)).isEmpty()) {
            return InteractionResult.PASS;
        }
        return super.use(level, player, hand);
    }

    private InteractionResult fillFromWater(Level level, Player player, InteractionHand hand, BlockPos pos) {
        if (player == null || !contentsFor(player, player.getItemInHand(hand)).canFillFromWater()) {
            return null;
        }
        if (!level.getFluidState(pos).is(FluidTags.WATER) || !level.getFluidState(pos).isSource()) {
            return null;
        }
        if (!level.mayInteract(player, pos)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack held = player.getItemInHand(hand);
            giveFilled(player, hand, held, contentsFor(player, held).fillWithWater());
            player.awardStat(Stats.ITEM_USED.get(held.getItem()));
            level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * The liquid this flagon drinks and fills. A normal flagon reads its own stack.
     */
    protected FlagonContents contentsFor(Player player, ItemStack stack) {
        return FlagonContents.of(stack);
    }

    public FlagonContents playerContents(Player player, ItemStack stack) {
        return contentsFor(player, stack);
    }

    public boolean hasDrinkableDose(Player player, ItemStack stack) {
        return !contentsFor(player, stack).isEmpty();
    }

    public void writeContents(Player player, ItemStack stack, FlagonContents contents) {
        if (contentsArePersonal()) {
            EnderFlagons.set(player, contents);
            return;
        }
        stack.set(ModDataComponents.FLAGON_CONTENTS.get(), contents);
    }

    /**
     * Personal flagons keep one store for the player, so filling must not split the held stack.
     */
    protected boolean contentsArePersonal() {
        return false;
    }

    protected void giveFilled(Player player, InteractionHand hand, ItemStack held, FlagonContents contents) {
        if (contentsArePersonal()) {
            writeContents(player, held, contents);
            return;
        }
        if (player.hasInfiniteMaterials()) {
            ItemStack filled = held.copyWithCount(1);
            filled.set(ModDataComponents.FLAGON_CONTENTS.get(), contents);
            if (!player.getInventory().add(filled)) {
                player.drop(filled, false);
            }
            return;
        }
        if (held.getCount() <= 1) {
            held.set(ModDataComponents.FLAGON_CONTENTS.get(), contents);
            return;
        }
        held.shrink(1);
        ItemStack filled = held.copyWithCount(1);
        filled.set(ModDataComponents.FLAGON_CONTENTS.get(), contents);
        if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!(user instanceof Player player)) {
            return stack;
        }
        FlagonContents contents = contentsFor(player, stack);
        if (contents.isEmpty() || level.isClientSide()) {
            return stack;
        }
        FlagonInteractions.applyDose(player, contents.dose());
        if (player instanceof ServerPlayer server && BeltDrinkContext.explicitSource().isEmpty()) {
            ModCriteria.FLAGON_HELD_DRINK.get().trigger(server);
        }
        if (!player.hasInfiniteMaterials()) {
            writeContents(player, stack, contents.removeDose());
        }
        return stack;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        if (contentsArePersonal()) {
            return super.getMaxStackSize(stack);
        }
        return FlagonContents.of(stack).isEmpty() ? super.getMaxStackSize(stack) : 1;
    }

    private void syncDrinkSound(Player player, ItemStack stack) {
        stack.set(
                DataComponents.CONSUMABLE,
                contentsFor(player, stack).isHoney() ? HONEY_DRINK : Consumables.DEFAULT_DRINK
        );
    }

    @Override
    public Component getName(ItemStack stack) {
        return FlagonContents.of(stack).itemName();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> tooltip,
            TooltipFlag flag
    ) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        FlagonContents contents = tooltipContents(stack);
        if (contents.isEmpty()) {
            return;
        }
        if (Remedies.isRemedy(contents.dose())) {
            Remedies.appendTooltip(contents.dose(), tooltip);
        } else if (contents.isHoney()) {
            if (this instanceof EnderFlagonItem) {
                tooltip.accept(Component.translatable("item.buffs_potions.flagon.liquid.honey"));
            }
        } else if (contents.isWater()) {
            if (this instanceof EnderFlagonItem) {
                tooltip.accept(Component.translatable("item.buffs_potions.flagon.liquid.water"));
            }
        } else {
            PotionContents potion = contents.dose().get(DataComponents.POTION_CONTENTS);
            if (potion != null && potion.is(ModPotions.SUSPICIOUS_BREW)) {
                tooltip.accept(SuspiciousBrew.unknownEffect());
            } else if (potion != null) {
                EyedDisplay.TOOLTIP.set(contents.dose().get(ModDataComponents.ESSENCE.get()));
                try {
                    PotionContents.addPotionTooltip(
                            potion.getAllEffects(),
                            tooltip,
                            contents.dose().getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F),
                            context.tickRate()
                    );
                } finally {
                    EyedDisplay.TOOLTIP.remove();
                }
            }
        }
        tooltip.accept(Component.translatable(
                contents.doses() == 1 ? "item.buffs_potions.flagon.dose" : "item.buffs_potions.flagon.doses",
                contents.doses()
        ).withStyle(ChatFormatting.GRAY));
    }

    protected FlagonContents tooltipContents(ItemStack stack) {
        return FlagonContents.of(stack);
    }
}
