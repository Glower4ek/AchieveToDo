package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAContainerLootCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void containerLootCatalogSnapshotMatchesGeneratedArtifacts() throws IOException {
        String generated = PhaseAContainerLootCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAContainerLootCertification.SNAPSHOT);
        assertTrue(Files.exists(snapshot), "PLAYER_GENERATES_CONTAINER_LOOT catalog snapshot must be generated and checked in");
        assertEquals(
            generated,
            Files.readString(snapshot, StandardCharsets.UTF_8),
            "PLAYER_GENERATES_CONTAINER_LOOT catalog snapshot is stale. Run writePhaseAContainerLootCertification."
        );
    }

    @Test
    void containerLootSummaryMatchesFrozenCanonicalFamily() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseAContainerLootCertification.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        JsonObject byEligibility = summary.getAsJsonObject("byEligibility");
        assertEquals(56, summary.get("totalCases").getAsInt());
        assertEquals(7, summary.get("uniqueAdvancements").getAsInt());
        assertEquals(56, summary.get("automationSupported").getAsInt());
        assertEquals(0, summary.get("automationDeferred").getAsInt());
        assertEquals(56, byEligibility.get("SUPPORTED").getAsInt());
    }
}
