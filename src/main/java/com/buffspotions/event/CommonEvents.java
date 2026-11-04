package com.buffspotions.event;

import com.buffspotions.advancement.BeltAdvancements;
import com.buffspotions.BuffsPotions;
import com.buffspotions.equipment.BeltSlotAccess;
import com.buffspotions.belt.BeltClickPriority;
import com.buffspotions.belt.BeltKey;
import com.buffspotions.belt.BeltConsumptionService;
import com.buffspotions.belt.BeltContents;
import com.buffspotions.belt.BeltEnchantments;
import com.buffspotions.belt.BeltSource;
import com.buffspotions.belt.BeltTipping;
import com.buffspotions.belt.BeltUseState;
import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModEffects;
import com.buffspotions.registry.ModItems;
import com.buffspotions.equipment.BeltSlotData;
import com.buffspotions.registry.ModDataComponents;
import com.buffspotions.flagon.EnderFlagons;
import com.buffspotions.flagon.FlagonAnchor;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.AnchoredEnderFlagonItem;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.skin.WorldSkins;
import com.buffspotions.stand.StandRecipeUnlocks;
import com.buffspotions.still.StillRecipeUnlocks;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.TriState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.buffspotions.bellows.BellowsBoosts;
import com.buffspotions.bellows.CopperGolemBellows;
import com.buffspotions.item.BellowsItem;
import com.buffspotions.item.PotionBeltItem;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.buffspotions.bellows.BellowsClaims;
import com.buffspotions.effect.WeatherCalls;
import net.minecraft.world.entity.animal.golem.CopperGolem;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

@EventBusSubscriber(modid = BuffsPotions.MOD_ID)
public final class CommonEvents {
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()
                || keepInventory(event.getOriginal())) {
            ItemStack belt = event.getOriginal().getData(ModAttachments.BELT_SLOT).stack();
            if (!belt.isEmpty()) {
                event.getEntity().setData(ModAttachments.BELT_SLOT, new BeltSlotData(belt.copy()));
            }
        }
        event.getEntity().setData(
                ModAttachments.HELD_LATENT,
                event.getOriginal().getData(ModAttachments.HELD_LATENT)
        );
        event.getEntity().setData(
                ModAttachments.HELD_ITEMS,
                new java.util.HashSet<>(event.getOriginal().getData(ModAttachments.HELD_ITEMS))
        );
        event.getEntity().setData(
                ModAttachments.KNOWN_EFFECTS,
                new java.util.HashSet<>(event.getOriginal().getData(ModAttachments.KNOWN_EFFECTS))
        );
        event.getEntity().setData(
                ModAttachments.KNOWN_EFFECTS_SEEDED,
                event.getOriginal().getData(ModAttachments.KNOWN_EFFECTS_SEEDED)
        );
    }

    @SubscribeEvent
    public static void onPlayerDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || BeltSlotAccess.usesCurios()
                || keepInventory(player)) {
            return;
        }
        BeltSlotData data = player.getData(ModAttachments.BELT_SLOT);
        if (!data.stack().isEmpty()) {
            event.getDrops().add(new ItemEntity(
                    player.level(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    data.stack().copy()
            ));
            data.set(ItemStack.EMPTY);
        }
    }

    @SubscribeEvent
    public static void onBatDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Bat bat)
                || !(bat.level() instanceof ServerLevel level)
                || !level.getGameRules().get(GameRules.MOB_DROPS)) {
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            if (drop.getItem().is(ModItems.WING.get())) {
                return;
            }
        }
        int count = 1;
        int looting = Math.min(lootingLevel(event.getSource()), 3);
        if (bat.getRandom().nextFloat() < 0.25F * (looting + 1)) {
            count++;
        }
        ItemEntity wings = new ItemEntity(level, bat.getX(), bat.getY(), bat.getZ(), new ItemStack(ModItems.WING.get(), count));
        wings.setDefaultPickUpDelay();
        event.getDrops().add(wings);
    }

    private static int lootingLevel(DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return 0;
        }
        var enchantments = attacker.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        return EnchantmentHelper.getEnchantmentLevel(enchantments.getOrThrow(Enchantments.LOOTING), attacker);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        stopHeldConsumableWhileSneaking(player);
        BeltAdvancements.survey(player);
        var state = player.getData(ModAttachments.BELT_USE);
        if (state.isDrinking()) {
            ItemStack belt = BeltSource.drinking(player);
            if (belt.isEmpty()
                    || player.getMainHandItem().getItem() instanceof PotionBeltItem
                    || (!state.gulp() && state.slot() != BeltContents.activeSlot(belt))
                    || !BeltContents.isDrinkableDose(player, belt, BeltContents.get(belt, state.slot()), state.gulp())) {
                state.cancel();
                state.clearAdvance();
                player.syncData(ModAttachments.BELT_USE);
            } else {
                int elapsed = state.tick();
                ItemStack dose = BeltContents.get(belt, state.slot());
                float volume = state.gulp() ? PotionBeltItem.GULP_DRINK_VOLUME : PotionBeltItem.DRINK_VOLUME;
                PotionBeltItem.playDrinkSound(player, dose, elapsed, volume);
                if (elapsed >= belt.getUseDuration(player)) {
                    boolean advance = !state.gulp();
                    BeltConsumptionService.consume(player, belt, state.slot(), state.inventorySlot() < 0, advance);
                    state.cancel();
                }
                player.syncData(ModAttachments.BELT_USE);
            }
        }
        if (!state.isDrinking() && state.pendingAdvance()) {
            advancePending(player, state);
        }
        if (!state.isDrinking()) {
            tryGulp(player);
        }
        com.buffspotions.effect.BoundlessNight.tick(player);
        com.buffspotions.recipebook.RecipeMystery.survey(player);
        StillRecipeUnlocks.survey(player);
        StandRecipeUnlocks.survey(player);
        com.buffspotions.stack.StackRecipeUnlocks.survey(player);
        BeltTipping.discardPending(player.getUUID());
    }

    private static void tryGulp(ServerPlayer player) {
        if (!BeltSource.held(player).isEmpty()
                || player.isUsingItem()
                || player.getData(ModAttachments.BELT_USE).pendingAdvance()) {
            return;
        }
        int inventorySlot = -1;
        int doseSlot = -1;
        for (int slot = 9; slot < 36; slot++) {
            ItemStack stored = player.getInventory().getItem(slot);
            if (!gulpCandidate(player, stored)) {
                continue;
            }
            var found = BeltEnchantments.gulpSlot(player, stored);
            if (found.isEmpty()) {
                continue;
            }
            inventorySlot = slot;
            doseSlot = found.get();
            break;
        }
        if (doseSlot < 0) {
            return;
        }
        var state = player.getData(ModAttachments.BELT_USE);
        state.cancel();
        state.clearAdvance();
        state.startGulp(doseSlot, inventorySlot);
        player.syncData(ModAttachments.BELT_USE);
    }

    private static boolean gulpCandidate(ServerPlayer player, ItemStack belt) {
        return belt.getItem() instanceof PotionBeltItem
                && !player.getCooldowns().isOnCooldown(belt)
                && BeltEnchantments.hasGulpReflex(belt);
    }

    private static void advancePending(ServerPlayer player, BeltUseState state) {
        ItemStack belt = BeltConsumptionService.beltAwaitingAdvance(player, state.advanceBelt());
        state.clearAdvance();
        if (!belt.isEmpty()) {
            belt.remove(ModDataComponents.BELT_ADVANCE.get());
            BeltConsumptionService.advanceSelection(player, belt);
            if (belt == BeltSlotAccess.getEquipped(player)) {
                BeltSlotAccess.setEquipped(player, belt);
            }
        }
        player.syncData(ModAttachments.BELT_USE);
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().getItem() instanceof PotionBeltItem) {
            PotionBeltItem.appendBeltTooltip(event.getItemStack(), event.getToolTip());
        }
        if (event.getItemStack().is(Items.MILK_BUCKET)) {
            PotionContents.addPotionTooltip(
                    java.util.List.of(new MobEffectInstance(ModEffects.MILK, 1)),
                    event.getToolTip()::add,
                    1.0F,
                    event.getContext().tickRate()
            );
        }
    }

    @SubscribeEvent
    public static void onItemUseStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (player.getData(ModAttachments.BELT_USE).isDrinking()) {
            event.setCanceled(true);
            return;
        }
        ItemStack belt = BeltSource.equipped(player);
        if (BeltEnchantments.hasQuickQuaff(belt) && quickDrink(event.getItem())) {
            event.setDuration(Math.max(1, event.getDuration() / 4));
        }
        if (shiftBlocksHeldConsumable(player) && event.getItem().has(DataComponents.CONSUMABLE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getLevel().isClientSide()
                || !(event.getEntity() instanceof ServerPlayer player)
                || !BeltKey.isDown(player)
                || event.getItemStack().getItem() instanceof PotionBeltItem) {
            return;
        }
        ItemStack belt = BeltSource.shown(player);
        if (!PotionBeltItem.canDrink(player, belt)) {
            return;
        }
        double reach = Math.max(player.blockInteractionRange(), player.entityInteractionRange());
        if (BeltClickPriority.passes(player, player.pick(reach, 1.0F, false), event.getItemStack())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
        if (player.getOffhandItem() == belt) {
            belt.getItem().use(player.level(), player, InteractionHand.OFF_HAND);
        } else if (player.getMainHandItem() != belt) {
            BeltConsumptionService.activateEquipped(player);
        }
    }

    @SubscribeEvent
    public static void onBellowsGive(PlayerInteractEvent.EntityInteract event) {
        CopperGolemBellows.give(event);
    }

    @SubscribeEvent
    public static void onBellowsBlow(PlayerInteractEvent.EntityInteract event) {
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof BellowsItem) || event.getEntity().getCooldowns().isOnCooldown(held)) {
            return;
        }
        if (!(event.getTarget() instanceof Mob mob)
                || mob instanceof CopperGolem
                || mob instanceof IronGolem) {
            return;
        }
        if (!event.getLevel().isClientSide()) {
            BellowsBoosts.blow(event.getEntity(), mob);
            event.getEntity().getCooldowns().addCooldown(held, BellowsBoosts.COOLDOWN_TICKS);
            event.getEntity().causeFoodExhaustion(BellowsBoosts.USE_EXHAUSTION);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onCopperGolemTick(EntityTickEvent.Post event) {
        CopperGolemBellows.tick(event.getEntity());
    }

    @SubscribeEvent
    public static void onAnchoredFlagonItem(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof ItemEntity item) || !(item.level() instanceof ServerLevel server)) {
            return;
        }
        ItemStack stack = item.getItem();
        if (!(stack.getItem() instanceof AnchoredEnderFlagonItem)) {
            return;
        }
        FlagonAnchor anchor = stack.get(ModDataComponents.FLAGON_ANCHOR.get());
        if (anchor == null) {
            return;
        }
        FlagonContents live = EnderFlagons.contents(server.getServer(), anchor.player());
        String known = EnderFlagons.knownName(server.getServer(), anchor.player());
        if (FlagonContents.of(stack).sameFill(live) && known.equals(anchor.name())) {
            return;
        }
        ItemStack updated = stack.copy();
        updated.set(ModDataComponents.FLAGON_CONTENTS.get(), live);
        updated.set(ModDataComponents.FLAGON_ANCHOR.get(), new FlagonAnchor(anchor.player(), known));
        item.setItem(updated);
    }

    @SubscribeEvent
    public static void onCopperGolemLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof CopperGolem golem && event.getLevel() instanceof ServerLevel level) {
            BellowsClaims.releaseAll(level, golem.getUUID());
        }
    }

    @SubscribeEvent
    public static void onBellowsUse(PlayerInteractEvent.RightClickBlock event) {
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof BellowsItem) || event.getEntity().isSecondaryUseActive()) {
            return;
        }
        if (event.getLevel().isClientSide()) {
            if (!event.getEntity().getCooldowns().isOnCooldown(held)
                    && BellowsBoosts.canUseOn(event.getLevel().getBlockState(event.getPos()))) {
                event.setUseBlock(TriState.FALSE);
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
            return;
        }
        if (event.getEntity().getCooldowns().isOnCooldown(held)) {
            return;
        }
        if (!BellowsBoosts.tryUse(event.getEntity(), event.getLevel(), event.getPos(), held)) {
            return;
        }
        event.setUseBlock(TriState.FALSE);
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (takeHiveHoney(event)) {
            return;
        }
        BeltClickPriority.keepFlagonClick(event);
        BeltClickPriority.preferBlock(event);
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof PotionBeltItem)
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof BrewingStandBlockEntity stand)
                || event.getEntity().isSecondaryUseActive()) {
            return;
        }
        if (!event.getLevel().isClientSide() && PotionBeltItem.fillFromBrewingStand(held, stand)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        } else if (event.getLevel().isClientSide() && PotionBeltItem.canFillFromBrewingStand(held, stand)) {
            event.setCancellationResult(InteractionResult.SUCCESS);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        WeatherCalls.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        WeatherCalls.forget(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BeltSlotAccess.migrateToCurios(player);
            EnderFlagons.adopt(player);
            EnderFlagons.refreshPlayerName(player.level().getServer(), player.getUUID());
            com.buffspotions.recipebook.RecipeMystery.survey(player);
            StillRecipeUnlocks.survey(player);
            StandRecipeUnlocks.survey(player);
            com.buffspotions.stack.StackRecipeUnlocks.survey(player);
            WorldSkins.get(player.level().getServer()).sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack held = event.getItemStack();
        if (!(event.getTarget() instanceof Cow) || !held.is(Items.GLASS_BOTTLE)) {
            return;
        }

        Player player = event.getEntity();
        if (!player.level().isClientSide()) {
            ItemStack filled = ItemUtils.createFilledResult(
                    held,
                    player,
                    FlagonContents.milkTemplate()
            );
            player.setItemInHand(event.getHand(), filled);
            player.level().playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.COW_MILK,
                    SoundSource.PLAYERS,
                    1.0F,
                    1.0F
            );
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }

    private static boolean quickDrink(ItemStack stack) {
        if (stack.getItem() instanceof ThrowablePotionItem || !stack.has(DataComponents.CONSUMABLE)) {
            return false;
        }
        return stack.getItem() instanceof PotionItem
                || stack.getItem() instanceof FlagonItem
                || stack.is(Items.HONEY_BOTTLE)
                || stack.is(Items.MILK_BUCKET);
    }

    private static boolean takeHiveHoney(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!BeltKey.isDown(player)) {
            return false;
        }
        ItemStack belt = BeltSource.shown(player);
        if (!(belt.getItem() instanceof PotionBeltItem) || belt.getItem() instanceof com.buffspotions.item.FlagonBeltItem) {
            return false;
        }
        int active = BeltContents.activeSlot(belt);
        if (!BeltContents.get(belt, active).is(Items.GLASS_BOTTLE)) {
            return false;
        }
        var state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof BeehiveBlock) || state.getValue(BeehiveBlock.HONEY_LEVEL) < 5) {
            return false;
        }
        if (!event.getLevel().isClientSide()) {
            BeltContents.set(belt, active, new ItemStack(Items.HONEY_BOTTLE));
            BeltSource.saveShown(player, belt);
            event.getLevel().setBlock(event.getPos(), state.setValue(BeehiveBlock.HONEY_LEVEL, 0), 3);
            event.getLevel().playSound(null, event.getPos(), SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        return true;
    }

    private static void stopHeldConsumableWhileSneaking(ServerPlayer player) {
        if (!player.isUsingItem() || !shiftBlocksHeldConsumable(player)) {
            return;
        }
        ItemStack using = player.getUseItem();
        if (using.has(DataComponents.CONSUMABLE) && !(using.getItem() instanceof PotionBeltItem)) {
            player.stopUsingItem();
        }
    }

    private static boolean shiftBlocksHeldConsumable(Player player) {
        if (!BeltKey.isDown(player) || !BeltSource.held(player).isEmpty()) {
            return false;
        }
        return PotionBeltItem.canDrink(player, BeltSource.shown(player));
    }

    private static boolean keepInventory(Player player) {
        return player.level() instanceof ServerLevel serverLevel
                && serverLevel.getGameRules().get(GameRules.KEEP_INVENTORY);
    }

    private CommonEvents() {
    }
}
