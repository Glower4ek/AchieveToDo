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
        // Same thresholds in every JVM and in the Fabric mod: a pinned digest, identical in both test suites.
        assertEquals(SEED_42_DIGEST, digest(ChaosProgressionGenerator.generateChaosProgression(42L)));
        assertChaosConstraints(0L);
        assertChaosConstraints(1L);
        assertChaosConstraints(42L);
    }

    // 0.4.0: the abandoned camp joined the shuffle (0.3.0 digest: 33c82f39...).
    static final String SEED_42_DIGEST = "b8c3fbbb617cbe680b729c52f96b3721698a141aa24e981d73baf894ee7b45f8";

    /** SHA-256 of one "NAME=count" line per ability, in declaration order. */
    static String digest(Map<AbilityType, Integer> progression) {
        var text = new StringBuilder();
        for (AbilityType type : AbilityType.values()) text.append(type.name()).append('=').append(progression.get(type)).append('\n');
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(text.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
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
