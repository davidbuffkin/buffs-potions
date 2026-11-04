package com.buffspotions.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisitationTest {
    @Test
    void levelOneChorusesOnlyWhenTheTripChangesDimension() {
        assertTrue(Visitation.chorusInsteadOfCrossing(0, true));
        assertFalse(Visitation.chorusInsteadOfCrossing(0, false));
        assertFalse(Visitation.chorusInsteadOfCrossing(1, true));
        assertFalse(Visitation.chorusInsteadOfCrossing(2, true));
    }
}
