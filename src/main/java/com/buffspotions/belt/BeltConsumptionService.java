package com.buffspotions.belt;

import com.buffspotions.advancement.BeltAdvancements;
import com.buffspotions.config.BuffsPotionsConfig;
import com.buffspotions.equipment.BeltSlotAccess;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.flagon.FlagonInteractions;
import com.buffspotions.item.FlagonBeltItem;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.item.PotionBeltItem;
import com.buffspotions.effect.Formation;
import com.buffspotions.effect.Incarnation;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.registry.ModPotions;
import com.buffspotions.registry.ModDataComponents;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;

public final class BeltConsumptionService {
    public static boolean consume(ServerPlayer player, ItemStack belt, int slot) {
        return consume(player, belt, slot, false);
    }

    public static boolean consume(ServerPlayer player, ItemStack belt, int slot, boolean equipped) {
        return consume(player, belt, slot, equipped, true);
    }

    public static boolean consume(ServerPlayer player, ItemStack belt, int slot, boolean equipped, boolean advanceSelection) {
        if (belt.isEmpty() || player.getCooldowns().isOnCooldown(belt)) {
            return false;
        }
        ItemStack dose = BeltContents.get(belt, slot).copy();
        ItemStack judged = dose.copy();
        boolean gulp = player.getData(ModAttachments.BELT_USE).gulp();
        if (!BeltContents.isDrinkableDose(player, belt, dose, gulp)) {
            return false;
        }

        ItemStack remainder = gulp && gulpHoney(player, dose)
                ? dose
                : BeltDrinkContext.finish(belt, () -> dose.finishUsingItem(player.level(), player));
        BeltContents.set(belt, slot, remainder);
        BeltAdvancements.onDrink(player, belt, judged);
        if (equipped && belt.getItem() instanceof PotionBeltItem && !(belt.getItem() instanceof FlagonBeltItem)) {
            ModCriteria.BELT_DRINK.get().trigger(player);
        }
        return afterUse(player, belt, equipped, advanceSelection && !flagonStillHasDose(player, belt, slot));
    }

    /** A gulp of honey from a flagon applies the dose immediately. Creative keeps the dose. */
    private static boolean gulpHoney(ServerPlayer player, ItemStack dose) {
        if (!(dose.getItem() instanceof FlagonItem flagon)) {
            return false;
        }
        FlagonContents contents = flagon.playerContents(player, dose);
        if (!contents.isHoney() || contents.doses() <= 0) {
            return false;
        }
        FlagonInteractions.applyDose(player, contents.dose());
        if (!player.hasInfiniteMaterials()) {
            flagon.writeContents(player, dose, contents.removeDose());
        }
        return true;
    }

    public static void activateEquipped(ServerPlayer player) {
        if (!BeltSource.held(player).isEmpty()) {
            return;
        }
        ItemStack belt = BeltSource.equipped(player);
        if (belt.isEmpty() || player.getCooldowns().isOnCooldown(belt)) {
            return;
        }
        BeltUseState state = player.getData(ModAttachments.BELT_USE);
        if (state.pendingAdvance()) {
            return;
        }
        int slot = BeltContents.activeSlot(belt);
        ItemStack dose = BeltContents.get(belt, slot);
        if (blockedThrow(player, belt, dose)) {
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
            failThrow(player);
            return;
        }
        if (shouldSplash(player, belt, dose)) {
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
            splashFlagon(player, belt, slot, true);
            return;
        }
        if (BeltContents.isThrowableDose(dose)) {
            if (player.isUsingItem()) {
                player.stopUsingItem();
            }
            throwDose(player, belt, slot, true);
            return;
        }
        if (!BeltContents.isDrinkableDose(player, belt, dose)) {
            return;
        }
        if (state.isDrinking() && state.slot() == slot) {
            return;
        }
        if (player.isUsingItem()) {
            player.stopUsingItem();
        }
        state.cancel();
        state.clearAdvance();
        state.start(slot);
        player.syncData(ModAttachments.BELT_USE);
    }

    public static boolean throwDose(ServerPlayer player, ItemStack belt, int slot, boolean equipped) {
        if (belt.isEmpty() || player.getCooldowns().isOnCooldown(belt)) {
            return false;
        }
        ItemStack dose = BeltContents.get(belt, slot);
        if (!BeltContents.isThrowableDose(dose)) {
            return false;
        }

        boolean beltInMain = player.getMainHandItem() == belt;
        boolean beltInOff = player.getOffhandItem() == belt;
        InteractionHand hand = beltInOff && !beltInMain ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack previous = player.getItemInHand(hand);
        ItemStack projectile = dose.copyWithCount(1);
        player.setItemInHand(hand, projectile);
        InteractionResult result = projectile.getItem().use(player.level(), player, hand);
        player.setItemInHand(hand, previous);
        player.swing(hand, true);
        if (!result.consumesAction()) {
            return false;
        }

        BeltContents.set(belt, slot, ItemStack.EMPTY);
        return afterUse(player, belt, equipped, true);
    }

    public static boolean shouldSplash(Player player, ItemStack belt, ItemStack dose) {
        if (!(belt.getItem() instanceof FlagonBeltItem flagonBelt) || !flagonBelt.throwsPotions(belt)) {
            return false;
        }
        if (!(dose.getItem() instanceof FlagonItem flagon) || !flagon.hasDrinkableDose(player, dose)) {
            return false;
        }
        FlagonContents contents = flagon.playerContents(player, dose);
        if (contents.isMilk()) {
            return true;
        }
        ItemStack potion = contents.dose();
        return potion.get(DataComponents.POTION_CONTENTS) != null && canThrow(potion, flagonBelt.lingers(belt));
    }

    /** Rain and sun cannot be thrown or put on an arrow. Incarnation and formation can be splashed, and cannot linger. */
    public static boolean canTipArrow(ItemStack dose, boolean lingering) {
        if (dose.get(DataComponents.POTION_CONTENTS) == null || isWeatherCalling(dose)) {
            return false;
        }
        return !lingering || !cannotLinger(dose);
    }
    static boolean canThrow(ItemStack dose, boolean lingering) {
        if (isWeatherCalling(dose)) {
            return false;
        }
        return !lingering || !cannotLinger(dose);
    }

    private static boolean cannotLinger(ItemStack dose) {
        return Incarnation.isIncarnation(dose) || Formation.isFormation(dose);
    }

    public static boolean isWeatherCalling(ItemStack dose) {
        return dose.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)
                .potion()
                .map(potion -> potion.unwrapKey().equals(ModPotions.RAIN_CALLING.unwrapKey())
                        || potion.unwrapKey().equals(ModPotions.SUN_CALLING.unwrapKey()))
                .orElse(false);
    }

    /** A throw-mode dose that must refuse instead of drinking. */
    public static boolean blockedThrow(Player player, ItemStack belt, ItemStack stored) {
        if (!(belt.getItem() instanceof FlagonBeltItem flagonBelt) || !flagonBelt.throwsPotions(belt)) {
            return false;
        }
        if (!(stored.getItem() instanceof FlagonItem flagon) || !flagon.hasDrinkableDose(player, stored)) {
            return false;
        }
        FlagonContents contents = flagon.playerContents(player, stored);
        if (contents.isMilk()) {
            return false;
        }
        if (contents.isHoney() || isWeatherCalling(contents.dose())) {
            return true;
        }
        return cannotLinger(contents.dose()) && flagonBelt.lingers(belt);
    }

    public static void failThrow(ServerPlayer player) {
        player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.BUNDLE_INSERT_FAIL,
                SoundSource.PLAYERS,
                0.8F,
                1.0F
        );
    }

    public static boolean splashFlagon(ServerPlayer player, ItemStack belt, int slot, boolean equipped) {
        if (belt.isEmpty() || player.getCooldowns().isOnCooldown(belt)) {
            return false;
        }
        ItemStack stored = BeltContents.get(belt, slot);
        if (!shouldSplash(player, belt, stored)) {
            return false;
        }
        if (!BeltContents.hasBottle(belt)) {
            player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.BUNDLE_INSERT_FAIL,
                    SoundSource.PLAYERS,
                    0.8F,
                    1.0F
            );
            return false;
        }

        FlagonItem flagon = (FlagonItem) stored.getItem();
        FlagonContents contents = flagon.playerContents(player, stored);
        boolean lingering = belt.getItem() instanceof FlagonBeltItem flagonBelt && flagonBelt.lingers(belt);
        Item bottle = lingering ? Items.LINGERING_POTION : Items.SPLASH_POTION;
        ItemStack dose = contents.dose();
        ItemStack projectile = new ItemStack(bottle);
        PotionContents potion = dose.get(DataComponents.POTION_CONTENTS);
        if (potion != null) {
            projectile.set(DataComponents.POTION_CONTENTS, BeltEnchantments.modifiedContents(potion, belt));
        }
        BeltEnchantments.copyDoseMarkers(dose, projectile);

        boolean beltInMain = player.getMainHandItem() == belt;
        boolean beltInOff = player.getOffhandItem() == belt;
        InteractionHand hand = beltInOff && !beltInMain ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack previous = player.getItemInHand(hand);
        player.setItemInHand(hand, projectile);
        InteractionResult result = projectile.getItem().use(player.level(), player, hand);
        player.setItemInHand(hand, previous);
        player.swing(hand, true);
        if (!result.consumesAction()) {
            return false;
        }

        if (!player.hasInfiniteMaterials()) {
            BeltContents.consumeBottle(player, belt);
            ItemStack updated = stored.copy();
            flagon.writeContents(player, updated, contents.removeDose());
            BeltContents.set(belt, slot, updated);
        }
        return afterUse(player, belt, equipped, !flagonStillHasDose(player, belt, slot));
    }

    private static boolean flagonStillHasDose(ServerPlayer player, ItemStack belt, int slot) {
        if (!(belt.getItem() instanceof FlagonBeltItem)) {
            return false;
        }
        ItemStack left = BeltContents.get(belt, slot);
        return left.getItem() instanceof FlagonItem flagon && flagon.hasDrinkableDose(player, left);
    }

    private static boolean afterUse(ServerPlayer player, ItemStack belt, boolean equipped, boolean advanceSelection) {
        List<ItemStack> contents = BeltContents.copyOccupied(belt, BeltContents.MAX_SLOTS);
        damage(player, belt);
        if (belt.isEmpty()) {
            for (ItemStack stored : contents) {
                player.drop(stored, true);
            }
            if (equipped) {
                BeltSlotAccess.setEquipped(player, ItemStack.EMPTY);
            }
            BeltUseState state = player.getData(ModAttachments.BELT_USE);
            state.clearAdvance();
            player.syncData(ModAttachments.BELT_USE);
            return true;
        }

        player.getCooldowns().addCooldown(belt, cooldownTicks(belt));
        if (advanceSelection) {
            advanceSelection(player, belt);
        } else {
            publish(player, belt);
        }
        if (equipped) {
            BeltSlotAccess.setEquipped(player, belt);
        }
        return true;
    }

    public static void advanceSelection(ServerPlayer player, ItemStack belt) {
        int next = BeltContents.nextDrinkableInRow(
                player,
                belt,
                BeltContents.activeSlot(belt),
                BeltEnchantments.rowStart(belt),
                BeltContents.BASE_SLOTS
        );
        if (next >= 0) {
            BeltContents.setActiveSlot(belt, next);
        }
        publish(player, belt);
    }

    /**
     * A belt taken from a chest can share its last sent snapshot with the stack in hand.
     * Changing a dose then looks unchanged, so the hotbar keeps the old picture until a full resync.
     */
    public static void publish(ServerPlayer player, ItemStack belt) {
        publishMenu(player.containerMenu, belt);
        if (player.inventoryMenu != player.containerMenu) {
            publishMenu(player.inventoryMenu, belt);
        }
    }

    private static void publishMenu(AbstractContainerMenu menu, ItemStack belt) {
        for (int index = 0; index < menu.slots.size(); index++) {
            Slot slot = menu.getSlot(index);
            if (slot.getItem() == belt) {
                menu.setRemoteSlot(index, ItemStack.EMPTY);
            }
        }
        menu.broadcastChanges();
    }

    public static ItemStack beltAwaitingAdvance(ServerPlayer player, int advanceId) {
        if (advanceId == 0) {
            return ItemStack.EMPTY;
        }
        ItemStack equipped = BeltSlotAccess.getEquipped(player);
        if (marked(equipped, advanceId)) {
            return equipped;
        }
        ItemStack main = player.getMainHandItem();
        if (marked(main, advanceId)) {
            return main;
        }
        ItemStack offhand = player.getOffhandItem();
        if (marked(offhand, advanceId)) {
            return offhand;
        }
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (marked(stack, advanceId)) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void clearAdvanceMarks(ServerPlayer player) {
        strip(BeltSlotAccess.getEquipped(player));
        strip(player.getMainHandItem());
        strip(player.getOffhandItem());
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            strip(player.getInventory().getItem(slot));
        }
    }

    private static boolean marked(ItemStack stack, int advanceId) {
        return stack.getItem() instanceof PotionBeltItem
                && stack.getOrDefault(ModDataComponents.BELT_ADVANCE.get(), 0) == advanceId;
    }

    private static void strip(ItemStack stack) {
        if (stack.getItem() instanceof PotionBeltItem && stack.has(ModDataComponents.BELT_ADVANCE.get())) {
            stack.remove(ModDataComponents.BELT_ADVANCE.get());
        }
    }

    public static int cooldownTicks(ItemStack belt) {
        int cooldown = BuffsPotionsConfig.BELT_COOLDOWN_TICKS.getAsInt();
        if (BeltEnchantments.hasQuickQuaff(belt) && cooldown > 0) {
            cooldown = Math.max(1, cooldown / 4);
        }
        return cooldown;
    }

    private static void damage(ServerPlayer player, ItemStack belt) {
        if (player.getMainHandItem() == belt) {
            belt.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
            return;
        }
        if (player.getOffhandItem() == belt) {
            belt.hurtAndBreak(1, player, InteractionHand.OFF_HAND);
            return;
        }
        belt.hurtAndBreak(1, player.level(), player, broken -> player.level().playSound(
                null,
                player.blockPosition(),
                SoundEvents.ITEM_BREAK.value(),
                SoundSource.PLAYERS,
                0.8F,
                1.0F
        ));
    }

    private BeltConsumptionService() {
    }
}
