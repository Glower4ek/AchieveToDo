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
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.resources.Identifier;

/** TEMP recorder and persistent promotion adapter for started_riding evidence. */
public final class PhaseAEntityTypeTagStartedRidingExecutionEvidence {
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
    public static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "entity_type_tag_started_riding_case_catalog.json"
    );
    public static final String SNAPSHOT = "phase_a_entity_type_tag_started_riding_execution_evidence";
    public static final String FAMILY = "ENTITY_TYPE_TAG_STARTED_RIDING";
    public static final String SOURCE = "PhaseAEntityTypeTagStartedRidingGameTest";
    public static final String GREEN = "GREEN";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String TRIGGER = "minecraft:started_riding";
    public static final String SOURCE_ARTIFACT_CLASSIFICATION = "FROZEN_BACAP_DEFINITION";
    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAEntityTypeTagStartedRidingExecutionEvidence() {
    }

    public static Path projectRoot() {
        String configured = System.getProperty(PROJECT_ROOT_PROPERTY);
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Missing system property " + PROJECT_ROOT_PROPERTY + " for GameTest runtime");
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    public static String beginRun(Path root) throws IOException {
        String runId = UUID.randomUUID().toString();
        JsonObject state = new JsonObject();
        state.addProperty("snapshot", SNAPSHOT);
        state.addProperty("schemaVersion", 1);
        state.addProperty("family", FAMILY);
        state.addProperty("source", SOURCE);
        state.addProperty("minecraftVersion", MINECRAFT_VERSION);
        state.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        state.addProperty("catalogFingerprint", currentCatalogFingerprint(root));
        state.addProperty("runId", runId);
        state.addProperty("startedAt", Instant.now().toString());
        writeJson(root.resolve(RUN_STATE_ARTIFACT), state);
        writeJson(root.resolve(TEMPORARY_ARTIFACT), emptyArtifact(runId, currentCatalogFingerprint(root)));
        return runId;
    }

    public static String currentRunId(Path root) throws IOException {
        JsonObject state = readJson(root.resolve(RUN_STATE_ARTIFACT));
        requireString(state, "snapshot", SNAPSHOT);
        requireString(state, "family", FAMILY);
        requireString(state, "source", SOURCE);
        requireString(state, "catalogFingerprint", currentCatalogFingerprint(root));
        return requiredNonBlank(state, "runId");
    }

    public static void recordGreen(Path root, JsonObject receipt) throws IOException {
        String runId = currentRunId(root);
        String fingerprint = currentCatalogFingerprint(root);
        JsonObject artifact = Files.exists(root.resolve(TEMPORARY_ARTIFACT))
            ? readJson(root.resolve(TEMPORARY_ARTIFACT))
            : emptyArtifact(runId, fingerprint);
        requireString(artifact, "snapshot", SNAPSHOT);
        requireString(artifact, "catalogFingerprint", fingerprint);
        requireString(artifact, "runId", runId);
        validateReceipt(receipt, runId, fingerprint);
        String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
        JsonArray entries = requiredArray(artifact, "entries");
        for (JsonElement element : entries) {
            JsonObject existing = element.getAsJsonObject();
            String existingKey = requiredNonBlank(existing, "advancementId") + "#" + requiredNonBlank(existing, "criterion");
            if (key.equals(existingKey)) {
                throw new IllegalStateException("Duplicate started-riding runtime evidence receipt: " + key);
            }
        }
        JsonArray updated = new JsonArray();
        for (JsonElement element : entries) {
            updated.add(element);
        }
        updated.add(receipt.deepCopy());
        List<JsonObject> ordered = new ArrayList<>();
        for (JsonElement element : updated) {
            ordered.add(element.getAsJsonObject());
        }
        ordered.sort(Comparator.comparing(e -> requiredNonBlank(e, "advancementId") + "#" + requiredNonBlank(e, "criterion")));
        JsonArray sorted = new JsonArray();
        ordered.forEach(sorted::add);
        artifact.add("entries", sorted);
        writeJson(root.resolve(TEMPORARY_ARTIFACT), artifact);
    }

    public static RuntimeExecutionArtifact loadValidatedTemporaryArtifact(Path root) throws IOException {
        return loadArtifact(root, false);
    }

    public static RuntimeExecutionArtifact loadValidatedPromotableTemporaryArtifact(Path root) throws IOException {
        return loadArtifact(root, true);
    }

    public static void promote(Path root) throws IOException {
        loadValidatedPromotableTemporaryArtifact(root);
        Path source = root.resolve(TEMPORARY_ARTIFACT);
        Path target = root.resolve(PERSISTENT_ARTIFACT);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public static String currentCatalogFingerprint(Path root) throws IOException {
        try {
            return "sha-256:" + hex(MessageDigest.getInstance("SHA-256").digest(
                Files.readAllBytes(root.resolve(CATALOG_PATH))
            ));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    private static RuntimeExecutionArtifact loadArtifact(Path root, boolean exact) throws IOException {
        String runId = currentRunId(root);
        String fingerprint = currentCatalogFingerprint(root);
        JsonObject artifact = readJson(root.resolve(TEMPORARY_ARTIFACT));
        requireString(artifact, "snapshot", SNAPSHOT);
        requireString(artifact, "family", FAMILY);
        requireString(artifact, "source", SOURCE);
        requireString(artifact, "minecraftVersion", MINECRAFT_VERSION);
        requireString(artifact, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(artifact, "catalogFingerprint", fingerprint);
        requireString(artifact, "runId", runId);
        JsonArray entries = requiredArray(artifact, "entries");
        Set<String> keys = new LinkedHashSet<>();
        for (JsonElement element : entries) {
            JsonObject receipt = element.getAsJsonObject();
            validateReceipt(receipt, runId, fingerprint);
            String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
            if (!keys.add(key)) {
                throw new IllegalStateException("Duplicate started-riding runtime evidence receipt: " + key);
            }
        }
        if (exact && !keys.equals(new LinkedHashSet<>(CertifiedEntityTypeTagStartedRidingCatalog.allCases().stream().map(CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition::key).toList()))) {
            throw new IllegalStateException("Started-riding TEMP artifact does not exactly cover the five catalog cases");
        }
        List<JsonObject> values = new ArrayList<>();
        for (JsonElement element : entries) {
            values.add(element.getAsJsonObject());
        }
        return new RuntimeExecutionArtifact(runId, List.copyOf(values));
    }

    private static void validateReceipt(JsonObject receipt, String runId, String fingerprint) {
        String id = requiredNonBlank(receipt, "advancementId");
        String criterion = requiredNonBlank(receipt, "criterion");
        CertifiedEntityTypeTagStartedRidingCatalog.CaseDefinition definition =
            CertifiedEntityTypeTagStartedRidingCatalog.requiredCase(Identifier.parse(id), criterion);
        requireString(receipt, "family", FAMILY);
        requireString(receipt, "source", SOURCE);
        requireString(receipt, "catalogFingerprint", fingerprint);
        requireString(receipt, "runId", runId);
        requireString(receipt, "minecraftVersion", MINECRAFT_VERSION);
        requireString(receipt, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(receipt, "result", GREEN);
        requireString(receipt, "trigger", TRIGGER);
        requireString(receipt, "entityTypeTag", definition.entityTypeTag());
        requireString(receipt, "selectedEntityType", definition.selectedEntityType());
        requireString(receipt, "sourceArtifactClassification", SOURCE_ARTIFACT_CLASSIFICATION);
        requireInt(receipt, "requirementGroupIndex", definition.requirementGroupIndex());
        requireBoolean(receipt, "runtimeEntityTypeTagMembership", true);
        requirePositive(receipt, "runtimeEntityTypeTagMemberCount");
        requireString(receipt, "playerClass", "net.minecraft.server.level.ServerPlayer");
        requireUuid(receipt, "playerUuid");
        requireNonBlank(receipt, "profileName");
        requireString(receipt, "gameMode", "SURVIVAL");
        requireBoolean(receipt, "joined", true);
        requireBoolean(receipt, "connectionRegistered", true);
        requireBoolean(receipt, "clientLoaded", true);
        requireBoolean(receipt, "normalScheduler", true);
        requireBoolean(receipt, "buildPermission", true);
        requireBoolean(receipt, "wasPassengerBefore", false);
        requireBoolean(receipt, "isPassengerAfter", true);
        requireString(receipt, "actualVehicleTypeAfter", definition.directVehicleType());
        requireBoolean(receipt, "criterionBefore", false);
        requireBoolean(receipt, "criterionAfter", true);
        int ticksToCriterion = requiredInt(receipt, "ticksToCriterion");
        if (ticksToCriterion < 0 || ticksToCriterion > 10) {
            throw new IllegalStateException("Invalid started-riding criterion tick count: " + ticksToCriterion);
        }
        requireBoolean(receipt, "legitimateTrigger", true);
        requireBoolean(receipt, "semanticMutation", true);
        requireString(receipt, "boundary", "Entity.startRiding");
        requireString(receipt, "action", definition.action());
        requireString(receipt, "interactionResult", "SUCCESS");
        JsonArray chain = requiredArray(receipt, "vehicleChainAfter");
        List<String> actualChain = new ArrayList<>();
        for (JsonElement value : chain) {
            actualChain.add(requiredStringValue(value, "vehicleChainAfter"));
        }
        List<String> expectedChain = new ArrayList<>();
        for (String value : definition.vehiclePredicateChain()) {
            expectedChain.add(value.startsWith("#") ? definition.selectedEntityType() : value);
        }
        if (!expectedChain.equals(actualChain)) {
            throw new IllegalStateException("Vehicle chain witness mismatch for " + definition.key());
        }
        if (definition.requiredPassengerType().isBlank()) {
            requireInt(receipt, "requiredPassengerCount", 0);
        } else {
            requireString(receipt, "requiredPassengerTypeAfter", definition.requiredPassengerType());
            requirePositive(receipt, "requiredPassengerCount");
        }
        JsonObject preconditions = requiredObject(receipt, "productionPreconditions");
        requireString(preconditions, "scoreboardObjective", "bac_advancements");
        requireInt(preconditions, "scoreBefore", 0);
        requireInt(preconditions, "scoreAfter", 1000);
        requireBoolean(preconditions, "boatAbilityLockedBefore", true);
        requireBoolean(preconditions, "boatAbilityLockedAfter", false);
        requireBoolean(preconditions, "minecartAbilityLockedBefore", true);
        requireBoolean(preconditions, "minecartAbilityLockedAfter", false);
        requireBoolean(preconditions, "lockedLandmark", false);
    }

    private static JsonObject emptyArtifact(String runId, String fingerprint) {
        JsonObject artifact = new JsonObject();
        artifact.addProperty("snapshot", SNAPSHOT);
        artifact.addProperty("schemaVersion", 1);
        artifact.addProperty("family", FAMILY);
        artifact.addProperty("source", SOURCE);
        artifact.addProperty("minecraftVersion", MINECRAFT_VERSION);
        artifact.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        artifact.addProperty("catalogFingerprint", fingerprint);
        artifact.addProperty("runId", runId);
        artifact.addProperty("generatedAt", Instant.now().toString());
        artifact.add("entries", new JsonArray());
        return artifact;
    }

    private static JsonObject readJson(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing started-riding evidence file: " + path);
        }
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
    }

    private static JsonArray requiredArray(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in started-riding evidence");
        }
        return json.getAsJsonArray(key);
    }

    private static String requiredNonBlank(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull() || json.get(key).getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in started-riding evidence");
        }
        return json.get(key).getAsString();
    }

    private static void requireNonBlank(JsonObject json, String key) {
        requiredNonBlank(json, key);
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

    private static void requireBoolean(JsonObject json, String key, boolean expected) {
        if (!json.has(key) || json.get(key).getAsBoolean() != expected) {
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

    private static void requirePositive(JsonObject json, String key) {
        if (requiredInt(json, key) <= 0) {
            throw new IllegalStateException("Expected positive " + key + " in started-riding evidence");
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

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xF, 16));
            result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
    }

    public record RuntimeExecutionArtifact(String runId, List<JsonObject> entries) {
    }
}
