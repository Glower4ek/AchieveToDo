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
import java.util.TreeSet;

final class PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation {
    static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "enchantment_inventory_changed_execution_evidence.json");
    static final Path CATALOG_PATH = Path.of("src", "test", "resources", "phase_a_certification", "enchantment_inventory_changed_case_catalog.json");
    static final String SNAPSHOT = "phase_a_enchantment_inventory_changed_execution_evidence";
    static final String CATALOG_SNAPSHOT = "phase_a_enchantment_inventory_changed_case_catalog";
    static final String MINECRAFT_VERSION = "26.2";
    static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    static final String GREEN = "GREEN";
    static final String FAMILY = "ENCHANTMENT_HOLDERSET_INVENTORY_CHANGED";
    static final String SOURCE = "PhaseAEnchantmentInventoryChangedGameTest";
    static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    static final String AUTOMATION_DEFERRED = "DEFERRED";

    private PhaseAEnchantmentInventoryChangedExecutionEvidenceValidation() {
    }

    static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        Map<String, RuntimeEvidenceData> evidence = new LinkedHashMap<>();
        Path evidencePath = projectRoot.resolve(PERSISTENT_ARTIFACT);
        if (!Files.exists(evidencePath)) {
            return evidence;
        }
        Path catalogPath = projectRoot.resolve(CATALOG_PATH);
        String catalogFingerprint = fingerprint(catalogPath);
        Map<String, CatalogCase> catalog = loadCatalog(catalogPath);
        JsonObject root = readJson(evidencePath);
        requireString(root, "snapshot", SNAPSHOT);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "catalogFingerprint", catalogFingerprint);
        requireNonBlank(root, "runId");
        requireNonBlank(root, "generatedAt");
        Set<String> expectedSupportedKeys = supportedCatalogKeys(catalog);
        Set<String> actualReceiptKeys = new LinkedHashSet<>();
        JsonArray entries = root.getAsJsonArray("entries");
        if (entries == null) {
            if (!expectedSupportedKeys.isEmpty()) {
                throw new IllegalStateException("Runtime evidence is missing required SUPPORTED catalog receipts");
            }
            return evidence;
        }
        for (JsonElement entryElement : entries) {
            JsonObject entry = entryElement.getAsJsonObject();
            String advancementId = entry.get("advancementId").getAsString();
            String criterion = entry.get("criterion").getAsString();
            String receiptKey = key(advancementId, criterion);
            if (!actualReceiptKeys.add(receiptKey)) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + receiptKey);
            }
            CatalogCase catalogCase = catalog.get(receiptKey);
            if (catalogCase == null) {
                throw new IllegalStateException("Runtime evidence references missing catalog case: " + receiptKey);
            }
            if (!AUTOMATION_SUPPORTED.equals(catalogCase.automationEligibility())) {
                throw new IllegalStateException("Runtime evidence references deferred catalog case: " + receiptKey);
            }
            requireString(entry, "result", GREEN);
            requireString(entry, "family", FAMILY);
            requireString(entry, "source", SOURCE);
            String selectedItem = entry.get("selectedItem").getAsString();
            requireNonBlank(selectedItem, "selectedItem");
            validateSelectedItem(receiptKey, catalogCase, selectedItem);
            List<ConfiguredEnchantment> configuredEnchantments = readConfiguredEnchantments(entry);
            validateConfiguredEnchantments(receiptKey, catalogCase, configuredEnchantments);
            RuntimeEvidenceData runtimeEvidence = evidence.computeIfAbsent(advancementId, RuntimeEvidenceData::empty);
            runtimeEvidence.greenCriteria().add(criterion);
            runtimeEvidence.families().add(FAMILY);
            runtimeEvidence.sources().add(SOURCE);
            runtimeEvidence.selectedItems().add(selectedItem);
            runtimeEvidence.criteriaByFamily().computeIfAbsent(FAMILY, ignored -> new TreeSet<>()).add(criterion);
            runtimeEvidence.criteriaBySource().computeIfAbsent(SOURCE, ignored -> new TreeSet<>()).add(criterion);
            for (ConfiguredEnchantment configuredEnchantment : configuredEnchantments) {
                runtimeEvidence.configuredEnchantments().add(configuredEnchantment.selector() + "@" + configuredEnchantment.level() + "|" + configuredEnchantment.storageType());
            }
        }
        if (!actualReceiptKeys.equals(expectedSupportedKeys)) {
            throw new IllegalStateException("Runtime evidence receipt keys do not exactly match current SUPPORTED catalog keys");
        }
        return evidence;
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
            List<String> allowedItems = new ArrayList<>();
            JsonArray allowedItemsJson = caseJson.getAsJsonArray("allowedItems");
            if (allowedItemsJson != null) {
                for (JsonElement allowedItemElement : allowedItemsJson) {
                    allowedItems.add(allowedItemElement.getAsString());
                }
            }
            catalog.put(key(advancementId, criterion), new CatalogCase(
                advancementId,
                criterion,
                caseJson.get("automationEligibility").getAsString(),
                allowedItems,
                enchantments
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
            if (expectedEnchantment.minLevel() != null && actualEnchantment.level() < expectedEnchantment.minLevel()) {
                throw new IllegalStateException("Configured enchantment below min level for " + receiptKey + ": " + key);
            }
            if (expectedEnchantment.maxLevel() != null && actualEnchantment.level() > expectedEnchantment.maxLevel()) {
                throw new IllegalStateException("Configured enchantment above max level for " + receiptKey + ": " + key);
            }
        }
    }

    private static void validateSelectedItem(String receiptKey, CatalogCase catalogCase, String selectedItem) {
        if (!catalogCase.allowedItems().isEmpty() && !catalogCase.allowedItems().contains(selectedItem)) {
            throw new IllegalStateException("Selected item is not allowed for " + receiptKey + ": " + selectedItem);
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

    static record RuntimeEvidenceData(
        String advancementId,
        Set<String> greenCriteria,
        Map<String, Set<String>> criteriaByFamily,
        Map<String, Set<String>> criteriaBySource,
        Set<String> families,
        Set<String> sources,
        Set<String> selectedItems,
        Set<String> configuredEnchantments
    ) {
        static RuntimeEvidenceData empty(String advancementId) {
            return new RuntimeEvidenceData(
                advancementId,
                new LinkedHashSet<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>()
            );
        }
    }

    static record CatalogCase(String advancementId, String criterion, String automationEligibility, List<String> allowedItems, List<CatalogEnchantment> enchantments) {
    }

    static record CatalogEnchantment(String selector, String storageType, Integer minLevel, Integer maxLevel) {
    }

    static record ConfiguredEnchantment(String selector, int level, String storageType) {
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

    private static void requireNonBlank(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        requireNonBlank(json.get(key).getAsString(), key);
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
