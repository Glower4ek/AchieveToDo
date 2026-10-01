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

class PhaseAEnchantmentInventoryChangedExecutionEvidenceValidationTest {
    private static final String SOURCE = "PhaseAEnchantmentInventoryChangedGameTest";

    @TempDir
    Path tempDir;

    @Test
    void missingPersistentArtifactReturnsEmptyEvidence() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))));
        assertTrue(PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir).isEmpty());
    }

    @Test
    void completeSupportedArtifactAccepted() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS")),
            caseJson("adv2", "crit2", "SUPPORTED", new String[]{"minecraft:enchanted_book"}, enchantmentJson("minecraft:swift_sneak", 1, null, "STORED_ENCHANTMENTS"))
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:wooden_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS")),
            entryJson("adv2", "crit2", "minecraft:enchanted_book", SOURCE, "GREEN", enchantmentEntryJson("minecraft:swift_sneak", 1, "STORED_ENCHANTMENTS"))
        ));

        Map<String, PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);
        PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData data = evidence.get("adv1");

        assertEquals(Set.of("crit1"), data.greenCriteria());
        assertEquals(Set.of(PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.FAMILY), data.families());
        assertEquals(Set.of("minecraft:wooden_sword"), data.selectedItems());
    }

    @Test
    void oneSupportedCatalogCaseMissingFromArtifactRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS")),
            caseJson("adv2", "crit2", "SUPPORTED", new String[]{"minecraft:enchanted_book"}, enchantmentJson("minecraft:swift_sneak", 1, null, "STORED_ENCHANTMENTS"))
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:wooden_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void deferredCatalogCaseReceiptRejected() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS")),
            caseJson("adv2", "crit2", "DEFERRED", new String[]{"minecraft:trident"}, enchantmentJson("minecraft:channeling", 1, null, "ENCHANTMENTS"))
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:wooden_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS")),
            entryJson("adv2", "crit2", "minecraft:trident", SOURCE, "GREEN", enchantmentEntryJson("minecraft:channeling", 1, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void wrongStorageTypeRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:enchanted_book"}, enchantmentJson("minecraft:swift_sneak", 1, null, "STORED_ENCHANTMENTS"))));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:enchanted_book", SOURCE, "GREEN", enchantmentEntryJson("minecraft:swift_sneak", 1, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void belowMinLevelRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 3, 5, "ENCHANTMENTS"))));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:wooden_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 2, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void aboveMaxLevelRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 3, 5, "ENCHANTMENTS"))));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:wooden_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 6, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void inRangeLevelAccepted() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 3, 5, "ENCHANTMENTS"))));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:wooden_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 4, "ENCHANTMENTS"))
        ));

        Map<String, PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);

        assertTrue(evidence.containsKey("adv1"));
    }

    @Test
    void duplicateReceiptRejectedBeforeSetEquality() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:wooden_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS")),
            entryJson("adv1", "crit1", "minecraft:wooden_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS"))
        ));

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
        assertTrue(error.getMessage().contains("Duplicate runtime evidence receipt key"));
    }

    @Test
    void wrongFamilyRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJsonWithFamily("adv1", "crit1", "WRONG_FAMILY", "minecraft:wooden_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void wrongSourceRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:wooden_sword", "WrongSource", "GREEN", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void redResultRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:wooden_sword", SOURCE, "RED", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void blankMetadataRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))));
        writeArtifact("{"
            + "\"snapshot\":\"phase_a_enchantment_inventory_changed_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + currentFingerprint() + "\","
            + "\"runId\":\"\","
            + "\"generatedAt\":\"\","
            + "\"entries\":[]"
            + "}");

        assertThrows(IllegalStateException.class, () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void staleFingerprintRejected() throws IOException {
        writeCatalog(catalogJson(caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))));
        writeArtifact(artifactJson(
            "sha-256:stale",
            entryJson("adv1", "crit1", "minecraft:wooden_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS"))
        ));

        assertThrows(IllegalStateException.class, () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void allowedItemAccepted() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword", "minecraft:stone_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:stone_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS"))
        ));

        Map<String, PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);

        assertEquals(Set.of("minecraft:stone_sword"), evidence.get("adv1").selectedItems());
    }

    @Test
    void nonAllowedSelectedItemRejectedWhenAllowedItemsPresent() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "SUPPORTED", new String[]{"minecraft:wooden_sword"}, enchantmentJson("minecraft:sharpness", 5, null, "ENCHANTMENTS"))
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:stone_sword", SOURCE, "GREEN", enchantmentEntryJson("minecraft:sharpness", 5, "ENCHANTMENTS"))
        ));

        IllegalStateException error = assertThrows(
            IllegalStateException.class,
            () -> PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir)
        );
        assertTrue(error.getMessage().contains("Selected item is not allowed"));
    }

    @Test
    void arbitraryNonblankSelectedItemAcceptedWhenAllowedItemsEmpty() throws IOException {
        writeCatalog(catalogJson(
            caseJson("adv1", "crit1", "SUPPORTED", new String[]{}, enchantmentJson("minecraft:wind_burst", 1, null, "ENCHANTMENTS"))
        ));
        writeArtifact(artifactJson(
            currentFingerprint(),
            entryJson("adv1", "crit1", "minecraft:mace", SOURCE, "GREEN", enchantmentEntryJson("minecraft:wind_burst", 1, "ENCHANTMENTS"))
        ));

        Map<String, PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);

        assertEquals(Set.of("minecraft:mace"), evidence.get("adv1").selectedItems());
    }

    private void writeCatalog(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private String currentFingerprint() throws IOException {
        return PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation.CATALOG_PATH)
        );
    }

    private static String catalogJson(String... casesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_enchantment_inventory_changed_case_catalog\","
            + "\"cases\":[" + String.join(",", casesJson) + "]"
            + "}";
    }

    private static String caseJson(String advancementId, String criterion, String automationEligibility, String[] allowedItems, String... enchantments) {
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
            + "\"automationEligibility\":\"" + automationEligibility + "\","
            + "\"allowedItems\":[" + allowedItemsJson + "],"
            + "\"enchantmentPredicates\":[" + String.join(",", enchantments) + "]"
            + "}";
    }

    private static String enchantmentJson(String selector, Integer minLevel, Integer maxLevel, String storageType) {
        return "{"
            + "\"selector\":\"" + selector + "\","
            + "\"storageType\":\"" + storageType + "\","
            + "\"minLevel\":" + (minLevel == null ? "null" : minLevel) + ","
            + "\"maxLevel\":" + (maxLevel == null ? "null" : maxLevel)
            + "}";
    }

    private static String artifactJson(String fingerprint, String... entriesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_enchantment_inventory_changed_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"run-1\","
            + "\"generatedAt\":\"2026-08-23T00:00:00Z\","
            + "\"entries\":[" + String.join(",", entriesJson) + "]"
            + "}";
    }

    private static String entryJson(String advancementId, String criterion, String selectedItem, String source, String result, String... enchantments) {
        return entryJsonWithFamily(advancementId, criterion, "ENCHANTMENT_HOLDERSET_INVENTORY_CHANGED", selectedItem, source, result, enchantments);
    }

    private static String entryJsonWithFamily(String advancementId, String criterion, String family, String selectedItem, String source, String result, String... enchantments) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"family\":\"" + family + "\","
            + "\"selectedItem\":\"" + selectedItem + "\","
            + "\"enchantments\":[" + String.join(",", enchantments) + "],"
            + "\"source\":\"" + source + "\","
            + "\"result\":\"" + result + "\","
            + "\"inventoryBefore\":\"[]\","
            + "\"inventoryAfter\":\"[0=ItemStack]\","
            + "\"itemEntityBefore\":\"alive=true\","
            + "\"itemEntityAfter\":\"alive=false\","
            + "\"ticksToPickup\":2"
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
