package com.buffspotions.belt;

import com.buffspotions.effect.Formation;
import com.buffspotions.effect.Incarnation;
import com.buffspotions.equipment.BeltSlotAccess;
import com.buffspotions.registry.ModCriteria;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.item.FlagonBeltItem;
import com.buffspotions.item.FlagonItem;
import com.buffspotions.mixin.AbstractArrowInvoker;
import com.buffspotions.mixin.ThrownPotionHitInvoker;
import com.buffspotions.registry.ModAttachments;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public final class BeltTipping {
    private static final Shot NONE = new Shot(-1, -1);
    private static final ThreadLocal<Shot> SHOT = new ThreadLocal<>();
    private static final ConcurrentHashMap<UUID, Shot> PENDING = new ConcurrentHashMap<>();

    /** The client’s flagon and mode at the click that fires a charged crossbow. */
    public static void pendingShot(UUID player, int mode, int slot) {
        PENDING.put(player, new Shot(mode, slot));
    }

    public static void beginShot(LivingEntity shooter) {
        if (!(shooter instanceof ServerPlayer player)) {
            SHOT.set(NONE);
            return;
        }
        Shot pending = PENDING.remove(player.getUUID());
        ItemStack belt = BeltSlotAccess.getEquipped(player);
        if (pending != null && pending.mode >= 0 && belt.getItem() instanceof FlagonBeltItem && BeltEnchantments.hasTipping(belt)) {
            FlagonBeltItem.setMode(belt, pending.mode);
            BeltContents.setActiveSlot(belt, pending.slot);
            BeltSlotAccess.setEquipped(player, belt);
            belt = BeltSlotAccess.getEquipped(player);
        }
        if (!(belt.getItem() instanceof FlagonBeltItem flagonBelt) || !BeltEnchantments.hasTipping(belt)) {
            SHOT.set(NONE);
            return;
        }
        SHOT.set(new Shot(flagonBelt.throwMode(belt), BeltContents.activeSlot(belt)));
    }

    public static void endShot() {
        SHOT.remove();
    }

    public static void discardPending(UUID player) {
        PENDING.remove(player);
    }

    public static void tip(AbstractArrow arrow, LivingEntity shooter, ItemStack ammo) {
        if (!(shooter instanceof ServerPlayer player)) {
            return;
        }
        if (!ammo.is(Items.ARROW) && !ammo.is(Items.SPECTRAL_ARROW)) {
            return;
        }
        ItemStack belt = BeltSlotAccess.getEquipped(player);
        Shot shot = SHOT.get();
        if (shot == NONE) {
            return;
        }
        if (!(belt.getItem() instanceof FlagonBeltItem flagonBelt) || !BeltEnchantments.hasTipping(belt)) {
            return;
        }
        int mode = shot != null ? shot.mode : flagonBelt.throwMode(belt);
        if (mode <= 0 || !BeltContents.hasBottle(belt)) {
            return;
        }
        int slot = shot != null ? shot.slot : BeltContents.activeSlot(belt);
        ItemStack stored = BeltContents.get(belt, slot);
        if (!(stored.getItem() instanceof FlagonItem flagon) || !flagon.hasDrinkableDose(player, stored)) {
            return;
        }
        FlagonContents contents = flagon.playerContents(player, stored);
        ItemStack dose = contents.dose();
        boolean linger = mode == 2;
        if (!BeltConsumptionService.canTipArrow(dose, linger)) {
            return;
        }
        if (linger) {
            ItemStack tipped = tippedArrow(dose, belt);
            arrow.setData(ModAttachments.ARROW_TIP.get(), tipped);
            if (!(arrow instanceof SpectralArrow)) {
                ((AbstractArrowInvoker) arrow).buffsPotions$setPickupItemStack(tipped);
            }
            if (player.hasInfiniteMaterials() || player.getRandom().nextInt(8) != 0) {
                return;
            }
        } else {
            arrow.setData(ModAttachments.ARROW_SPLASH.get(), splashDose(dose, belt));
            if (player.hasInfiniteMaterials()) {
                return;
            }
        }
        consume(player, belt, slot, stored, flagon, contents);
    }

    public static void land(AbstractArrow arrow, HitResult hit) {
        ItemStack splash = arrow.getData(ModAttachments.ARROW_SPLASH.get());
        if (splash.isEmpty() || !(arrow.level() instanceof ServerLevel level)) {
            return;
        }
        arrow.setData(ModAttachments.ARROW_SPLASH.get(), ItemStack.EMPTY);
        boolean reincarnate = Incarnation.isIncarnation(splash) && arrow.getOwner() instanceof ServerPlayer;
        if (reincarnate) {
            Incarnation.watchSpawn();
        }
        if (arrow.isOnFire()) {
            Formation.light();
        }
        ThrownSplashPotion potionEntity = arrow.getOwner() instanceof LivingEntity owner
                ? new ThrownSplashPotion(level, owner, splash)
                : new ThrownSplashPotion(level, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, splash);
        potionEntity.setPos(hit.getLocation());
        PotionContents potion = splash.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        ThrownPotionHitInvoker thrown = (ThrownPotionHitInvoker) potionEntity;
        try {
            if (potion.is(Potions.WATER)) {
                thrown.buffsPotions$onHitAsWater(level);
                if (hit instanceof BlockHitResult blockHit) {
                    Direction direction = blockHit.getDirection();
                    BlockPos hitPos = blockHit.getBlockPos();
                    thrown.buffsPotions$dowseFire(hitPos);
                    thrown.buffsPotions$dowseFire(hitPos.relative(direction));
                    for (Direction adjacent : Direction.Plane.HORIZONTAL) {
                        thrown.buffsPotions$dowseFire(hitPos.relative(direction).relative(adjacent));
                    }
                }
            } else if (potion.hasEffects()) {
                potionEntity.onHitAsPotion(level, splash, hit);
            }
        } finally {
            Formation.clear();
            if (reincarnate && Incarnation.takeSpawn() && arrow.getOwner() instanceof ServerPlayer player) {
                ModCriteria.MOBS_ON_DEMAND.get().trigger(player);
            }
        }
        int event = potion.potion().isPresent() && potion.potion().get().value().hasInstantEffects() ? 2007 : 2002;
        level.levelEvent(event, BlockPos.containing(hit.getLocation()), potion.getColor());
    }

    public static ItemStack tippedArrow(ItemStack dose, ItemStack belt) {
        ItemStack arrow = new ItemStack(Items.TIPPED_ARROW);
        PotionContents potion = dose.get(DataComponents.POTION_CONTENTS);
        if (potion != null) {
            arrow.set(DataComponents.POTION_CONTENTS, BeltEnchantments.modifiedContents(potion, belt));
        }
        arrow.set(DataComponents.POTION_DURATION_SCALE, BeltEnchantments.TIPPED_ARROW_DURATION_SCALE);
        BeltEnchantments.copyDoseMarkers(dose, arrow);
        return arrow;
    }

    private static ItemStack splashDose(ItemStack dose, ItemStack belt) {
        ItemStack splash = new ItemStack(Items.SPLASH_POTION);
        PotionContents potion = dose.get(DataComponents.POTION_CONTENTS);
        if (potion != null) {
            splash.set(DataComponents.POTION_CONTENTS, BeltEnchantments.modifiedContents(potion, belt));
        }
        BeltEnchantments.copyDoseMarkers(dose, splash);
        return splash;
    }

    private static void consume(ServerPlayer player, ItemStack belt, int slot, ItemStack stored, FlagonItem flagon, FlagonContents contents) {
        BeltContents.consumeBottle(player, belt);
        ItemStack updated = stored.copy();
        flagon.writeContents(player, updated, contents.removeDose());
        BeltContents.set(belt, slot, updated);
        BeltSlotAccess.setEquipped(player, belt);
    }

    private record Shot(int mode, int slot) {
    }

    private BeltTipping() {
    }
}
