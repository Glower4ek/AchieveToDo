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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class PhaseAContainerLootExecutionEvidenceValidation {
    static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "container_loot_execution_evidence.json");
    static final Path CATALOG_PATH = Path.of("src", "test", "resources", "phase_a_certification", "container_loot_case_catalog.json");
    static final String SNAPSHOT = "phase_a_container_loot_execution_evidence";
    static final String CATALOG_SNAPSHOT = "phase_a_container_loot_case_catalog";
    static final String MINECRAFT_VERSION = "26.2";
    static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    static final String GREEN = "GREEN";
    static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    static final String AUTOMATION_DEFERRED = "DEFERRED";
    static final String PLAYER_GENERATES_CONTAINER_LOOT_FAMILY = "PLAYER_GENERATES_CONTAINER_LOOT";

    private PhaseAContainerLootExecutionEvidenceValidation() {
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
            String family = entry.get("family").getAsString();
            String lootTable = entry.get("lootTable").getAsString();
            String receiptKey = key(advancementId, criterion);
            if (!actualReceiptKeys.add(receiptKey)) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + receiptKey);
            }
            CatalogCase catalogCase = catalog.get(receiptKey);
            if (catalogCase == null) {
                throw new IllegalStateException("Runtime evidence references missing catalog case: " + advancementId + "#" + criterion);
            }
            requireString(entry, "result", GREEN);
            requireString(entry, "family", PLAYER_GENERATES_CONTAINER_LOOT_FAMILY);
            requireString(entry, "lootTable", catalogCase.lootTable());
            if (!isAutomationEligible(catalogCase.automationEligibility())) {
                throw new IllegalStateException("Runtime evidence references ineligible catalog case: " + advancementId + "#" + criterion);
            }
            RuntimeEvidenceData runtimeEvidence = evidence.computeIfAbsent(advancementId, RuntimeEvidenceData::empty);
            runtimeEvidence.greenCriteria().add(criterion);
            runtimeEvidence.families().add(family);
            runtimeEvidence.lootTables().add(lootTable);
            runtimeEvidence.sources().add(entry.get("source").getAsString());
            runtimeEvidence.criteriaByFamily()
                .computeIfAbsent(family, ignored -> new TreeSet<>())
                .add(criterion);
            runtimeEvidence.criteriaBySource()
                .computeIfAbsent(entry.get("source").getAsString(), ignored -> new TreeSet<>())
                .add(criterion);
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
            String lootTable = caseJson.has("lootTable") && !caseJson.get("lootTable").isJsonNull()
                ? caseJson.get("lootTable").getAsString()
                : null;
            catalog.put(key(advancementId, criterion), new CatalogCase(
                advancementId,
                criterion,
                lootTable,
                caseJson.get("automationEligibility").getAsString()
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

    static boolean isAutomationEligible(String automationEligibility) {
        return AUTOMATION_SUPPORTED.equals(automationEligibility);
    }

    static Set<String> supportedCatalogKeys(Map<String, CatalogCase> catalog) {
        Set<String> supportedKeys = new LinkedHashSet<>();
        for (Map.Entry<String, CatalogCase> entry : catalog.entrySet()) {
            if (isAutomationEligible(entry.getValue().automationEligibility())) {
                supportedKeys.add(entry.getKey());
            }
        }
        return supportedKeys;
    }

    static record RuntimeEvidenceData(
        String advancementId,
        Set<String> greenCriteria,
        Map<String, Set<String>> criteriaByFamily,
        Map<String, Set<String>> criteriaBySource,
        Set<String> families,
        Set<String> lootTables,
        Set<String> sources
    ) {
        static RuntimeEvidenceData empty(String advancementId) {
            return new RuntimeEvidenceData(
                advancementId,
                new TreeSet<>(),
                new LinkedHashMap<>(),
                new LinkedHashMap<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>()
            );
        }
    }

    static record CatalogCase(String advancementId, String criterion, String lootTable, String automationEligibility) {
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
        String value = json.get(key).getAsString();
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Expected nonblank " + key);
        }
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
