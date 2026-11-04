package com.buffspotions.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnShiftTest {
    @Test
    void cryingObsidianBetweenTheFeetBlocksStopsTheShift() {
        assertTrue(ColumnShift.cryingObsidianBetween(10, 20, y -> y == 15));
        assertTrue(ColumnShift.cryingObsidianBetween(20, 10, y -> y == 11));
    }

    @Test
    void cryingObsidianAtEitherEndOrOutsideTheSpanDoesNot() {
        assertFalse(ColumnShift.cryingObsidianBetween(10, 20, y -> y == 10));
        assertFalse(ColumnShift.cryingObsidianBetween(10, 20, y -> y == 20));
        assertFalse(ColumnShift.cryingObsidianBetween(10, 20, y -> y == 9 || y == 21));
        assertFalse(ColumnShift.cryingObsidianBetween(10, 11, y -> true));
        assertFalse(ColumnShift.cryingObsidianBetween(10, 10, y -> true));
    }

    @Test
    void heightsSearchStartsAtTheWaist() {
        assertEquals(65, ColumnShift.ascentStart(65.0, 1.8F));
        assertEquals(65, ColumnShift.ascentStart(64.875, 1.8F));
    }
}
