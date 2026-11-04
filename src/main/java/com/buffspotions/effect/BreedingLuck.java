package com.buffspotions.effect;

import com.buffspotions.mixin.FoxTrustInvoker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.monster.Vex;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;

/**
 * Luck and Bad Luck change a bred litter. One roll in five replaces the single baby.
 * The higher amplifier wins. Equal amplifiers leave the litter unchanged.
 * A stronger dose stays at level II, and still beats a weaker dose of the other effect.
 */
public final class BreedingLuck {
    public enum Litter {
        /** The ordinary single baby. */
        VANILLA,
        /** Two babies. */
        TWINS,
        /** Four babies. */
        QUADS,
        /** The breeding finishes and no baby appears. */
        NONE,
        /** No baby. A vex appears instead. */
        VEX;

        public int extraBabies() {
            return switch (this) {
                case TWINS -> 1;
                case QUADS -> 3;
                default -> 0;
            };
        }

        public boolean replacesBaby() {
            return this == NONE || this == VEX;
        }
    }

    private BreedingLuck() {
    }

    public static Litter roll(RandomSource random, Animal parent, Animal partner) {
        return roll(random, best(parent, partner, true), best(parent, partner, false));
    }

    public static Litter roll(RandomSource random, int luckAmplifier, int badLuckAmplifier) {
        TamingLuck.Kind kind = TamingLuck.select(luckAmplifier, badLuckAmplifier);
        if (kind == TamingLuck.Kind.VANILLA || random.nextInt(5) != 0) {
            return Litter.VANILLA;
        }
        return switch (kind) {
            case LUCK_I -> Litter.TWINS;
            case LUCK_II -> Litter.QUADS;
            case BAD_I -> Litter.NONE;
            case BAD_II -> Litter.VEX;
            case VANILLA -> Litter.VANILLA;
        };
    }

    /** Parents cool down as if they had bred. No experience and no baby. */
    public static void deny(Animal parent, Animal partner) {
        parent.setAge(6000);
        partner.setAge(6000);
        parent.resetLove();
        partner.resetLove();
        parent.level().broadcastEntityEvent(parent, (byte) 18);
    }

    public static void spawnVex(ServerLevel level, Animal parent) {
        Vex vex = EntityTypes.VEX.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (vex == null) {
            return;
        }
        vex.snapTo(parent.getX(), parent.getY(), parent.getZ(), parent.getYRot(), 0.0F);
        vex.finalizeSpawn(level, level.getCurrentDifficultyAt(parent.blockPosition()), EntitySpawnReason.MOB_SUMMONED, null);
        vex.setBoundOrigin(parent.blockPosition());
        if (!vex.fudgePositionAfterSizeChange(EntityDimensions.fixed(0.0F, 0.0F))) {
            vex.snapTo(parent.getX(), parent.getY(), parent.getZ(), parent.getYRot(), 0.0F);
        }
        level.addFreshEntity(vex);
    }

    /** Another baby from the same parents, without cooling them down a second time. */
    public static void spawnExtra(ServerLevel level, Animal parent, Animal partner) {
        AgeableMob child = parent.getBreedOffspring(level, partner);
        BabyEntitySpawnEvent event = new BabyEntitySpawnEvent(parent, partner, child);
        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
            return;
        }
        child = event.getChild();
        if (child == null) {
            return;
        }
        if (child instanceof Fox fox) {
            ServerPlayer parentCause = parent.getLoveCause();
            trust(fox, parentCause);
            ServerPlayer partnerCause = partner.getLoveCause();
            if (partnerCause != parentCause) {
                trust(fox, partnerCause);
            }
        }
        child.setBaby(true);
        child.snapTo(parent.getX(), parent.getY(), parent.getZ(), 0.0F, 0.0F);
        level.addFreshEntityWithPassengers(child);
    }

    private static void trust(Fox fox, ServerPlayer player) {
        if (player != null) {
            ((FoxTrustInvoker) fox).buffsPotions$trust(player);
        }
    }

    private static int best(Animal parent, Animal partner, boolean luck) {
        int fromParent = parent == null ? -1 : TamingLuck.amplifier(parent.getLoveCause(), luck);
        int fromPartner = partner == null ? -1 : TamingLuck.amplifier(partner.getLoveCause(), luck);
        return Math.max(fromParent, fromPartner);
    }
}
