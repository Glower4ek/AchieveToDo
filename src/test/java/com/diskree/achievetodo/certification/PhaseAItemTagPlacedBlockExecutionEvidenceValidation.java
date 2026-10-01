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

final class PhaseAItemTagPlacedBlockExecutionEvidenceValidation {
    static final Path TEMPORARY_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "item_tag_placed_block_execution_evidence.json");
    static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_placed_block_execution_evidence.json");
    static final Path CATALOG_PATH = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_placed_block_case_catalog.json");
    static final String SNAPSHOT = "phase_a_item_tag_placed_block_execution_evidence";
    static final String CATALOG_SNAPSHOT = "phase_a_item_tag_placed_block_case_catalog";
    static final String MINECRAFT_VERSION = "26.2";
    static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    static final String GREEN = "GREEN";
    static final String FAMILY = "ITEM_TAG_PLACED_BLOCK";
    static final String SOURCE = "PhaseAItemTagPlacedBlockGameTest";
    static final String AUTOMATION_SUPPORTED = "SUPPORTED";

    private PhaseAItemTagPlacedBlockExecutionEvidenceValidation() {
    }

    static RuntimeArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, false);
    }

    static RuntimeArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, true);
    }

    static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        RuntimeArtifact artifact = loadValidatedArtifact(projectRoot, PERSISTENT_ARTIFACT, true);
        Map<String, RuntimeEvidenceData> evidence = new LinkedHashMap<>();
        for (RuntimeReceipt receipt : artifact.receipts()) {
            RuntimeEvidenceData runtimeEvidence = evidence.computeIfAbsent(receipt.advancementId(), RuntimeEvidenceData::empty);
            runtimeEvidence.greenCriteria().add(receipt.criterion());
            runtimeEvidence.criteriaByFamily().computeIfAbsent(receipt.family(), ignored -> new TreeSet<>()).add(receipt.criterion());
            runtimeEvidence.criteriaBySource().computeIfAbsent(receipt.source(), ignored -> new TreeSet<>()).add(receipt.criterion());
            runtimeEvidence.families().add(receipt.family());
            runtimeEvidence.sources().add(receipt.source());
        }
        return evidence;
    }

    private static RuntimeArtifact loadValidatedArtifact(Path projectRoot, Path relativeArtifactPath, boolean requireExactCoverage) throws IOException {
        Path artifactPath = projectRoot.resolve(relativeArtifactPath);
        if (!Files.exists(artifactPath)) {
            throw new IllegalStateException("Missing " + relativeArtifactPath + " ITEM_TAG_PLACED_BLOCK artifact");
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
                if (!AUTOMATION_SUPPORTED.equals(catalogCase.automationEligibility())) {
                    throw new IllegalStateException("Runtime evidence references deferred catalog case: " + receiptKey);
                }
                requireString(entry, "result", GREEN);
                requireString(entry, "family", FAMILY);
                requireString(entry, "source", SOURCE);
                requireString(entry, "catalogFingerprint", catalogFingerprint);
                requireString(entry, "minecraftVersion", MINECRAFT_VERSION);
                requireString(entry, "compatibilityMarker", COMPATIBILITY_MARKER);
                requireString(entry, "runId", root.get("runId").getAsString());
                requireBoolean(entry, "criterionBefore", false);
                requireBoolean(entry, "criterionAfter", true);
                requireInt(entry, "handCountBefore", 1);
                requireInt(entry, "handCountAfter", 0);
                requireBoolean(entry, "tagMembership", true);
                requirePositive(entry, "tagMemberCount");
                requireNonNegative(entry, "ticksToCriterion");
                requireString(entry, "placedBlockBefore", "minecraft:air");
                String itemTag = requiredNonBlank(entry, "itemTag");
                String selectedItem = requiredNonBlank(entry, "selectedItem");
                String placedBlock = requiredNonBlank(entry, "placedBlock");
                String interactionResult = requiredNonBlank(entry, "interactionResult");
                String targetPos = requiredNonBlank(entry, "targetPos");
                String supportPos = requiredNonBlank(entry, "supportPos");
                String placedBlockAfter = requiredNonBlank(entry, "placedBlockAfter");
                if ("minecraft:air".equals(placedBlockAfter)) {
                    throw new IllegalStateException("Runtime evidence placedBlockAfter cannot be minecraft:air for " + receiptKey);
                }
                if (!placedBlock.equals(placedBlockAfter)) {
                    throw new IllegalStateException("Runtime evidence placedBlock mismatch for " + receiptKey);
                }
                if (!catalogCase.itemTag().equals(itemTag)) {
                    throw new IllegalStateException("Runtime evidence itemTag mismatch for " + receiptKey);
                }
                receipts.add(new RuntimeReceipt(
                    advancementId,
                    criterion,
                    itemTag,
                    selectedItem,
                    placedBlock,
                    interactionResult,
                    targetPos,
                    supportPos,
                    entry.get("tagMemberCount").getAsInt(),
                    entry.get("ticksToCriterion").getAsInt(),
                    FAMILY,
                    SOURCE,
                    GREEN
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
            catalog.put(key(advancementId, criterion), new CatalogCase(
                advancementId,
                criterion,
                caseJson.get("itemTag").getAsString(),
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

    static Set<String> supportedCatalogKeys(Map<String, CatalogCase> catalog) {
        Set<String> supportedKeys = new LinkedHashSet<>();
        for (Map.Entry<String, CatalogCase> entry : catalog.entrySet()) {
            if (AUTOMATION_SUPPORTED.equals(entry.getValue().automationEligibility())) {
                supportedKeys.add(entry.getKey());
            }
        }
        return supportedKeys;
    }

    static record CatalogCase(String advancementId, String criterion, String itemTag, String automationEligibility) {
    }

    static record RuntimeReceipt(
        String advancementId,
        String criterion,
        String itemTag,
        String selectedItem,
        String placedBlock,
        String interactionResult,
        String targetPos,
        String supportPos,
        int tagMemberCount,
        int ticksToCriterion,
        String family,
        String source,
        String result
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
                new TreeSet<>(),
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

    private static void requireInt(JsonObject json, String key, int expectedValue) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        if (json.get(key).getAsInt() != expectedValue) {
            throw new IllegalStateException("Expected " + key + "=" + expectedValue + " but got " + json.get(key).getAsInt());
        }
    }

    private static void requirePositive(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        if (json.get(key).getAsInt() <= 0) {
            throw new IllegalStateException("Expected " + key + ">0");
        }
    }

    private static void requireNonNegative(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        if (json.get(key).getAsInt() < 0) {
            throw new IllegalStateException("Expected " + key + ">=0");
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
