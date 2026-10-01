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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAEntityTypeTagStartedRidingCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void frozenCatalogMatchesDerivedDefinitions() throws IOException {
        JsonObject generated = JsonParser.parseString(
            PhaseAEntityTypeTagStartedRidingCertification.generateSnapshot(PROJECT_ROOT)
        ).getAsJsonObject();
        Path path = PROJECT_ROOT.resolve(PhaseAEntityTypeTagStartedRidingCertification.SNAPSHOT);
        assertTrue(Files.exists(path), "Started-riding catalog must be present");
        JsonObject checkedIn = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals(generated, checkedIn, "Started-riding catalog is stale");
    }

    @Test
    void exactFiveCasesExposeTheAcceptedPredicates() throws IOException {
        JsonObject root = JsonParser.parseString(
            PhaseAEntityTypeTagStartedRidingCertification.generateSnapshot(PROJECT_ROOT)
        ).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        assertEquals(5, summary.get("totalCases").getAsInt());
        assertEquals(5, summary.get("uniqueKeys").getAsInt());
        assertEquals(5, summary.get("requirementGroups").getAsInt());
        assertEquals(5, summary.get("automationSupported").getAsInt());
        JsonArray cases = root.getAsJsonArray("cases");
        Set<String> keys = new LinkedHashSet<>();
        for (int i = 0; i < cases.size(); i++) {
            JsonObject value = cases.get(i).getAsJsonObject();
            String key = value.get("advancementId").getAsString() + "#" + value.get("criterion").getAsString();
            keys.add(key);
            assertEquals(0, value.get("requirementGroupIndex").getAsInt());
            assertEquals("minecraft:started_riding", value.get("trigger").getAsString());
            assertEquals("SUPPORTED", value.get("automationEligibility").getAsString());
            assertTrue(value.getAsJsonArray("vehiclePredicateChain").size() >= 1);
        }
        assertEquals(new LinkedHashSet<>(PhaseAEntityTypeTagStartedRidingCertification.EXPECTED_KEYS), keys);
    }
}
