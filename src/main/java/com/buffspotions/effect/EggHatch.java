package com.buffspotions.effect;

import com.buffspotions.registry.ModAttachments;
import com.buffspotions.registry.ModCriteria;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.level.Level;

/** Luck and Bad Luck change what a thrown egg hatches. A stronger dose stays at level II. */
public final class EggHatch {
    public enum Kind {
        VANILLA,
        LUCK_I,
        LUCK_II,
        BAD_I,
        BAD_II
    }

    public record Result(int chickens, boolean silverfish, boolean creeper) {
        public static Result chickens(int count) {
            return new Result(count, false, false);
        }
    }

    private EggHatch() {
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

    public static Result roll(RandomSource random, Kind kind) {
        return switch (kind) {
            case VANILLA -> Result.chickens(0);
            case LUCK_I -> luck(random, 4, 16, 8, 0);
            case LUCK_II -> luck(random, 2, 8, 4, 8);
            case BAD_I -> bad(random, 16, 16, 0);
            case BAD_II -> bad(random, 64, 8, 64);
        };
    }

    public static void spawn(ThrownEgg egg, Result result) {
        Level level = egg.level();
        int chicks = 0;
        if (result.chickens > 0) {
            var variant = egg.getItem().get(DataComponents.CHICKEN_VARIANT);
            for (int i = 0; i < result.chickens; i++) {
                boolean flock = result.chickens() >= 64;
                if (!place(level, EntityTypes.CHICKEN, egg, chicken -> {
                    ((Chicken) chicken).setAge(-24000);
                    Optional.ofNullable(variant).ifPresent(((Chicken) chicken)::setVariant);
                    if (flock) {
                        chicken.setData(ModAttachments.NO_CRAMMING, true);
                    }
                })) {
                    break;
                }
                chicks++;
            }
        }
        if (result.silverfish) {
            place(level, EntityTypes.SILVERFISH, egg, mob -> { });
        }
        boolean creeper = result.creeper && place(level, EntityTypes.CREEPER, egg, mob -> { });
        if (egg.getOwner() instanceof ServerPlayer player) {
            if (chicks >= 64) {
                ModCriteria.DONT_COUNT_YOUR_CHICKENS.get().trigger(player);
            }
            if (creeper) {
                ModCriteria.BEFORE_THEY_HATCH.get().trigger(player);
            }
        }
    }

    /**
     * Each upgrade is rolled only after the previous one hit. A zero chance skips that step.
     */
    private static Result luck(RandomSource random, int first, int four, int sixteen, int sixtyFour) {
        if (random.nextInt(first) != 0) {
            return Result.chickens(0);
        }
        if (random.nextInt(four) != 0) {
            return Result.chickens(1);
        }
        if (random.nextInt(sixteen) != 0) {
            return Result.chickens(4);
        }
        if (sixtyFour > 0 && random.nextInt(sixtyFour) != 0) {
            return Result.chickens(16);
        }
        return Result.chickens(sixtyFour > 0 ? 64 : 16);
    }

    /** A failed chicken roll can still spawn a silverfish, a creeper, or both. A zero chance skips that mob. */
    private static Result bad(RandomSource random, int chicken, int silverfish, int creeper) {
        if (random.nextInt(chicken) == 0) {
            return Result.chickens(1);
        }
        boolean fish = silverfish > 0 && random.nextInt(silverfish) == 0;
        boolean boom = creeper > 0 && random.nextInt(creeper) == 0;
        return new Result(0, fish, boom);
    }

    private static boolean place(Level level, EntityType<?> type, ThrownEgg egg, Consumer<Entity> prepare) {
        Entity mob = type.create(level, EntitySpawnReason.TRIGGERED);
        if (mob == null) {
            return false;
        }
        prepare.accept(mob);
        mob.snapTo(egg.getX(), egg.getY(), egg.getZ(), egg.getYRot(), 0.0F);
        if (!mob.fudgePositionAfterSizeChange(EntityDimensions.fixed(0.0F, 0.0F))) {
            return false;
        }
        level.addFreshEntity(mob);
        return true;
    }
}
