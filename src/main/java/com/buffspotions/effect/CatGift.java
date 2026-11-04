package com.buffspotions.effect;

import com.buffspotions.registry.ModItems;
import java.util.function.BiConsumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Luck and Bad Luck change a cat's morning gift. A stronger dose stays at level II.
 * The usual gifts stay in the draw; only the listed weights change.
 */
public final class CatGift {
    public enum Kind {
        VANILLA,
        LUCK_I,
        LUCK_II,
        BAD_I,
        BAD_II
    }

    public enum Gift {
        RABBIT_HIDE,
        RABBIT_FOOT,
        CHICKEN,
        FEATHER,
        ROTTEN_FLESH,
        STRING,
        MEMBRANE,
        WING,
        SHARD,
        BOTTLE,
        ENDERMITE,
        PUFFERFISH
    }

    private CatGift() {
    }

    public static Kind select(int luckAmplifier, int badLuckAmplifier) {
        if (luckAmplifier < 0 && badLuckAmplifier < 0) {
            return Kind.VANILLA;
        }
        if (luckAmplifier >= 0 && badLuckAmplifier >= 0 && luckAmplifier == badLuckAmplifier) {
            return Kind.VANILLA;
        }
        if (luckAmplifier > badLuckAmplifier) {
            return luckAmplifier <= 0 ? Kind.LUCK_I : Kind.LUCK_II;
        }
        return badLuckAmplifier <= 0 ? Kind.BAD_I : Kind.BAD_II;
    }

    /** Phantom membrane is 2 in the base gift. Luck triples it and matches the new extras to that. */
    public static int weight(Kind kind, Gift gift) {
        return switch (gift) {
            case MEMBRANE -> kind == Kind.LUCK_I || kind == Kind.LUCK_II ? 6 : 2;
            case WING -> kind == Kind.LUCK_I || kind == Kind.LUCK_II ? 6 : 0;
            case SHARD, BOTTLE -> kind == Kind.LUCK_II ? 6 : 0;
            case ENDERMITE -> switch (kind) {
                case BAD_I -> 2;
                case BAD_II -> 6;
                default -> 0;
            };
            case PUFFERFISH -> kind == Kind.BAD_II ? 2 : 0;
            default -> 10;
        };
    }

    public static int total(Kind kind) {
        int sum = 0;
        for (Gift gift : Gift.values()) {
            sum += weight(kind, gift);
        }
        return sum;
    }

    public static Gift roll(RandomSource random, Kind kind) {
        int pick = random.nextInt(total(kind));
        for (Gift gift : Gift.values()) {
            int weight = weight(kind, gift);
            if (weight <= 0) {
                continue;
            }
            if (pick < weight) {
                return gift;
            }
            pick -= weight;
        }
        throw new IllegalStateException("cat gift roll missed the table");
    }

    public static void deliver(ServerLevel level, LivingEntity player, Gift gift, BiConsumer<ServerLevel, ItemStack> drop) {
        if (gift == Gift.ENDERMITE) {
            spawn(level, EntityTypes.ENDERMITE, player);
            return;
        }
        if (gift == Gift.PUFFERFISH) {
            spawn(level, EntityTypes.PUFFERFISH, player);
            return;
        }
        drop.accept(level, stack(gift));
    }

    private static ItemStack stack(Gift gift) {
        return switch (gift) {
            case RABBIT_HIDE -> new ItemStack(Items.RABBIT_HIDE);
            case RABBIT_FOOT -> new ItemStack(Items.RABBIT_FOOT);
            case CHICKEN -> new ItemStack(Items.CHICKEN);
            case FEATHER -> new ItemStack(Items.FEATHER);
            case ROTTEN_FLESH -> new ItemStack(Items.ROTTEN_FLESH);
            case STRING -> new ItemStack(Items.STRING);
            case MEMBRANE -> new ItemStack(Items.PHANTOM_MEMBRANE);
            case WING -> new ItemStack(ModItems.WING.get());
            case SHARD -> new ItemStack(Items.AMETHYST_SHARD);
            case BOTTLE -> new ItemStack(Items.EXPERIENCE_BOTTLE);
            case ENDERMITE, PUFFERFISH -> ItemStack.EMPTY;
        };
    }

    /** The mob appears beside the waking player. The cat has already run off to leave an item. */
    private static void spawn(ServerLevel level, EntityType<?> type, LivingEntity player) {
        Entity mob = type.create(level, EntitySpawnReason.TRIGGERED);
        if (mob == null) {
            return;
        }
        mob.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        if (!mob.fudgePositionAfterSizeChange(EntityDimensions.fixed(0.0F, 0.0F))) {
            mob.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
        }
        level.addFreshEntity(mob);
    }
}
