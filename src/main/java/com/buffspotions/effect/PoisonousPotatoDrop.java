package com.buffspotions.effect;

/**
 * A mature potato plant has a 2% chance to also drop a poisonous potato.
 * Bad Luck I raises that chance to 4%, and Bad Luck II to 8%.
 * The higher amplifier wins. Equal amplifiers leave the 2% chance unchanged.
 * A stronger dose stays at level II, and still beats a weaker dose of the other effect.
 */
public final class PoisonousPotatoDrop {
    public static final float VANILLA = 0.02F;
    public static final float BAD_I = 0.04F;
    public static final float BAD_II = 0.08F;

    private PoisonousPotatoDrop() {
    }

    public static float chance(int luckAmplifier, int badLuckAmplifier) {
        return switch (TamingLuck.select(luckAmplifier, badLuckAmplifier)) {
            case BAD_I -> BAD_I;
            case BAD_II -> BAD_II;
            default -> VANILLA;
        };
    }
}
