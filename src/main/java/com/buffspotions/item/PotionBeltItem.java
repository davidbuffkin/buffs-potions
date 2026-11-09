package com.buffspotions.item;

import com.buffspotions.belt.BeltContents;
import com.buffspotions.belt.BeltEnchantments;
import com.buffspotions.belt.BeltConsumptionService;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.equipment.FallbackBeltContainer;
import com.buffspotions.menu.BeltItemContainer;
import com.buffspotions.menu.PotionBeltMenu;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;

public class PotionBeltItem extends Item {
    public PotionBeltItem(Properties properties) {
        super(properties);
    }

    public static void clearStoredDye(ItemStack stack) {
        if (stack.has(DataComponents.DYED_COLOR)) {
            stack.remove(DataComponents.DYED_COLOR);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        clearStoredDye(stack);
    }

    public static void openMenu(ServerPlayer player, int source) {
        ItemStack belt = new BeltItemContainer(player, source).belt();
        player.openMenu(
                menuProvider(source, title(belt)),
                buffer -> BeltItemContainer.writeOpenData(buffer, source, new BeltItemContainer(player, source).belt())
        );
    }

    public static void openMenu(ServerPlayer player, Container container, int slot) {
        player.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, owner) -> new PotionBeltMenu(containerId, inventory, container, slot),
                        title(container.getItem(slot))
                ),
                buffer -> BeltItemContainer.writeOpenData(buffer, container.getItem(slot))
        );
    }

    public static boolean canActivate(Player player, ItemStack belt) {
        ItemStack dose = BeltContents.get(belt, BeltContents.activeSlot(belt));
        if (player.getCooldowns().isOnCooldown(belt) || !BeltContents.accepts(belt, dose)) {
            return false;
        }
        if (dose.getItem() instanceof FlagonItem flagon) {
            return flagon.hasDrinkableDose(player, dose);
        }
        return true;
    }

    public static boolean canDrink(Player player, ItemStack belt) {
        return BeltContents.isDrinkableDose(player, belt, BeltContents.get(belt, BeltContents.activeSlot(belt)))
                && !player.getCooldowns().isOnCooldown(belt);
    }

    /** A belt in hand whose active slot cannot be drunk or thrown. Right-click opens that belt when the click does not use a block or entity. */
    public static boolean opensWhenUndrinkable(Player player, ItemStack belt) {
        if (!(belt.getItem() instanceof PotionBeltItem) || player.isSpectator()) {
            return false;
        }
        ItemStack dose = BeltContents.get(belt, BeltContents.activeSlot(belt));
        if (BeltContents.isThrowableDose(dose)
                || BeltConsumptionService.shouldSplash(player, belt, dose)
                || BeltConsumptionService.blockedThrow(player, belt, dose)) {
            return false;
        }
        return !BeltContents.isDrinkableDose(player, belt, dose);
    }

    public static final float DRINK_VOLUME = 0.5F;
    public static final float GULP_DRINK_VOLUME = 0.05F;

    public static void playDrinkSound(Player player, ItemStack dose, int elapsedTicks) {
        playDrinkSound(player, dose, elapsedTicks, DRINK_VOLUME);
    }

    public static void playDrinkSound(Player player, ItemStack dose, int elapsedTicks, float volume) {
        if (player.level().isClientSide() || elapsedTicks <= 0 || elapsedTicks % 4 != 0) {
            return;
        }
        boolean honey = dose.is(Items.HONEY_BOTTLE)
                || (dose.getItem() instanceof FlagonItem flagon && flagon.playerContents(player, dose).isHoney());
        Consumable consumable = dose.get(DataComponents.CONSUMABLE);
        if (consumable == null && !honey) {
            return;
        }
        if (honey || consumable.hasConsumeParticles()) {
            player.spawnItemParticles(dose, 5);
        }
        SoundEvent sound = honey ? SoundEvents.HONEY_DRINK.value() : consumable.sound().value();
        player.level().playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                sound,
                SoundSource.PLAYERS,
                volume,
                Mth.randomBetween(player.getRandom(), 0.9F, 1.0F)
        );
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.getData(ModAttachments.BELT_USE).pendingAdvance()) {
            return InteractionResult.CONSUME;
        }
        ItemStack belt = player.getItemInHand(hand);
        ItemStack dose = BeltContents.get(belt, BeltContents.activeSlot(belt));
        if (BeltConsumptionService.blockedThrow(player, belt, dose)) {
            if (player instanceof ServerPlayer serverPlayer) {
                BeltConsumptionService.failThrow(serverPlayer);
            }
            return InteractionResult.SUCCESS;
        }
        if (BeltConsumptionService.shouldSplash(player, belt, dose)) {
            if (player instanceof ServerPlayer serverPlayer) {
                BeltConsumptionService.splashFlagon(serverPlayer, belt, BeltContents.activeSlot(belt), false);
            }
            return InteractionResult.SUCCESS;
        }
        if (BeltContents.isThrowableDose(dose)) {
            if (!player.getCooldowns().isOnCooldown(belt) && player instanceof ServerPlayer serverPlayer) {
                BeltConsumptionService.throwDose(serverPlayer, belt, BeltContents.activeSlot(belt), false);
            }
            return InteractionResult.SUCCESS;
        }
        if (!canDrink(player, belt)) {
            if (opensWhenUndrinkable(player, belt) && player instanceof ServerPlayer serverPlayer) {
                int source = hand == InteractionHand.MAIN_HAND ? BeltItemContainer.MAIN_HAND : BeltItemContainer.OFF_HAND;
                openMenu(serverPlayer, source);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack belt, int remainingUseDuration) {
        if (!(user instanceof Player player)) {
            return;
        }
        int elapsed = belt.getUseDuration(player) - remainingUseDuration;
        ItemStack dose = BeltContents.get(belt, BeltContents.activeSlot(belt));
        playDrinkSound(player, dose, elapsed);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack belt, Level level, LivingEntity user) {
        int slot = BeltContents.activeSlot(belt);
        if (user instanceof ServerPlayer player) {
            BeltConsumptionService.consume(player, belt, slot);
        }
        return belt;
    }

    public static boolean canFillFromBrewingStand(ItemStack belt, BrewingStandBlockEntity stand) {
        if (firstTransferSlot(belt) < 0) {
            return false;
        }
        for (int standSlot = 0; standSlot < 3; standSlot++) {
            if (BeltContents.accepts(belt, stand.getItem(standSlot))) {
                return true;
            }
        }
        return false;
    }

    public static boolean fillFromBrewingStand(ItemStack belt, BrewingStandBlockEntity stand) {
        boolean changed = false;
        for (int standSlot = 0; standSlot < 3; standSlot++) {
            ItemStack potion = stand.getItem(standSlot);
            if (!BeltContents.accepts(belt, potion)) {
                continue;
            }
            int destination = firstTransferSlot(belt);
            if (destination < 0) {
                break;
            }
            ItemStack replaced = BeltContents.get(belt, destination);
            BeltContents.set(belt, destination, potion);
            stand.setItem(standSlot, BeltContents.isResidue(replaced)
                    ? new ItemStack(Items.GLASS_BOTTLE)
                    : ItemStack.EMPTY);
            changed = true;
        }
        if (changed) {
            stand.setChanged();
        }
        return changed;
    }

    public static void appendBeltTooltip(ItemStack belt, List<Component> tooltip) {
        int capacity = BeltEnchantments.capacity(belt);
        int filled = 0;
        for (int slot = 0; slot < capacity; slot++) {
            if (!BeltContents.get(belt, slot).isEmpty()) {
                filled++;
            }
        }
        boolean flagons = belt.getItem() instanceof FlagonBeltItem;
        tooltip.add(Component.translatable(
                flagons ? "item.buffs_potions.flagon_belt.count" : "item.buffs_potions.potion_belt.count",
                filled,
                capacity
        ).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack belt, Slot slot, ClickAction action, Player player) {
        if (action == ClickAction.SECONDARY && slot.getItem().isEmpty()) {
            depositLast(belt, slot, player);
            return true;
        }
        if (action != ClickAction.PRIMARY) {
            return false;
        }
        ItemStack other = slot.getItem();
        if (other.isEmpty()) {
            return false;
        }
        if (BeltContents.insertFirst(belt, other, BeltEnchantments.capacity(belt), player) < 0) {
            playInsertFail(player);
            return true;
        }
        slot.setChanged();
        return true;
    }

    @Override
    public boolean overrideOtherStackedOnMe(
            ItemStack belt,
            ItemStack other,
            Slot slot,
            ClickAction action,
            Player player,
            SlotAccess carried
    ) {
        if (action == ClickAction.SECONDARY && other.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                openFromSlot(serverPlayer, slot);
            }
            return true;
        }
        if (action != ClickAction.PRIMARY || other.isEmpty()) {
            return false;
        }
        if (BeltContents.insertFirst(belt, other, BeltEnchantments.capacity(belt), player) < 0) {
            playInsertFail(player);
            return true;
        }
        return true;
    }

    /**
     * Right-click an empty slot while the belt is on the cursor. Empty bottles or flagons
     * leave together. Otherwise the rightmost item drops there.
     */
    private static void depositLast(ItemStack belt, Slot slot, Player player) {
        int capacity = BeltEnchantments.capacity(belt);
        ItemStack empties = BeltContents.takeEmptyStack(belt, capacity);
        if (!empties.isEmpty()) {
            giveRemoved(belt, slot, player, empties, capacity);
            return;
        }
        int index = BeltContents.lastOccupied(belt, capacity);
        if (index < 0) {
            return;
        }
        ItemStack removed = BeltContents.get(belt, index).copy();
        int before = removed.getCount();
        ItemStack remainder = slot.safeInsert(removed);
        if (remainder.getCount() >= before) {
            return;
        }
        BeltContents.set(belt, index, remainder);
        playRemove(player);
    }

    private static void giveRemoved(ItemStack belt, Slot slot, Player player, ItemStack removed, int capacity) {
        int before = removed.getCount();
        ItemStack remainder = slot.safeInsert(removed);
        if (!remainder.isEmpty()) {
            putBack(belt, remainder, capacity);
        }
        if (remainder.getCount() < before) {
            playRemove(player);
        }
    }

    /** Dose slots hold one item each. Bottle rows on a creeper or dragon belt take a stack. */
    private static void putBack(ItemStack belt, ItemStack remainder, int capacity) {
        if (remainder.is(Items.GLASS_BOTTLE) && BeltContents.bottleSlotCount(belt) > 0) {
            BeltContents.insertFirst(belt, remainder, capacity);
            return;
        }
        for (int index = 0; index < capacity && !remainder.isEmpty(); index++) {
            if (!BeltContents.get(belt, index).isEmpty()) {
                continue;
            }
            BeltContents.set(belt, index, remainder);
            remainder.shrink(1);
        }
    }

    private static void playRemove(Player player) {
        if (!player.level().isClientSide()) {
            player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F + player.level().getRandom().nextFloat() * 0.4F);
        }
    }

    private static void playInsertFail(Player player) {
        // The inventory click runs on the client. playSound there is what the clicking player hears.
        player.playSound(SoundEvents.BUNDLE_INSERT_FAIL, 1.0F, 1.0F);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        int duration = BuffsPotionsConfig.BELT_DRINK_DURATION_TICKS.getAsInt();
        if (BeltEnchantments.hasQuickQuaff(stack)) {
            duration = Math.max(1, duration / 4);
        }
        return duration;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.DRINK;
    }

    private static void openFromSlot(ServerPlayer player, Slot slot) {
        if (slot.container instanceof FallbackBeltContainer) {
            openMenu(player, BeltItemContainer.EQUIPPED);
            return;
        }
        if (slot.container == player.getInventory()) {
            openMenu(player, BeltItemContainer.inventorySource(slot.getContainerSlot()));
            return;
        }
        openMenu(player, slot.container, slot.getContainerSlot());
    }

    private static SimpleMenuProvider menuProvider(int source, Component title) {
        return new SimpleMenuProvider(
                (containerId, inventory, owner) -> new PotionBeltMenu(containerId, inventory, source),
                title
        );
    }

    private static Component title(ItemStack belt) {
        Component custom = belt.get(DataComponents.CUSTOM_NAME);
        if (custom != null) {
            return custom;
        }
        return Component.translatable(belt.getItem().getDescriptionId());
    }

    private static int firstTransferSlot(ItemStack belt) {
        for (int slot = 0; slot < BeltEnchantments.capacity(belt); slot++) {
            ItemStack stored = BeltContents.get(belt, slot);
            if (stored.isEmpty() || BeltContents.isResidue(stored)) {
                return slot;
            }
        }
        return -1;
    }
}
