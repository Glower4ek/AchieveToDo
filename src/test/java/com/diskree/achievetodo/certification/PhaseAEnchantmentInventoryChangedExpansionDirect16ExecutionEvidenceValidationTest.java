package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidationTest {
    @TempDir
    Path tempDir;

    @Test
    void partialTemporaryArtifactAcceptedForDiagnosticSlice() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:bow", new String[]{"minecraft:bow"}, enchantmentJson("minecraft:power", 5, null))
        ));
        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            entryJson("adv1", "crit1", "minecraft:bow", enchantmentEntryJson("minecraft:power", 5, "ENCHANTMENTS"))
        ));

        PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir);

        assertEquals(1, artifact.receipts().size());
        assertEquals("minecraft:bow", artifact.receipts().getFirst().selectedItem());
    }

    @Test
    void persistentArtifactValidatesWithCanonicalCoverage() throws IOException {
        Map<String, PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.loadValidatedRuntimeEvidence(
                Path.of("").toAbsolutePath().normalize()
            );

        assertEquals(16, evidence.size());
        assertEquals(25, evidence.values().stream().mapToInt(value -> value.greenCriteria().size()).sum());
        assertTrue(evidence.values().stream().allMatch(value -> value.families().contains(
            PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.FAMILY
        )));
        assertTrue(evidence.values().stream().allMatch(value -> value.sources().contains(
            PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.SOURCE
        )));
    }

    @Test
    void missingPersistentArtifactIsRejected() {
        assertThrows(
            IllegalStateException.class,
            () -> PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir)
        );
    }

    @Test
    void promotableValidationStillRequiresExactSupportedSet() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:bow", new String[]{"minecraft:bow"}, enchantmentJson("minecraft:power", 5, null)),
            caseJson("adv2", "crit2", "minecraft:diamond_sword", new String[]{}, enchantmentJson("minecraft:sharpness", 5, null))
        ));
        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            entryJson("adv1", "crit1", "minecraft:bow", enchantmentEntryJson("minecraft:power", 5, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class,
            () -> PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void wrongSelectedItemRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:bow", new String[]{"minecraft:bow"}, enchantmentJson("minecraft:power", 5, null))
        ));
        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            entryJson("adv1", "crit1", "minecraft:crossbow", enchantmentEntryJson("minecraft:power", 5, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class,
            () -> PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void wrongStorageTypeRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:bow", new String[]{"minecraft:bow"}, enchantmentJson("minecraft:power", 5, null))
        ));
        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            entryJson("adv1", "crit1", "minecraft:bow", enchantmentEntryJson("minecraft:power", 5, "STORED_ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class,
            () -> PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void duplicateReceiptRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "minecraft:bow", new String[]{"minecraft:bow"}, enchantmentJson("minecraft:power", 5, null))
        ));
        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            entryJson("adv1", "crit1", "minecraft:bow", enchantmentEntryJson("minecraft:power", 5, "ENCHANTMENTS")),
            entryJson("adv1", "crit1", "minecraft:bow", enchantmentEntryJson("minecraft:power", 5, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class,
            () -> PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    private void writeCatalog(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeTemporaryArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.TEMPORARY_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private String currentFingerprint() throws IOException {
        return PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation.CATALOG_PATH)
        );
    }

    private static String catalogJson(String... casesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_enchantment_inventory_changed_expansion_direct16_case_catalog\","
            + "\"cases\":[" + String.join(",", casesJson) + "]"
            + "}";
    }

    private static String caseJson(String advancementId, String criterion, String selectedItem, String[] allowedItems, String... enchantments) {
        StringBuilder allowedItemsJson = new StringBuilder();
        for (int i = 0; i < allowedItems.length; i++) {
            if (i > 0) {
                allowedItemsJson.append(",");
            }
            allowedItemsJson.append("\"").append(allowedItems[i]).append("\"");
        }
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"selectedItem\":\"" + selectedItem + "\","
            + "\"automationEligibility\":\"SUPPORTED\","
            + "\"allowedItems\":[" + allowedItemsJson + "],"
            + "\"enchantmentPredicates\":[" + String.join(",", enchantments) + "]"
            + "}";
    }

    private static String enchantmentJson(String selector, Integer minLevel, Integer maxLevel) {
        return "{"
            + "\"selector\":\"" + selector + "\","
            + "\"storageType\":\"ENCHANTMENTS\","
            + "\"minLevel\":" + (minLevel == null ? "null" : minLevel) + ","
            + "\"maxLevel\":" + (maxLevel == null ? "null" : maxLevel)
            + "}";
    }

    private static String artifactJson(String fingerprint, String runId, String... entriesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_enchantment_inventory_changed_expansion_direct16_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"" + runId + "\","
            + "\"generatedAt\":\"2026-08-30T00:00:00Z\","
            + "\"entries\":[" + String.join(",", entriesJson) + "]"
            + "}";
    }

    private static String entryJson(String advancementId, String criterion, String selectedItem, String... enchantments) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"family\":\"ENCHANTMENT_INVENTORY_CHANGED_EXPANSION_DIRECT16\","
            + "\"selectedItem\":\"" + selectedItem + "\","
            + "\"enchantments\":[" + String.join(",", enchantments) + "],"
            + "\"source\":\"PhaseAEnchantmentInventoryChangedExpansionDirect16GameTest\","
            + "\"result\":\"GREEN\","
            + "\"criterionBefore\":false,"
            + "\"criterionAfter\":true,"
            + "\"itemEntityConsumed\":true,"
            + "\"matchingStackPresentAfter\":true,"
            + "\"inventoryBefore\":\"[]\","
            + "\"inventoryAfter\":\"[0=ItemStack]\","
            + "\"itemEntityBefore\":\"alive=true\","
            + "\"itemEntityAfter\":\"alive=false\","
            + "\"ticksToPickup\":1"
            + "}";
    }

    private static String enchantmentEntryJson(String selector, int level, String storageType) {
        return "{"
            + "\"selector\":\"" + selector + "\","
            + "\"level\":" + level + ","
            + "\"storageType\":\"" + storageType + "\""
            + "}";
    }
}
