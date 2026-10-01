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

class PhaseAItemTagPlacedBlockCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void itemTagPlacedBlockSnapshotMatchesGeneratedArtifacts() throws IOException {
        String generated = PhaseAItemTagPlacedBlockCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAItemTagPlacedBlockCertification.SNAPSHOT);
        assertTrue(Files.exists(snapshot), "ITEM_TAG_PLACED_BLOCK catalog snapshot must be generated and checked in");
        assertEquals(
            generated,
            Files.readString(snapshot, StandardCharsets.UTF_8),
            "ITEM_TAG_PLACED_BLOCK catalog snapshot is stale. Run writePhaseAItemTagPlacedBlockCertification."
        );
    }

    @Test
    void itemTagPlacedBlockCatalogMatchesAcceptedFrontier() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseAItemTagPlacedBlockCertification.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        JsonArray cases = root.getAsJsonArray("cases");
        assertEquals(PhaseACertification.EXPECTED_CANONICAL_ADVANCEMENTS, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals(5, summary.get("totalCases").getAsInt());
        assertEquals(5, summary.get("automationSupported").getAsInt());
        assertEquals(0, summary.get("automationDeferred").getAsInt());

        Set<String> supportedKeys = new LinkedHashSet<>();
        for (int i = 0; i < cases.size(); i++) {
            JsonObject caseJson = cases.get(i).getAsJsonObject();
            String key = caseJson.get("advancementId").getAsString() + "#" + caseJson.get("criterion").getAsString();
            supportedKeys.add(key);
            assertEquals("minecraft:placed_block", caseJson.get("trigger").getAsString());
            assertEquals("SUPPORTED", caseJson.get("automationEligibility").getAsString());
            assertEquals("TAG_CONTEXT_REQUIRED", caseJson.get("deferReason").getAsString());
        }

        assertEquals(
            Set.of(
                "blazeandcave:building/en_garde#fence",
                "blazeandcave:building/hanging_around#hanging_sign",
                "blazeandcave:building/its_a_sign#sign",
                "blazeandcave:building/its_a_trap#trapdoor",
                "blazeandcave:building/raise_the_flag#banner"
            ),
            supportedKeys
        );
    }
}
