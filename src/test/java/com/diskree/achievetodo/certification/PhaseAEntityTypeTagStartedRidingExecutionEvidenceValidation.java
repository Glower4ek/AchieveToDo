package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.UUID;

/** Fail-closed validator for ENTITY_TYPE_TAG_STARTED_RIDING runtime receipts. */
public final class PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation {
    public enum Mode {
        TEMP_DIAGNOSTIC,
        TEMP_PROMOTABLE,
        PERSISTENT
    }

    public static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification",
        "entity_type_tag_started_riding_execution_evidence.json"
    );
    public static final Path RUN_STATE_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification",
        "entity_type_tag_started_riding_execution_evidence.run.json"
    );
    public static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "entity_type_tag_started_riding_execution_evidence.json"
    );
    public static final Path CATALOG_PATH = PhaseAEntityTypeTagStartedRidingCertification.SNAPSHOT;
    public static final String SNAPSHOT = "phase_a_entity_type_tag_started_riding_execution_evidence";
    public static final String FAMILY = PhaseAEntityTypeTagStartedRidingCertification.FAMILY;
    public static final String SOURCE = "PhaseAEntityTypeTagStartedRidingGameTest";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String TRIGGER = "minecraft:started_riding";
    public static final String GREEN = "GREEN";
    public static final String BOUNDARY = "Entity.startRiding";
    public static final String SOURCE_ARTIFACT_CLASSIFICATION = "FROZEN_BACAP_DEFINITION";
    public static final int MAX_POLL_TICKS = 10;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation() {
    }

    public static RuntimeArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, Mode.TEMP_DIAGNOSTIC);
    }

    public static RuntimeArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, Mode.TEMP_PROMOTABLE);
    }

    public static RuntimeArtifact validateArtifact(Path projectRoot, Path relativeArtifact, Mode mode) throws IOException {
        return loadValidatedArtifact(projectRoot, relativeArtifact, mode);
    }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        RuntimeArtifact artifact = loadValidatedArtifact(projectRoot, PERSISTENT_ARTIFACT, Mode.PERSISTENT);
        Map<String, RuntimeEvidenceData> result = new LinkedHashMap<>();
        for (RuntimeReceipt receipt : artifact.receipts()) {
            RuntimeEvidenceData data = result.computeIfAbsent(receipt.advancementId(), RuntimeEvidenceData::empty);
            data.greenCriteria().add(receipt.criterion());
            data.criteriaByFamily().computeIfAbsent(receipt.family(), ignored -> new TreeSet<>()).add(receipt.criterion());
            data.criteriaBySource().computeIfAbsent(receipt.source(), ignored -> new TreeSet<>()).add(receipt.criterion());
            data.families().add(receipt.family());
            data.sources().add(receipt.source());
        }
        return result;
    }

    public static String fingerprint(Path catalogPath) throws IOException {
        try {
            return "sha-256:" + hex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(catalogPath)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    private static RuntimeArtifact loadValidatedArtifact(Path projectRoot, Path relativeArtifact, Mode mode) throws IOException {
        Path artifactPath = projectRoot.resolve(relativeArtifact);
        if (!Files.exists(artifactPath)) {
            throw new IllegalStateException("Missing started_riding artifact: " + artifactPath);
        }
        Catalog catalog = loadCatalog(projectRoot);
        String catalogFingerprint = fingerprint(projectRoot.resolve(CATALOG_PATH));
        JsonObject root = readJson(artifactPath);
        requireString(root, "snapshot", SNAPSHOT);
        requireInt(root, "schemaVersion", 1);
        requireString(root, "family", FAMILY);
        requireString(root, "source", SOURCE);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "catalogFingerprint", catalogFingerprint);
        String runId = requiredNonBlank(root, "runId");
        requireNonBlank(root, "generatedAt");
        JsonArray entries = requiredArray(root, "entries");
        Set<String> actualKeys = new LinkedHashSet<>();
        List<RuntimeReceipt> receipts = new ArrayList<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("Started-riding runtime receipt is not an object");
            }
            JsonObject entry = element.getAsJsonObject();
            String advancementId = requiredNonBlank(entry, "advancementId");
            String criterion = requiredNonBlank(entry, "criterion");
            String key = advancementId + "#" + criterion;
            if (!actualKeys.add(key)) {
                throw new IllegalStateException("Duplicate started-riding runtime receipt key: " + key);
            }
            CatalogCase caseDefinition = catalog.byKey().get(key);
            if (caseDefinition == null) {
                throw new IllegalStateException("Unknown started-riding runtime receipt key: " + key);
            }
            validateReceipt(entry, caseDefinition, runId, catalogFingerprint);
            receipts.add(new RuntimeReceipt(advancementId, criterion, requiredNonBlank(entry, "family"), requiredNonBlank(entry, "source")));
        }
        if (mode != Mode.TEMP_DIAGNOSTIC && !actualKeys.equals(catalog.keys())) {
            throw new IllegalStateException("Started-riding runtime evidence does not exactly cover the five-key catalog"
                + " | expected=" + catalog.keys() + " | actual=" + actualKeys);
        }
        return new RuntimeArtifact(runId, requiredNonBlank(root, "generatedAt"), List.copyOf(receipts), mode);
    }

    private static Catalog loadCatalog(Path projectRoot) throws IOException {
        Path path = projectRoot.resolve(CATALOG_PATH);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing started-riding catalog: " + path);
        }
        String actual = Files.readString(path, StandardCharsets.UTF_8);
        JsonElement expected = JsonParser.parseString(PhaseAEntityTypeTagStartedRidingCertification.generateSnapshot(projectRoot));
        JsonElement parsed = JsonParser.parseString(actual);
        if (!expected.equals(parsed)) {
            throw new IllegalStateException("Started-riding catalog is stale or not derived from frozen BACAP semantics");
        }
        JsonObject root = parsed.getAsJsonObject();
        requireString(root, "snapshot", PhaseAEntityTypeTagStartedRidingCertification.SNAPSHOT_ID);
        requireInt(root, "schemaVersion", 1);
        requireString(root, "family", FAMILY);
        requireInt(root, "canonicalAdvancementCount", 1152);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "semanticsSource", PhaseAEntityTypeTagStartedRidingCertification.SEMANTICS_SOURCE);
        requireString(root, "sourceArtifactClassification", SOURCE_ARTIFACT_CLASSIFICATION);
        JsonObject summary = requiredObject(root, "summary");
        requireInt(summary, "totalCases", 5);
        requireInt(summary, "uniqueKeys", 5);
        requireInt(summary, "uniqueAdvancements", 5);
        requireInt(summary, "requirementGroups", 5);
        requireInt(summary, "automationSupported", 5);
        requireInt(summary, "automationDeferred", 0);
        JsonArray entries = requiredArray(root, "cases");
        if (entries.size() != 5) {
            throw new IllegalStateException("Expected five started-riding catalog cases but found " + entries.size());
        }
        Map<String, CatalogCase> byKey = new LinkedHashMap<>();
        List<String> order = new ArrayList<>();
        for (JsonElement element : entries) {
            JsonObject json = element.getAsJsonObject();
            CatalogCase value = CatalogCase.fromJson(json);
            if (byKey.put(value.key(), value) != null) {
                throw new IllegalStateException("Duplicate started-riding catalog key: " + value.key());
            }
            order.add(value.key());
        }
        if (!order.equals(PhaseAEntityTypeTagStartedRidingCertification.EXPECTED_KEYS)) {
            throw new IllegalStateException("Started-riding catalog order/set mismatch: " + order);
        }
        return new Catalog(byKey, new LinkedHashSet<>(order));
    }

    private static void validateReceipt(JsonObject entry, CatalogCase definition, String runId, String fingerprint) {
        String key = definition.key();
        requireString(entry, "family", FAMILY);
        requireString(entry, "source", SOURCE);
        requireString(entry, "catalogFingerprint", fingerprint);
        requireString(entry, "runId", runId);
        requireString(entry, "minecraftVersion", MINECRAFT_VERSION);
        requireString(entry, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(entry, "result", GREEN);
        requireString(entry, "sourceArtifactClassification", SOURCE_ARTIFACT_CLASSIFICATION);
        requireInt(entry, "requirementGroupIndex", 0);
        requireString(entry, "trigger", TRIGGER);
        requireString(entry, "entityTypeTag", definition.entityTypeTag());
        requireString(entry, "selectedEntityType", definition.selectedEntityType());
        requireBoolean(entry, "runtimeEntityTypeTagMembership", true);
        if (requiredInt(entry, "runtimeEntityTypeTagMemberCount") <= 0) {
            throw new IllegalStateException("Expected a non-empty runtime entity-type tag for " + key);
        }
        requireString(entry, "playerClass", "net.minecraft.server.level.ServerPlayer");
        requireUuid(entry, "playerUuid");
        requireNonBlank(entry, "profileName");
        requireString(entry, "gameMode", "SURVIVAL");
        requireBoolean(entry, "joined", true);
        requireBoolean(entry, "connectionRegistered", true);
        requireBoolean(entry, "clientLoaded", true);
        requireBoolean(entry, "normalScheduler", true);
        requireBoolean(entry, "wasPassengerBefore", false);
        requireBoolean(entry, "isPassengerAfter", true);
        requireBoolean(entry, "buildPermission", true);
        requireString(entry, "actualVehicleTypeAfter", definition.directVehicleType());
        requireBoolean(entry, "criterionBefore", false);
        requireBoolean(entry, "criterionAfter", true);
        int ticks = requiredInt(entry, "ticksToCriterion");
        if (ticks < 0 || ticks > MAX_POLL_TICKS) {
            throw new IllegalStateException("Unexpected started-riding criterion tick count for " + key + ": " + ticks);
        }
        requireString(entry, "boundary", BOUNDARY);
        requireString(entry, "action", definition.action());
        requireBoolean(entry, "legitimateTrigger", true);
        requireString(entry, "interactionResult", "SUCCESS");
        requireBoolean(entry, "semanticMutation", true);
        JsonArray chain = requiredArray(entry, "vehicleChainAfter");
        List<String> actualChain = new ArrayList<>();
        for (JsonElement value : chain) {
            actualChain.add(requiredStringValue(value, "vehicleChainAfter"));
        }
        List<String> expectedChain = new ArrayList<>();
        for (String value : definition.vehiclePredicateChain()) {
            expectedChain.add(value.startsWith("#") ? definition.selectedEntityType() : value);
        }
        if (!expectedChain.equals(actualChain)) {
            throw new IllegalStateException("Vehicle chain witness mismatch for " + key + " | expected=" + expectedChain + " | actual=" + actualChain);
        }
        String requiredPassenger = definition.requiredPassengerType();
        if (requiredPassenger.isBlank()) {
            requireInt(entry, "requiredPassengerCount", 0);
        } else {
            requireString(entry, "requiredPassengerTypeAfter", requiredPassenger);
            if (requiredInt(entry, "requiredPassengerCount") <= 0) {
                throw new IllegalStateException("Required passenger was not observed for " + key);
            }
        }
        JsonObject preconditions = requiredObject(entry, "productionPreconditions");
        requireString(preconditions, "scoreboardObjective", "bac_advancements");
        requireInt(preconditions, "scoreBefore", 0);
        requireInt(preconditions, "scoreAfter", 1000);
        requireBoolean(preconditions, "boatAbilityLockedBefore", true);
        requireBoolean(preconditions, "boatAbilityLockedAfter", false);
        requireBoolean(preconditions, "minecartAbilityLockedBefore", true);
        requireBoolean(preconditions, "minecartAbilityLockedAfter", false);
        requireBoolean(preconditions, "lockedLandmark", false);
    }

    private static String requiredNonBlank(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull() || json.get(key).getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in started-riding evidence");
        }
        return json.get(key).getAsString();
    }

    private static String requiredStringValue(JsonElement element, String key) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
            || element.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " value in started-riding evidence");
        }
        return element.getAsString();
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredNonBlank(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in started-riding evidence");
        }
    }

    private static int requiredInt(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing " + key + " in started-riding evidence");
        }
        return json.get(key).getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in started-riding evidence");
        }
    }

    private static void requireBoolean(JsonObject json, String key, boolean expected) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || json.get(key).getAsBoolean() != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in started-riding evidence");
        }
    }

    private static void requireUuid(JsonObject json, String key) {
        try {
            UUID.fromString(requiredNonBlank(json, key));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid UUID in " + key, e);
        }
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in started-riding evidence");
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in started-riding evidence");
        }
        return value.getAsJsonArray();
    }

    private static void requireNonBlank(JsonObject json, String key) {
        requiredNonBlank(json, key);
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xF, 16));
            result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
    }

    private record Catalog(Map<String, CatalogCase> byKey, Set<String> keys) {
    }

    private record CatalogCase(
        String advancementId,
        String criterion,
        int requirementGroupIndex,
        String trigger,
        String entityTypeTag,
        String selectedEntityType,
        String directVehicleType,
        List<String> vehiclePredicateChain,
        String requiredPassengerType,
        String action
    ) {
        static CatalogCase fromJson(JsonObject json) {
            String id = requiredNonBlank(json, "advancementId");
            String criterion = requiredNonBlank(json, "criterion");
            String trigger = requiredNonBlank(json, "trigger");
            if (!TRIGGER.equals(trigger)) {
                throw new IllegalStateException("Unexpected catalog trigger: " + trigger);
            }
            JsonArray chainJson = requiredArray(json, "vehiclePredicateChain");
            List<String> chain = new ArrayList<>();
            for (JsonElement value : chainJson) {
                chain.add(requiredStringValue(value, "vehiclePredicateChain"));
            }
            return new CatalogCase(
                id,
                criterion,
                requiredInt(json, "requirementGroupIndex"),
                trigger,
                requiredNonBlank(json, "entityTypeTag"),
                requiredNonBlank(json, "selectedEntityType"),
                requiredNonBlank(json, "directVehicleType"),
                List.copyOf(chain),
                json.has("requiredPassengerType") ? json.get("requiredPassengerType").getAsString() : "",
                requiredNonBlank(json, "action")
            );
        }

        String key() {
            return advancementId + "#" + criterion;
        }
    }

    public record RuntimeArtifact(String runId, String generatedAt, List<RuntimeReceipt> receipts, Mode mode) {
    }

    public record RuntimeReceipt(String advancementId, String criterion, String family, String source) {
    }

    public static final class RuntimeEvidenceData {
        private final String advancementId;
        private final Set<String> greenCriteria = new TreeSet<>();
        private final Map<String, Set<String>> criteriaByFamily = new LinkedHashMap<>();
        private final Map<String, Set<String>> criteriaBySource = new LinkedHashMap<>();
        private final Set<String> families = new LinkedHashSet<>();
        private final Set<String> sources = new LinkedHashSet<>();

        private RuntimeEvidenceData(String advancementId) {
            this.advancementId = advancementId;
        }

        private static RuntimeEvidenceData empty(String advancementId) {
            return new RuntimeEvidenceData(advancementId);
        }

        public String advancementId() { return advancementId; }
        public Set<String> greenCriteria() { return greenCriteria; }
        public Map<String, Set<String>> criteriaByFamily() { return criteriaByFamily; }
        public Map<String, Set<String>> criteriaBySource() { return criteriaBySource; }
        public Set<String> families() { return families; }
        public Set<String> sources() { return sources; }
    }
}
