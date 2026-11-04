package com.buffspotions.item;

import com.buffspotions.belt.BeltContents;
import com.buffspotions.belt.BeltKey;
import com.buffspotions.belt.BeltSource;
import com.buffspotions.flagon.FlagonContents;
import com.buffspotions.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class FlagonBeltItem extends PotionBeltItem {
    public FlagonBeltItem(Properties properties) {
        super(properties);
    }

    /** Splash and lingering belts can throw a dose instead of drinking it. */
    public boolean throwsPotions() {
        return false;
    }

    /** Creeper and Dragon belts remember a drink, splash, or linger mode on the stack. */
    public int modeCount() {
        return 0;
    }

    public int throwMode(ItemStack belt) {
        return 0;
    }

    public boolean hasThrowMode() {
        return modeCount() >= 2;
    }

    public boolean throwsPotions(ItemStack belt) {
        return modeCount() == 0 ? throwsPotions() : throwMode(belt) > 0;
    }

    public boolean lingers(ItemStack belt) {
        return modeCount() == 0 ? lingers() : throwMode(belt) == 2;
    }

    public static void setMode(ItemStack belt, int mode) {
        if (!(belt.getItem() instanceof FlagonBeltItem flagon) || flagon.modeCount() < 2) {
            return;
        }
        belt.set(ModDataComponents.BELT_MODE.get(), Math.clamp(mode, 0, flagon.modeCount() - 1));
    }

    public static void setThrowing(ItemStack belt, boolean throwing) {
        setMode(belt, throwing ? 1 : 0);
    }

    public boolean lingers() {
        return false;
    }

    /**
     * Use Belt on a placed flagon. A worn flagon belt fills its active flagon, or
     * takes the block into the active slot when that slot is empty. A worn potion
     * belt exchanges the active bottle with one dose.
     */
    public static boolean pickup(Player player, Level level, BlockPos pos, ItemStack placed) {
        if (!BeltKey.isDown(player) || !(placed.getItem() instanceof FlagonItem placedItem)) {
            return false;
        }
        ItemStack belt = BeltSource.shown(player);
        if (belt.getItem() instanceof FlagonBeltItem) {
            if (belt.getItem() instanceof FlagonBeltItem throwing && throwing.throwsPotions(belt)) {
                FlagonContents placedContents = placedItem.playerContents(player, placed);
                if (!BeltContents.allowsContents(belt, placedContents)) {
                    if (!level.isClientSide()) {
                        level.playSound(null, pos, SoundEvents.BUNDLE_INSERT_FAIL, SoundSource.PLAYERS, 0.8F, 1.0F);
                    }
                    return true;
                }
            }
            return fillOrTake(player, level, pos, placed, placedItem, belt);
        }
        if (belt.getItem() instanceof PotionBeltItem) {
            return exchangeBottle(player, level, pos, placed, placedItem, belt);
        }
        return false;
    }

    private static boolean fillOrTake(
            Player player,
            Level level,
            BlockPos pos,
            ItemStack placed,
            FlagonItem placedItem,
            ItemStack belt
    ) {
        int active = BeltContents.activeSlot(belt);
        ItemStack stored = BeltContents.get(belt, active);
        if (stored.isEmpty()) {
            if (!level.isClientSide()) {
                BeltContents.set(belt, active, placed.copyWithCount(1));
                level.removeBlock(pos, false);
                BeltSource.saveShown(player, belt);
                level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 1.0F);
            }
            return true;
        }
        if (!(stored.getItem() instanceof FlagonItem storedItem)) {
            return false;
        }
        FlagonContents destination = storedItem.playerContents(player, stored);
        FlagonContents source = placedItem.playerContents(player, placed);
        java.util.UUID storedStore = com.buffspotions.flagon.EnderFlagons.storeId(player, stored);
        java.util.UUID placedStore = com.buffspotions.flagon.EnderFlagons.storeId(player, placed);
        if (storedStore != null && storedStore.equals(placedStore)) {
            return false;
        }
        if (source.isEmpty() || !destination.canAccept(source.dose(), 1)) {
            return false;
        }
        if (!level.isClientSide()) {
            int moved = Math.min(source.doses(), destination.space());
            storedItem.writeContents(player, stored, destination.insert(source.dose(), moved));
            placedItem.writeContents(player, placed, source.remove(moved));
            writePlaced(level, pos, placed);
            BeltContents.set(belt, active, stored);
            BeltSource.saveShown(player, belt);
            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return true;
    }

    private static boolean exchangeBottle(
            Player player,
            Level level,
            BlockPos pos,
            ItemStack placed,
            FlagonItem placedItem,
            ItemStack belt
    ) {
        int active = BeltContents.activeSlot(belt);
        ItemStack bottle = BeltContents.get(belt, active);
        FlagonContents contents = placedItem.playerContents(player, placed);
        if (bottle.is(Items.GLASS_BOTTLE) && !contents.isEmpty()) {
            if (!level.isClientSide()) {
                ItemStack filled = contents.bottledDose();
                placedItem.writeContents(player, placed, contents.removeDose());
                writePlaced(level, pos, placed);
                BeltContents.set(belt, active, filled);
                BeltSource.saveShown(player, belt);
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        if (FlagonContents.isDoseItem(bottle) && contents.canInsertDose(bottle)) {
            if (!level.isClientSide()) {
                placedItem.writeContents(player, placed, contents.insertDose(bottle));
                writePlaced(level, pos, placed);
                BeltContents.set(belt, active, new ItemStack(Items.GLASS_BOTTLE));
                BeltSource.saveShown(player, belt);
                level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return true;
        }
        return false;
    }

    private static void writePlaced(Level level, BlockPos pos, ItemStack placed) {
        if (placed.getItem() instanceof EnderFlagonItem) {
            return;
        }
        if (level.getBlockEntity(pos) instanceof com.buffspotions.flagon.FlagonBlockEntity flagon) {
            flagon.setContents(FlagonContents.of(placed));
        }
    }
}
