package com.buffspotions.bellows;

/**
 * A block the bellows can stoke. Its tick should take a second step while {@link BellowsBoosts#isActive} is true.
 */
public interface BellowsTarget {
    boolean isWorking();
}
