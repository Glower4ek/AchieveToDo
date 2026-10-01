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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAContainerLootExecutionEvidenceValidationTest {
    private static final String SOURCE = "PhaseAContainerLootGameTest";

    @TempDir
    Path tempDir;

    @Test
    void missingPersistentArtifactReturnsEmptyEvidence() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        assertTrue(PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir).isEmpty());
    }

    @Test
    void completeSupportedArtifactAccepted() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED),
            caseJson("adv2", "crit2", "minecraft:chests/buried_treasure", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJsonWithMetadata(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, "minecraft:chests/simple_dungeon", SOURCE, PhaseAContainerLootExecutionEvidenceValidation.GREEN),
            entryJson("adv2", "crit2", PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, "minecraft:chests/buried_treasure", SOURCE, PhaseAContainerLootExecutionEvidenceValidation.GREEN)
        ));

        Map<String, PhaseAContainerLootExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);
        PhaseAContainerLootExecutionEvidenceValidation.RuntimeEvidenceData data = evidence.get("adv1");

        assertEquals(Set.of("crit1"), data.greenCriteria());
        assertEquals(Set.of(PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY), data.families());
        assertEquals(Set.of("minecraft:chests/simple_dungeon"), data.lootTables());
        assertEquals(Set.of(SOURCE), data.sources());
    }

    @Test
    void oneSupportedCatalogCaseMissingFromArtifactRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED),
            caseJson("adv2", "crit2", "minecraft:chests/buried_treasure", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, "minecraft:chests/simple_dungeon", SOURCE, PhaseAContainerLootExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void duplicateReceiptKeyRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, "minecraft:chests/simple_dungeon", SOURCE, PhaseAContainerLootExecutionEvidenceValidation.GREEN),
            entryJson("adv1", "crit1", PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, "minecraft:chests/simple_dungeon", SOURCE, PhaseAContainerLootExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void deferredCatalogCaseDoesNotRequireReceipt() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED),
            caseJson("adv2", "crit2", "minecraft:chests/buried_treasure", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_DEFERRED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, "minecraft:chests/simple_dungeon", SOURCE, PhaseAContainerLootExecutionEvidenceValidation.GREEN)
        ));

        Map<String, PhaseAContainerLootExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);

        assertEquals(Set.of("adv1"), evidence.keySet());
    }

    @Test
    void extraReceiptMissingCatalogCaseRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, "minecraft:chests/simple_dungeon", SOURCE, PhaseAContainerLootExecutionEvidenceValidation.GREEN),
            entryJson("adv2", "crit2", PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, "minecraft:chests/buried_treasure", SOURCE, PhaseAContainerLootExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void staleFingerprintRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        writeArtifact(artifactJson("sha-256:stale", entryJson(
            "adv1",
            "crit1",
            PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY,
            "minecraft:chests/simple_dungeon",
            SOURCE,
            PhaseAContainerLootExecutionEvidenceValidation.GREEN
        )));

        assertThrows(IllegalStateException.class, () -> PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void blankRunIdRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        writeArtifact(artifactJsonWithMetadata(
            currentFingerprint(),
            "",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, "minecraft:chests/simple_dungeon", SOURCE, PhaseAContainerLootExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void wrongLootTableRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY,
            "minecraft:chests/buried_treasure",
            SOURCE,
            PhaseAContainerLootExecutionEvidenceValidation.GREEN
        )));

        assertThrows(IllegalStateException.class, () -> PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void redRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            PhaseAContainerLootExecutionEvidenceValidation.PLAYER_GENERATES_CONTAINER_LOOT_FAMILY,
            "minecraft:chests/simple_dungeon",
            SOURCE,
            "RED"
        )));

        assertThrows(IllegalStateException.class, () -> PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void unsupportedFamilyRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "minecraft:chests/simple_dungeon", PhaseAContainerLootExecutionEvidenceValidation.AUTOMATION_SUPPORTED)));
        writeArtifact(artifactJson(currentFingerprint(), entryJson(
            "adv1",
            "crit1",
            "UNSUPPORTED_FAMILY",
            "minecraft:chests/simple_dungeon",
            SOURCE,
            PhaseAContainerLootExecutionEvidenceValidation.GREEN
        )));

        assertThrows(IllegalStateException.class, () -> PhaseAContainerLootExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    private void writeCatalog(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAContainerLootExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAContainerLootExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private String currentFingerprint() throws IOException {
        return PhaseAContainerLootExecutionEvidenceValidation.fingerprint(tempDir.resolve(PhaseAContainerLootExecutionEvidenceValidation.CATALOG_PATH));
    }

    private static String catalogJson(String... casesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_container_loot_case_catalog\","
            + "\"cases\":[" + String.join(",", casesJson) + "]"
            + "}";
    }

    private static String caseJson(String advancementId, String criterion, String lootTable, String automationEligibility) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"lootTable\":\"" + lootTable + "\","
            + "\"automationEligibility\":\"" + automationEligibility + "\""
            + "}";
    }

    private static String artifactJson(String fingerprint, String... entriesJson) {
        return artifactJsonWithMetadata(fingerprint, "run-1", "2026-08-22T00:00:00Z", entriesJson);
    }

    private static String artifactJsonWithMetadata(String fingerprint, String runId, String generatedAt, String... entriesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_container_loot_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"" + runId + "\","
            + "\"generatedAt\":\"" + generatedAt + "\","
            + "\"entries\":[" + String.join(",", entriesJson) + "]"
            + "}";
    }

    private static String entryJson(String advancementId, String criterion, String family, String lootTable, String source, String result) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"family\":\"" + family + "\","
            + "\"lootTable\":\"" + lootTable + "\","
            + "\"source\":\"" + source + "\","
            + "\"result\":\"" + result + "\""
            + "}";
    }
}
