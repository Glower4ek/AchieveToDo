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

class PhaseAItemTagInventoryChangedExecutionEvidenceValidationTest {
    private static final String SOURCE = "PhaseAItemTagInventoryChangedGameTest";

    @TempDir
    Path tempDir;

    @Test
    void absentPersistentArtifactReturnsEmptyEvidence() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        assertTrue(PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir).isEmpty());
    }

    @Test
    void completeValidSupportedArtifactAccepted() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED),
            caseJson("adv2", "crit2", "minecraft:emerald_ores", 64, 64, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:slabs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN),
            entryJson("adv2", "crit2", "minecraft:emerald_ores", "minecraft:deepslate_emerald_ore", 64, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        Map<String, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);
        PhaseAItemTagInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData adv2 = evidence.get("adv2");

        assertEquals(Set.of("crit2"), adv2.greenCriteria());
        assertEquals(Set.of(PhaseAItemTagInventoryChangedExecutionEvidenceValidation.FAMILY), adv2.families());
        assertEquals(Set.of(SOURCE), adv2.sources());
        assertEquals(Set.of("minecraft:emerald_ores"), adv2.itemTags());
        assertEquals(Set.of("minecraft:deepslate_emerald_ore"), adv2.selectedItems());
    }

    @Test
    void missingSupportedReceiptRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED),
            caseJson("adv2", "crit2", "minecraft:emerald_ores", 64, 64, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:slabs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void extraUnknownReceiptRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:slabs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN),
            entryJson("adv2", "crit2", "minecraft:emerald_ores", "minecraft:deepslate_emerald_ore", 64, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void duplicateKeyRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:slabs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN),
            entryJson("adv1", "crit1", "minecraft:slabs", "minecraft:stone_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void deferredCatalogCaseReceiptRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, "DEFERRED")
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:slabs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void wrongItemTagRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:stairs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void wrongRequiredCountRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:emerald_ores", 64, 64, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:emerald_ores", "minecraft:deepslate_emerald_ore", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void blankSelectedItemRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:slabs", "", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void redResultRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:slabs", "minecraft:acacia_slab", 1, "RED")
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void wrongFamilyRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJsonWithFamilyAndSource(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            "WRONG_FAMILY",
            SOURCE,
            entryJsonWithFamilyAndSource("adv1", "crit1", "WRONG_FAMILY", "minecraft:slabs", "minecraft:acacia_slab", 1, SOURCE, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void wrongSourceRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJsonWithFamilyAndSource(
            currentFingerprint(),
            "run-1",
            "2026-08-22T00:00:00Z",
            PhaseAItemTagInventoryChangedExecutionEvidenceValidation.FAMILY,
            "WrongSource",
            entryJsonWithFamilyAndSource("adv1", "crit1", PhaseAItemTagInventoryChangedExecutionEvidenceValidation.FAMILY, "minecraft:slabs", "minecraft:acacia_slab", 1, "WrongSource", PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void staleCatalogFingerprintRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            "sha-256:stale",
            "run-1",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:slabs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void blankRunIdRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "",
            "2026-08-22T00:00:00Z",
            entryJson("adv1", "crit1", "minecraft:slabs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void blankGeneratedAtRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:slabs", null, null, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.AUTOMATION_SUPPORTED)
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            "run-1",
            "",
            entryJson("adv1", "crit1", "minecraft:slabs", "minecraft:acacia_slab", 1, PhaseAItemTagInventoryChangedExecutionEvidenceValidation.GREEN)
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAItemTagInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    private void writeCatalog(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAItemTagInventoryChangedExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAItemTagInventoryChangedExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private String currentFingerprint() throws IOException {
        return PhaseAItemTagInventoryChangedExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseAItemTagInventoryChangedExecutionEvidenceValidation.CATALOG_PATH)
        );
    }

    private static String catalogJson(String... casesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_item_tag_inventory_changed_case_catalog\","
            + "\"cases\":[" + String.join(",", casesJson) + "]"
            + "}";
    }

    private static String caseJson(
        String advancementId,
        String criterion,
        String itemTag,
        Integer requiredCountMin,
        Integer requiredCountMax,
        String automationEligibility
    ) {
        String min = requiredCountMin == null ? "null" : requiredCountMin.toString();
        String max = requiredCountMax == null ? "null" : requiredCountMax.toString();
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"itemTag\":\"" + itemTag + "\","
            + "\"requiredCountMin\":" + min + ","
            + "\"requiredCountMax\":" + max + ","
            + "\"automationEligibility\":\"" + automationEligibility + "\""
            + "}";
    }

    private static String artifactJson(String fingerprint, String runId, String generatedAt, String... entriesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_item_tag_inventory_changed_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"" + runId + "\","
            + "\"generatedAt\":\"" + generatedAt + "\","
            + "\"entries\":[" + String.join(",", entriesJson) + "]"
            + "}";
    }

    private static String artifactJsonWithFamilyAndSource(
        String fingerprint,
        String runId,
        String generatedAt,
        String family,
        String source,
        String... entriesJson
    ) {
        return artifactJson(fingerprint, runId, generatedAt, entriesJson);
    }

    private static String entryJson(String advancementId, String criterion, String itemTag, String selectedItem, int requiredCount, String result) {
        return entryJsonWithFamilyAndSource(
            advancementId,
            criterion,
            PhaseAItemTagInventoryChangedExecutionEvidenceValidation.FAMILY,
            itemTag,
            selectedItem,
            requiredCount,
            SOURCE,
            result
        );
    }

    private static String entryJsonWithFamilyAndSource(
        String advancementId,
        String criterion,
        String family,
        String itemTag,
        String selectedItem,
        int requiredCount,
        String source,
        String result
    ) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"family\":\"" + family + "\","
            + "\"itemTag\":\"" + itemTag + "\","
            + "\"selectedItem\":\"" + selectedItem + "\","
            + "\"requiredCount\":" + requiredCount + ","
            + "\"source\":\"" + source + "\","
            + "\"result\":\"" + result + "\""
            + "}";
    }
}
