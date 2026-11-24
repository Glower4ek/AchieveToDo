package com.diskree.achievetodo.ability;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CHAOS thresholds depend only on the seed: the same in every JVM start and the same as in the Purpur plugin
 * (port/.../ChaosProgressionGeneratorTest pins the identical digest). Known issue 2, fixed in 0.3.0.
 */
final class ChaosDeterminismTest {
    // 0.4.0: the abandoned camp joined the shuffle (0.3.0 digest: 33c82f39...).
    static final String SEED_42_DIGEST = "b8c3fbbb617cbe680b729c52f96b3721698a141aa24e981d73baf894ee7b45f8";

    @BeforeAll static void bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap(); }

    @Test
    void seed42MatchesThePinnedDigestSharedWithThePlugin() throws Exception {
        assertEquals(SEED_42_DIGEST, digest(ChaosProgressionGenerator.generateChaosProgression(42L)));
    }

    /** SHA-256 of one "NAME=count" line per ability, in declaration order. */
    private static String digest(Map<AbilityType, Integer> progression) throws Exception {
        var text = new StringBuilder();
        for (AbilityType type : AbilityType.values()) text.append(type.name()).append('=').append(progression.get(type)).append('\n');
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.toString().getBytes(StandardCharsets.UTF_8)));
    }
}
