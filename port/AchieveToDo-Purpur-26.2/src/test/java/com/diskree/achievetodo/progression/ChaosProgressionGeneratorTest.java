package com.diskree.achievetodo.progression;

import com.diskree.achievetodo.ability.AbilityType;

import java.util.Map;

import static com.diskree.achievetodo.tests.TestSupport.assertEquals;
import static com.diskree.achievetodo.tests.TestSupport.assertNotEquals;
import static com.diskree.achievetodo.tests.TestSupport.assertTrue;

public final class ChaosProgressionGeneratorTest {

    private ChaosProgressionGeneratorTest() {
    }

    public static void run() {
        assertEquals(
            ChaosProgressionGenerator.generateChaosProgression(42L),
            ChaosProgressionGenerator.generateChaosProgression(42L)
        );
        assertEquals(
            ChaosProgressionGenerator.generateChaosProgression(123456789L),
            ChaosProgressionGenerator.generateChaosProgression(123456789L)
        );
        assertNotEquals(
            ChaosProgressionGenerator.generateChaosProgression(42L),
            ChaosProgressionGenerator.generateChaosProgression(43L)
        );
        assertChaosConstraints(0L);
        assertChaosConstraints(1L);
        assertChaosConstraints(42L);
    }

    private static void assertChaosConstraints(long seed) {
        Map<AbilityType, Integer> progression = ChaosProgressionGenerator.generateChaosProgression(seed);
        assertEquals(AbilityType.values().length, progression.size());
        assertEquals(0, progression.get(AbilityType.VISION));
        assertNotEquals(ProgressionConstants.PERMANENTLY_LOCKED_FLAG, progression.get(AbilityType.OPEN_CRAFTING_TABLE));
        progression.forEach((abilityType, requiredCount) -> assertTrue(
            requiredCount == ProgressionConstants.INITIALLY_UNLOCKED_FLAG ||
                requiredCount == ProgressionConstants.PERMANENTLY_LOCKED_FLAG ||
                (requiredCount >= ProgressionConstants.MIN_ADVANCEMENTS_COUNT && requiredCount <= 1000),
            () -> "Unexpected chaos requirement for " + abilityType + " on seed " + seed + ": " + requiredCount
        ));
    }
}
