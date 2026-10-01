package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAEntityTypeTagPlayerKilledEntityCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void entityTypeTagPlayerKilledEntitySnapshotMatchesGeneratedArtifacts() throws IOException {
        String generated = PhaseAEntityTypeTagPlayerKilledEntityCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAEntityTypeTagPlayerKilledEntityCertification.SNAPSHOT);
        assertTrue(Files.exists(snapshot), "ENTITY_TYPE_TAG_PLAYER_KILLED_ENTITY catalog snapshot must be generated and checked in");
        assertEquals(
            JsonParser.parseString(generated),
            JsonParser.parseString(Files.readString(snapshot, StandardCharsets.UTF_8)),
            "ENTITY_TYPE_TAG_PLAYER_KILLED_ENTITY catalog snapshot is stale. Run writePhaseAEntityTypeTagPlayerKilledEntityCertification."
        );
    }

    @Test
    void entityTypeTagPlayerKilledEntityCatalogMatchesAcceptedDiagnosticFamily() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseAEntityTypeTagPlayerKilledEntityCertification.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        JsonArray cases = root.getAsJsonArray("cases");
        assertEquals(PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals(2, summary.get("totalCases").getAsInt());
        assertEquals(2, summary.get("automationSupported").getAsInt());
        assertEquals(0, summary.get("automationDeferred").getAsInt());

        Set<String> supportedKeys = new LinkedHashSet<>();
        for (int i = 0; i < cases.size(); i++) {
            JsonObject caseJson = cases.get(i).getAsJsonObject();
            String key = caseJson.get("advancementId").getAsString() + "#" + caseJson.get("criterion").getAsString();
            supportedKeys.add(key);
            assertEquals("minecraft:player_killed_entity", caseJson.get("trigger").getAsString());
            assertEquals("SUPPORTED", caseJson.get("automationEligibility").getAsString());
            assertEquals("RUNTIME_DEFERRED", caseJson.get("staticStatus").getAsString());
            assertEquals("ENTITY_TYPE_TAG", caseJson.getAsJsonArray("registryComponents").get(0).getAsString());
        }

        assertEquals(
            Set.of(
                "blazeandcave:nether/cultural_misunderstandings#piglin",
                "minecraft:adventure/kill_a_mob#monster_hunter"
            ),
            supportedKeys
        );
    }
}
