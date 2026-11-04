package com.buffspotions.belt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BeltDeathExperienceTest {
    @Test
    void dropsOneThirdOfHeldExperienceAtEveryLevel() {
        int held = 0;
        for (int level = 0; level < 40; level++) {
            int pointsToNext = pointsToNext(level);
            assertEquals(held / 3, BeltDeathExperience.dropped(level, 0.0F, pointsToNext));
            int partial = Math.round(pointsToNext * 0.5F);
            assertEquals((held + partial) / 3, BeltDeathExperience.dropped(level, 0.5F, pointsToNext));
            held += pointsToNext;
        }
    }

    @Test
    void lowLevelsAreNotRaisedToTheVanillaPile() {
        assertEquals(30, BeltDeathExperience.dropped(7, 0.0F, pointsToNext(7)));
        assertEquals(465, BeltDeathExperience.dropped(30, 0.0F, pointsToNext(30)));
        assertEquals(1, BeltDeathExperience.dropped(0, 5.0F / 7.0F, 7));
    }

    private static int pointsToNext(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }
}
