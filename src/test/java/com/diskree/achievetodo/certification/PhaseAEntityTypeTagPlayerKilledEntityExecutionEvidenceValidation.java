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

final class PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation {
    static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "reports", "phase-a-certification", "entity_type_tag_player_killed_entity_execution_evidence.json"
    );
    static final Path RUN_STATE_ARTIFACT = Path.of(
        "build", "reports", "phase-a-certification", "entity_type_tag_player_killed_entity_execution_evidence.run.json"
    );
    static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification", "entity_type_tag_player_killed_entity_execution_evidence.json"
    );
    static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification", "entity_type_tag_player_killed_entity_case_catalog.json"
    );
    static final String SNAPSHOT = "phase_a_entity_type_tag_player_killed_entity_execution_evidence";
    static final String CATALOG_SNAPSHOT = "phase_a_entity_type_tag_player_killed_entity_case_catalog";
    static final String MINECRAFT_VERSION = "26.2";
    static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    static final String GREEN = "GREEN";
    static final String FAMILY = "ENTITY_TYPE_TAG_PLAYER_KILLED_ENTITY";
    static final String SOURCE = "PhaseAEntityTypeTagPlayerKilledEntityGameTest";
    static final String AUTOMATION_SUPPORTED = "SUPPORTED";

    private PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation() {
    }

    static RuntimeArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, false, true);
    }

    static RuntimeArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, true, true);
    }

    static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        RuntimeArtifact artifact = loadValidatedArtifact(projectRoot, PERSISTENT_ARTIFACT, true, false);
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

    private static RuntimeArtifact loadValidatedArtifact(
        Path projectRoot,
        Path relativeArtifactPath,
        boolean requireExactCoverage,
        boolean requireActiveRunStateAlignment
    ) throws IOException {
        Path artifactPath = projectRoot.resolve(relativeArtifactPath);
        if (!Files.exists(artifactPath)) {
            throw new IllegalStateException("Missing " + relativeArtifactPath + " ENTITY_TYPE_TAG_PLAYER_KILLED_ENTITY artifact");
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
        Path runStatePath = projectRoot.resolve(RUN_STATE_ARTIFACT);
        if (requireActiveRunStateAlignment && Files.exists(runStatePath)) {
            JsonObject runState = readJson(runStatePath);
            requireString(runState, "snapshot", SNAPSHOT);
            requireString(runState, "minecraftVersion", MINECRAFT_VERSION);
            requireString(runState, "compatibilityMarker", COMPATIBILITY_MARKER);
            requireString(runState, "catalogFingerprint", catalogFingerprint);
            requireString(root, "runId", runState.get("runId").getAsString());
        }

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
                requireString(entry, "trigger", catalogCase.trigger());
                requireString(entry, "entityTypeTag", catalogCase.entityTypeTag());
                requireBoolean(entry, "tagExists", true);
                requireBoolean(entry, "tagMembership", true);
                requirePositive(entry, "tagMemberCount");
                requireBoolean(entry, "criterionBefore", false);
                requireBoolean(entry, "criterionAfter", true);
                requireBoolean(entry, "entityAliveBefore", true);
                boolean entityAliveAfter = requiredBoolean(entry, "entityAliveAfter");
                boolean entityRemovedAfter = requiredBoolean(entry, "entityRemovedAfter");
                if (entityAliveAfter) {
                    throw new IllegalStateException("Runtime evidence did not prove the witness was killed for " + receiptKey);
                }
                requireBoolean(entry, "playerKillAttributed", true);
                requireNonNegative(entry, "ticksToCompletion");
                requireNonBlank(entry, "selectedEntityType");
                requireNonBlank(entry, "combatBoundary");
                requireNonBlank(entry, "combatAction");
                if (catalogCase.entityTypeTag().equals(requiredNonBlank(entry, "selectedEntityType"))) {
                    throw new IllegalStateException("selectedEntityType must be an entity id, not the tag id, for " + receiptKey);
                }
                receipts.add(new RuntimeReceipt(
                    advancementId,
                    criterion,
                    catalogCase.trigger(),
                    catalogCase.entityTypeTag(),
                    entry.get("selectedEntityType").getAsString(),
                    entry.get("tagMemberCount").getAsInt(),
                    entry.get("criterionBefore").getAsBoolean(),
                    entry.get("criterionAfter").getAsBoolean(),
                    entry.get("entityAliveBefore").getAsBoolean(),
                    entityAliveAfter,
                    entityRemovedAfter,
                    entry.get("playerKillAttributed").getAsBoolean(),
                    entry.get("ticksToCompletion").getAsInt(),
                    entry.get("combatBoundary").getAsString(),
                    entry.get("combatAction").getAsString(),
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
                caseJson.get("trigger").getAsString(),
                caseJson.get("entityTypeTag").getAsString(),
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

    static record CatalogCase(String advancementId, String criterion, String trigger, String entityTypeTag, String automationEligibility) {
    }

    static record RuntimeReceipt(
        String advancementId,
        String criterion,
        String trigger,
        String entityTypeTag,
        String selectedEntityType,
        int tagMemberCount,
        boolean criterionBefore,
        boolean criterionAfter,
        boolean entityAliveBefore,
        boolean entityAliveAfter,
        boolean entityRemovedAfter,
        boolean playerKillAttributed,
        int ticksToCompletion,
        String combatBoundary,
        String combatAction,
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

    private static boolean requiredBoolean(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        return json.get(key).getAsBoolean();
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
