package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseARuntimeExecutionEvidenceValidationTest {
    private static final String SOURCE = "PhaseAStructureLocationGameTest";

    @TempDir
    Path tempDir;

    @Test
    void familyMappingIsNarrow() {
        assertEquals(PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY, PhaseARuntimeExecutionEvidenceValidation.familyFor("BIOME_ONLY", null));
        assertEquals(PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY, PhaseARuntimeExecutionEvidenceValidation.familyFor("BIOME_SET", null));
        assertEquals(PhaseARuntimeExecutionEvidenceValidation.STRUCTURE_ONLY_FAMILY, PhaseARuntimeExecutionEvidenceValidation.familyFor("STRUCTURE_LOCATION", "STRUCTURE_ONLY"));
        assertNull(PhaseARuntimeExecutionEvidenceValidation.familyFor("STRUCTURE_LOCATION", null));
        assertNull(PhaseARuntimeExecutionEvidenceValidation.familyFor("OTHER", null));
    }

    @Test
    void supportedSimpleBiomeAccepted() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        )));

        Map<String, PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData> evidence = PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);
        PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData data = evidence.get("adv1");

        assertEquals(Set.of("crit1"), data.greenCriteria());
        assertEquals(Set.of(PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY), data.families());
        assertEquals(Set.of(SOURCE), data.sources());
    }

    @Test
    void supportedStructureOnlyAccepted() throws IOException {
        writeMatrix(matrixJson(
            "STRUCTURE_LOCATION",
            "STRUCTURE_ONLY",
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            "RUNTIME_DEFERRED"
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.STRUCTURE_ONLY_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            "RUNTIME_DEFERRED"
        )));

        Map<String, PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData> evidence = PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);
        PhaseARuntimeExecutionEvidenceValidation.RuntimeEvidenceData data = evidence.get("adv1");

        assertTrue(data.greenCriteria().contains("crit1"));
        assertTrue(data.families().contains(PhaseARuntimeExecutionEvidenceValidation.STRUCTURE_ONLY_FAMILY));
        assertTrue(data.sources().contains(SOURCE));
        assertTrue(data.criteriaByFamily().containsKey(PhaseARuntimeExecutionEvidenceValidation.STRUCTURE_ONLY_FAMILY));
        assertTrue(data.criteriaBySource().containsKey(SOURCE));
        assertEquals(Set.of("crit1"), data.criteriaByFamily().get(PhaseARuntimeExecutionEvidenceValidation.STRUCTURE_ONLY_FAMILY));
        assertEquals(Set.of("crit1"), data.criteriaBySource().get(SOURCE));
        assertFalse(data.criteriaByFamily().containsKey(SOURCE));
        assertFalse(data.criteriaBySource().containsKey(PhaseARuntimeExecutionEvidenceValidation.STRUCTURE_ONLY_FAMILY));
    }

    @Test
    void deferredEligibilityRejected() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_DEFERRED,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        )));

        assertThrows(IllegalStateException.class, () -> PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void nonGreenReceiptRejected() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            "RED",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        )));

        assertThrows(IllegalStateException.class, () -> PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void staleFingerprintRejected() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        ));
        writeArtifact(artifactJson("sha-256:stale", entryJson(
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        )));

        assertThrows(IllegalStateException.class, () -> PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void unsupportedFamilyRejected() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            "UNSUPPORTED_FAMILY",
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        )));

        assertThrows(IllegalStateException.class, () -> PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void missingMatrixCaseRejected() throws IOException {
        writeMatrix(matrixJson(
            "BIOME_ONLY",
            null,
            "adv1",
            "crit1",
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATION_SUPPORTED,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        ));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit2",
            PhaseARuntimeExecutionEvidenceValidation.LOCATION_MOVEMENT_FAMILY,
            SOURCE,
            PhaseARuntimeExecutionEvidenceValidation.GREEN,
            PhaseARuntimeExecutionEvidenceValidation.AUTOMATED_GREEN
        )));

        assertThrows(IllegalStateException.class, () -> PhaseARuntimeExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    private void writeMatrix(String json) throws IOException {
        Path path = tempDir.resolve(PhaseARuntimeExecutionEvidenceValidation.MATRIX_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseARuntimeExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private String currentFingerprint() throws IOException {
        return PhaseARuntimeExecutionEvidenceValidation.fingerprint(tempDir.resolve(PhaseARuntimeExecutionEvidenceValidation.MATRIX_PATH));
    }

    private static String matrixJson(String category, String structureLocationType, String advancementId, String criterion, String automationEligibility, String certificationStatus) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"cases\":[{");
        builder.append("\"category\":\"").append(category).append("\",");
        if (structureLocationType != null) {
            builder.append("\"structureLocationType\":\"").append(structureLocationType).append("\",");
        }
        builder.append("\"advancementId\":\"").append(advancementId).append("\",");
        builder.append("\"criterion\":\"").append(criterion).append("\",");
        builder.append("\"automationEligibility\":\"").append(automationEligibility).append("\",");
        builder.append("\"certificationStatus\":\"").append(certificationStatus).append("\"");
        builder.append("}]}");
        return builder.toString();
    }

    private static String artifactJson(String fingerprint, String entryJson) {
        return "{"
            + "\"snapshot\":\"phase_a_runtime_execution_evidence\"," 
            + "\"minecraftVersion\":\"26.2\"," 
            + "\"compatibilityMarker\":\"compat_26_2_r15\"," 
            + "\"matrixFingerprint\":\"" + fingerprint + "\"," 
            + "\"runId\":\"run-1\"," 
            + "\"generatedAt\":\"2026-08-22T00:00:00Z\"," 
            + "\"entries\":[" + entryJson + "]" 
            + "}";
    }

    private static String entryJson(String advancementId, String criterion, String family, String source, String result, String matrixCertificationStatus) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\"," 
            + "\"criterion\":\"" + criterion + "\"," 
            + "\"family\":\"" + family + "\"," 
            + "\"source\":\"" + source + "\"," 
            + "\"result\":\"" + result + "\"," 
            + "\"matrixCertificationStatus\":\"" + matrixCertificationStatus + "\""
            + "}";
    }
}
