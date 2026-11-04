package com.buffspotions.witch;

import com.buffspotions.registry.ModPotions;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.alchemy.Potion;

/** Splash confounding for witches: always when she is full and a player is close, otherwise one throw in five. */
public final class WitchThrow {
    public static final double CLOSE_BLOCKS = 3.0;
    /** Harming, slowness, poison, weakness, and confounding. */
    static final int SPLASHES = 5;

    private WitchThrow() {
    }

    public static Holder<Potion> choose(Witch witch, Holder<Potion> vanilla) {
        boolean fullAndClose = witch.getHealth() >= witch.getMaxHealth() && playerWithin(witch, CLOSE_BLOCKS);
        return choose(fullAndClose, witch.getRandom().nextInt(SPLASHES), vanilla);
    }

    static Holder<Potion> choose(boolean fullAndClose, int roll, Holder<Potion> vanilla) {
        if (fullAndClose || roll == 0) {
            return ModPotions.INSTANT_CONFUSION;
        }
        return vanilla;
    }

    static boolean playerWithin(Witch witch, double blocks) {
        double range = blocks * blocks;
        for (Player player : witch.level().players()) {
            if (EntitySelector.NO_SPECTATORS.test(player) && player.isAlive() && player.distanceToSqr(witch) <= range) {
                return true;
            }
        }
        return false;
    }
}
