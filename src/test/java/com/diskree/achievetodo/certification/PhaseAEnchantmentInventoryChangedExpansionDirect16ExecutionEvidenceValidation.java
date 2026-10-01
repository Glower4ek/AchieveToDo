package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation {
    static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "reports", "phase-a-certification", "enchantment_inventory_changed_expansion_direct16_execution_evidence.json"
    );
    static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification", "enchantment_inventory_changed_expansion_direct16_execution_evidence.json"
    );
    static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification", "enchantment_inventory_changed_expansion_direct16_case_catalog.json"
    );
    static final String SNAPSHOT = "phase_a_enchantment_inventory_changed_expansion_direct16_execution_evidence";
    static final String CATALOG_SNAPSHOT = "phase_a_enchantment_inventory_changed_expansion_direct16_case_catalog";
    static final String MINECRAFT_VERSION = "26.2";
    static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    static final String GREEN = "GREEN";
    static final String FAMILY = "ENCHANTMENT_INVENTORY_CHANGED_EXPANSION_DIRECT16";
    static final String SOURCE = "PhaseAEnchantmentInventoryChangedExpansionDirect16GameTest";
    static final String AUTOMATION_SUPPORTED = "SUPPORTED";

    private PhaseAEnchantmentInventoryChangedExpansionDirect16ExecutionEvidenceValidation() {
    }

    static RuntimeArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, false);
    }

    static RuntimeArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, true);
    }

    static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        RuntimeArtifact artifact = loadValidatedArtifact(projectRoot, PERSISTENT_ARTIFACT, true);
        if (artifact.receipts().size() != 25) {
            throw new IllegalStateException("Expected exactly 25 runtime evidence receipts but got " + artifact.receipts().size());
        }
        Set<String> actualAdvancementIds = new LinkedHashSet<>();
        Map<String, RuntimeEvidenceData> evidence = new LinkedHashMap<>();
        for (RuntimeReceipt receipt : artifact.receipts()) {
            actualAdvancementIds.add(receipt.advancementId());
            RuntimeEvidenceData runtimeEvidence = evidence.computeIfAbsent(receipt.advancementId(), RuntimeEvidenceData::empty);
            runtimeEvidence.greenCriteria().add(receipt.criterion());
            runtimeEvidence.criteriaByFamily().computeIfAbsent(FAMILY, ignored -> new LinkedHashSet<>()).add(receipt.criterion());
            runtimeEvidence.criteriaBySource().computeIfAbsent(SOURCE, ignored -> new LinkedHashSet<>()).add(receipt.criterion());
            runtimeEvidence.families().add(FAMILY);
            runtimeEvidence.sources().add(SOURCE);
        }
        if (actualAdvancementIds.size() != 16) {
            throw new IllegalStateException("Expected exactly 16 runtime evidence advancements but got " + actualAdvancementIds.size());
        }
        return evidence;
    }

    private static RuntimeArtifact loadValidatedArtifact(Path projectRoot, boolean requireExactCoverage) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, requireExactCoverage);
    }

    private static RuntimeArtifact loadValidatedArtifact(
        Path projectRoot,
        Path relativeArtifactPath,
        boolean requireExactCoverage
    ) throws IOException {
        Path artifactPath = projectRoot.resolve(relativeArtifactPath);
        if (!Files.exists(artifactPath)) {
            throw new IllegalStateException("Missing " + relativeArtifactPath + " ENCHANTMENT_INVENTORY_CHANGED_EXPANSION_DIRECT16 artifact");
        }
        Path catalogPath = projectRoot.resolve(CATALOG_PATH);
        String catalogFingerprint = fingerprint(catalogPath);
        Map<String, CatalogCase> catalog = loadCatalog(catalogPath);
        JsonObject root = readJson(artifactPath);
        requireString(root, "snapshot", SNAPSHOT);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "catalogFingerprint", catalogFingerprint);
        requireNonBlank(root, "runId");
        requireNonBlank(root, "generatedAt");

        Set<String> actualReceiptKeys = new LinkedHashSet<>();
        List<RuntimeReceipt> receipts = new ArrayList<>();
        JsonArray entries = root.getAsJsonArray("entries");
        if (entries != null) {
            for (JsonElement entryElement : entries) {
                JsonObject entry = entryElement.getAsJsonObject();
                String advancementId = requiredNonBlank(entry, "advancementId");
                String criterion = requiredNonBlank(entry, "criterion");
                String receiptKey = key(advancementId, criterion);
                if (!actualReceiptKeys.add(receiptKey)) {
                    throw new IllegalStateException("Duplicate runtime evidence receipt key: " + receiptKey);
                }
                CatalogCase catalogCase = catalog.get(receiptKey);
                if (catalogCase == null) {
                    throw new IllegalStateException("Runtime evidence references unsupported or unknown catalog case: " + receiptKey);
                }
                requireString(entry, "result", GREEN);
                requireString(entry, "family", FAMILY);
                requireString(entry, "source", SOURCE);
                requireBoolean(entry, "criterionBefore", false);
                requireBoolean(entry, "criterionAfter", true);
                requireBoolean(entry, "itemEntityConsumed", true);
                requireBoolean(entry, "matchingStackPresentAfter", true);
                String selectedItem = requiredNonBlank(entry, "selectedItem");
                if (!selectedItem.equals(catalogCase.selectedItem())) {
                    throw new IllegalStateException("Selected item drift for " + receiptKey + ": " + selectedItem);
                }
                if (!catalogCase.allowedItems().isEmpty() && !catalogCase.allowedItems().contains(selectedItem)) {
                    throw new IllegalStateException("Selected item is not allowed for " + receiptKey + ": " + selectedItem);
                }
                List<ConfiguredEnchantment> configuredEnchantments = readConfiguredEnchantments(entry);
                validateConfiguredEnchantments(receiptKey, catalogCase, configuredEnchantments);
                receipts.add(new RuntimeReceipt(
                    advancementId,
                    criterion,
                    selectedItem,
                    configuredEnchantments,
                    entry.get("ticksToPickup").getAsInt()
                ));
            }
        }
        Set<String> expectedSupportedKeys = supportedCatalogKeys(catalog);
        if (requireExactCoverage && !actualReceiptKeys.equals(expectedSupportedKeys)) {
            throw new IllegalStateException("Runtime evidence receipt keys do not exactly match current SUPPORTED catalog keys");
        }
        return new RuntimeArtifact(root.get("runId").getAsString(), root.get("generatedAt").getAsString(), receipts);
    }

    static Map<String, CatalogCase> loadCatalog(Path catalogPath) throws IOException {
        Map<String, CatalogCase> catalog = new LinkedHashMap<>();
        JsonObject root = readJson(catalogPath);
        requireString(root, "snapshot", CATALOG_SNAPSHOT);
        JsonArray cases = root.getAsJsonArray("cases");
        if (cases == null) {
            return catalog;
        }
        for (JsonElement element : cases) {
            JsonObject caseJson = element.getAsJsonObject();
            String advancementId = caseJson.get("advancementId").getAsString();
            String criterion = caseJson.get("criterion").getAsString();
            List<String> allowedItems = new ArrayList<>();
            JsonArray allowedItemsJson = caseJson.getAsJsonArray("allowedItems");
            if (allowedItemsJson != null) {
                for (JsonElement allowedItemElement : allowedItemsJson) {
                    allowedItems.add(allowedItemElement.getAsString());
                }
            }
            List<CatalogEnchantment> enchantments = new ArrayList<>();
            JsonArray enchantmentsJson = caseJson.getAsJsonArray("enchantmentPredicates");
            if (enchantmentsJson != null) {
                for (JsonElement enchantmentElement : enchantmentsJson) {
                    JsonObject enchantmentJson = enchantmentElement.getAsJsonObject();
                    enchantments.add(new CatalogEnchantment(
                        enchantmentJson.get("selector").getAsString(),
                        enchantmentJson.get("storageType").getAsString(),
                        nullableInt(enchantmentJson, "minLevel"),
                        nullableInt(enchantmentJson, "maxLevel")
                    ));
                }
            }
            catalog.put(key(advancementId, criterion), new CatalogCase(
                advancementId,
                criterion,
                caseJson.get("selectedItem").getAsString(),
                caseJson.get("automationEligibility").getAsString(),
                List.copyOf(allowedItems),
                List.copyOf(enchantments)
            ));
        }
        return catalog;
    }

    static String fingerprint(Path catalogPath) throws IOException {
        try {
            byte[] bytes = Files.readAllBytes(catalogPath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha-256:" + toHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    static Set<String> supportedCatalogKeys(Map<String, CatalogCase> catalog) {
        Set<String> supportedKeys = new LinkedHashSet<>();
        for (Map.Entry<String, CatalogCase> entry : catalog.entrySet()) {
            if (AUTOMATION_SUPPORTED.equals(entry.getValue().automationEligibility())) {
                supportedKeys.add(entry.getKey());
            }
        }
        return supportedKeys;
    }

    private static void validateConfiguredEnchantments(String receiptKey, CatalogCase catalogCase, List<ConfiguredEnchantment> configuredEnchantments) {
        if (configuredEnchantments.size() != catalogCase.enchantments().size()) {
            throw new IllegalStateException("Configured enchantment count mismatch for " + receiptKey);
        }
        Map<String, ConfiguredEnchantment> configuredByKey = new LinkedHashMap<>();
        for (ConfiguredEnchantment configuredEnchantment : configuredEnchantments) {
            String key = configuredEnchantment.selector() + "|" + configuredEnchantment.storageType();
            if (configuredByKey.putIfAbsent(key, configuredEnchantment) != null) {
                throw new IllegalStateException("Duplicate configured enchantment in receipt for " + receiptKey);
            }
        }
        for (CatalogEnchantment expectedEnchantment : catalogCase.enchantments()) {
            String key = expectedEnchantment.selector() + "|" + expectedEnchantment.storageType();
            ConfiguredEnchantment actualEnchantment = configuredByKey.get(key);
            if (actualEnchantment == null) {
                throw new IllegalStateException("Missing required configured enchantment for " + receiptKey + ": " + key);
            }
            int selectedLevel = expectedEnchantment.minLevel() != null ? expectedEnchantment.minLevel() : 1;
            if (actualEnchantment.level() != selectedLevel) {
                throw new IllegalStateException("Configured enchantment level drift for " + receiptKey + ": " + key);
            }
            if (expectedEnchantment.minLevel() != null && actualEnchantment.level() < expectedEnchantment.minLevel()) {
                throw new IllegalStateException("Configured enchantment below min level for " + receiptKey + ": " + key);
            }
            if (expectedEnchantment.maxLevel() != null && actualEnchantment.level() > expectedEnchantment.maxLevel()) {
                throw new IllegalStateException("Configured enchantment above max level for " + receiptKey + ": " + key);
            }
        }
    }

    private static List<ConfiguredEnchantment> readConfiguredEnchantments(JsonObject entry) {
        JsonArray enchantmentsJson = entry.getAsJsonArray("enchantments");
        if (enchantmentsJson == null) {
            throw new IllegalStateException("Missing enchantments array");
        }
        List<ConfiguredEnchantment> enchantments = new ArrayList<>();
        for (JsonElement enchantmentElement : enchantmentsJson) {
            JsonObject enchantment = enchantmentElement.getAsJsonObject();
            enchantments.add(new ConfiguredEnchantment(
                enchantment.get("selector").getAsString(),
                enchantment.get("level").getAsInt(),
                enchantment.get("storageType").getAsString()
            ));
        }
        return enchantments;
    }

    static record CatalogCase(
        String advancementId,
        String criterion,
        String selectedItem,
        String automationEligibility,
        List<String> allowedItems,
        List<CatalogEnchantment> enchantments
    ) {
    }

    static record CatalogEnchantment(String selector, String storageType, Integer minLevel, Integer maxLevel) {
    }

    static record ConfiguredEnchantment(String selector, int level, String storageType) {
    }

    static record RuntimeReceipt(
        String advancementId,
        String criterion,
        String selectedItem,
        List<ConfiguredEnchantment> enchantments,
        int ticksToPickup
    ) {
    }

    static record RuntimeArtifact(String runId, String generatedAt, List<RuntimeReceipt> receipts) {
    }

    static record RuntimeEvidenceData(
        String advancementId,
        Set<String> greenCriteria,
        Map<String, Set<String>> criteriaByFamily,
        Map<String, Set<String>> criteriaBySource,
        Set<String> families,
        Set<String> sources
    ) {
        static RuntimeEvidenceData empty(String advancementId) {
            return new RuntimeEvidenceData(
                advancementId,
                new LinkedHashSet<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>()
            );
        }
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void requireString(JsonObject json, String key, String expectedValue) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        if (!expectedValue.equals(json.get(key).getAsString())) {
            throw new IllegalStateException("Expected " + key + "=" + expectedValue + " but got " + json.get(key).getAsString());
        }
    }

    private static void requireBoolean(JsonObject json, String key, boolean expectedValue) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        if (json.get(key).getAsBoolean() != expectedValue) {
            throw new IllegalStateException("Expected " + key + "=" + expectedValue + " but got " + json.get(key).getAsBoolean());
        }
    }

    private static void requireNonBlank(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        requireNonBlank(json.get(key).getAsString(), key);
    }

    private static String requiredNonBlank(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        String value = json.get(key).getAsString();
        requireNonBlank(value, key);
        return value;
    }

    private static void requireNonBlank(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Expected nonblank " + key);
        }
    }

    private static Integer nullableInt(JsonObject json, String key) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? null : element.getAsInt();
    }

    private static String key(String advancementId, String criterion) {
        return advancementId + "#" + criterion;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            int b = value & 0xFF;
            builder.append(Character.forDigit((b >>> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }
}
