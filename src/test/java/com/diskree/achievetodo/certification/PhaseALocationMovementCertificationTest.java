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

class PhaseALocationMovementCertificationTest {

    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void locationMovementMatrixSnapshotMatchesGeneratedArtifacts() throws IOException {
        String generated = PhaseALocationMovementCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseALocationMovementCertification.SNAPSHOT);
        assertTrue(Files.exists(snapshot), "LOCATION_MOVEMENT matrix snapshot must be generated and checked in");
        assertEquals(
            generated,
            Files.readString(snapshot, StandardCharsets.UTF_8),
            "LOCATION_MOVEMENT matrix snapshot is stale. Run PhaseALocationMovementCertification."
        );
    }

    @Test
    void locationMovementSummaryMatchesExpectedFrozenClassification() throws IOException {
        JsonObject root = JsonParser.parseString(PhaseALocationMovementCertification.generateSnapshot(PROJECT_ROOT)).getAsJsonObject();
        JsonObject summary = root.getAsJsonObject("summary");
        JsonObject byCategory = summary.getAsJsonObject("byCategory");
        JsonObject byStatus = summary.getAsJsonObject("byStatus");
        assertEquals(259, summary.get("totalFamilyCases").getAsInt());
        assertEquals(166, summary.get("simpleBiomeCases").getAsInt());
        assertEquals(95, summary.get("automatedGreen").getAsInt());
        assertEquals(0, summary.get("automatedRed").getAsInt());
        assertEquals(164, summary.get("deferredToSpecializedHarness").getAsInt());
        assertEquals(139, summary.get("automationSupported").getAsInt());
        assertEquals(120, summary.get("automationDeferred").getAsInt());
        assertEquals(146, byCategory.get("BIOME_ONLY").getAsInt());
        assertEquals(20, byCategory.get("BIOME_SET").getAsInt());
        assertEquals(41, byCategory.get("DIMENSION_LOCATION").getAsInt());
        assertEquals(48, byCategory.get("STRUCTURE_LOCATION").getAsInt());
        assertEquals(4, byCategory.get("BLOCK_OR_SURFACE_MOVEMENT").getAsInt());
        assertEquals(95, byStatus.get("AUTOMATED_GREEN").getAsInt());
        assertEquals(164, byStatus.get("RUNTIME_DEFERRED").getAsInt());

        int structureOnly = 0;
        int structurePlusBiome = 0;
        int structurePlusDimension = 0;
        int structurePlusPlayerState = 0;
        int structurePlusOther = 0;
        int supportedBiomes = 0;
        int supportedStructureOnly = 0;
        int supportedTotal = 0;
        int deferredTotal = 0;
        for (var caseElement : root.getAsJsonArray("cases")) {
            JsonObject locationCase = caseElement.getAsJsonObject();
            String automationEligibility = locationCase.get("automationEligibility").getAsString();
            if ("SUPPORTED".equals(automationEligibility)) {
                supportedTotal++;
                if ("BIOME_ONLY".equals(locationCase.get("category").getAsString()) || "BIOME_SET".equals(locationCase.get("category").getAsString())) {
                    supportedBiomes++;
                }
                if ("STRUCTURE_LOCATION".equals(locationCase.get("category").getAsString())
                    && "STRUCTURE_ONLY".equals(locationCase.get("structureLocationType").getAsString())) {
                    supportedStructureOnly++;
                }
            } else if ("DEFERRED".equals(automationEligibility)) {
                deferredTotal++;
            } else {
                throw new AssertionError("Unexpected automationEligibility: " + automationEligibility);
            }
            if (!"STRUCTURE_LOCATION".equals(locationCase.get("category").getAsString())) {
                continue;
            }
            String structureType = locationCase.get("structureLocationType").getAsString();
            switch (structureType) {
                case "STRUCTURE_ONLY" -> structureOnly++;
                case "STRUCTURE_PLUS_BIOME" -> structurePlusBiome++;
                case "STRUCTURE_PLUS_DIMENSION" -> structurePlusDimension++;
                case "STRUCTURE_PLUS_PLAYER_STATE" -> structurePlusPlayerState++;
                case "STRUCTURE_PLUS_OTHER" -> structurePlusOther++;
                default -> throw new AssertionError("Unexpected structureLocationType: " + structureType);
            }
        }
        assertEquals(44, structureOnly);
        assertEquals(2, structurePlusBiome);
        assertEquals(2, structurePlusDimension);
        assertEquals(0, structurePlusPlayerState);
        assertEquals(0, structurePlusOther);
        assertEquals(95, supportedBiomes);
        assertEquals(44, supportedStructureOnly);
        assertEquals(139, supportedTotal);
        assertEquals(120, deferredTotal);
    }
}
