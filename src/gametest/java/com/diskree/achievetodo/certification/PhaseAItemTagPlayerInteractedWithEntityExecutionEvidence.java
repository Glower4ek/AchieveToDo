package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.Identifier;

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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** TEMP recorder, validator, and persistent promotion adapter for this family. */
public final class PhaseAItemTagPlayerInteractedWithEntityExecutionEvidence {
    public static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification",
        "item_tag_player_interacted_with_entity_execution_evidence.json"
    );
    public static final Path RUN_STATE_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification",
        "item_tag_player_interacted_with_entity_execution_evidence.run.json"
    );
    public static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "item_tag_player_interacted_with_entity_execution_evidence.json"
    );
    public static final Path CATALOG_PATH = CertifiedItemTagPlayerInteractedWithEntityCatalog.CATALOG_PATH;
    public static final String SNAPSHOT = "phase_a_item_tag_player_interacted_with_entity_execution_evidence";
    public static final String FAMILY = "ITEM_TAG_PLAYER_INTERACTED_WITH_ENTITY";
    public static final String SOURCE = "PhaseAItemTagPlayerInteractedWithEntityGameTest";
    public static final String GREEN = "GREEN";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String TRIGGER = "minecraft:player_interacted_with_entity";
    public static final String BOUNDARY = "ServerGamePacketListenerImpl.handleInteract";
    public static final String PACKET_PATH = "ServerboundInteractPacket.handle->ServerGamePacketListenerImpl.handleInteract->CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY";
    public static final String SOURCE_ARTIFACT_CLASSIFICATION = "FROZEN_BACAP_DEFINITION";
    private static final int SCHEMA_VERSION = 1;
    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAItemTagPlayerInteractedWithEntityExecutionEvidence() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = args.length > 1 ? Path.of(args[1]).toAbsolutePath().normalize() : projectRoot();
        System.setProperty(PROJECT_ROOT_PROPERTY, projectRoot.toString());
        if (args.length > 0 && "reset".equals(args[0])) {
            resetRun(projectRoot);
            return;
        }
        if (args.length > 0 && "promote".equals(args[0])) {
            promote(projectRoot);
            return;
        }
        throw new IllegalArgumentException("Expected promote or reset command");
    }

    public static Path projectRoot() {
        String configured = System.getProperty(PROJECT_ROOT_PROPERTY);
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Missing system property " + PROJECT_ROOT_PROPERTY + " for GameTest runtime");
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    public static void resetRun(Path root) throws IOException {
        beginRun(root);
    }

    public static String beginRun(Path root) throws IOException {
        String runId = UUID.randomUUID().toString();
        String fingerprint = currentCatalogFingerprint(root);
        JsonObject state = new JsonObject();
        state.addProperty("snapshot", SNAPSHOT);
        state.addProperty("schemaVersion", SCHEMA_VERSION);
        state.addProperty("family", FAMILY);
        state.addProperty("source", SOURCE);
        state.addProperty("minecraftVersion", MINECRAFT_VERSION);
        state.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        state.addProperty("catalogFingerprint", fingerprint);
        state.addProperty("runId", runId);
        state.addProperty("startedAt", Instant.now().toString());
        writeJson(root.resolve(RUN_STATE_ARTIFACT), state);
        writeJson(root.resolve(TEMPORARY_ARTIFACT), emptyArtifact(runId, fingerprint));
        return runId;
    }

    public static String currentRunId(Path root) throws IOException {
        JsonObject state = readJson(root.resolve(RUN_STATE_ARTIFACT));
        requireString(state, "snapshot", SNAPSHOT);
        requireInt(state, "schemaVersion", SCHEMA_VERSION);
        requireString(state, "family", FAMILY);
        requireString(state, "source", SOURCE);
        requireString(state, "minecraftVersion", MINECRAFT_VERSION);
        requireString(state, "compatibilityMarker", COMPATIBILITY_MARKER);
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
        requireInt(artifact, "schemaVersion", SCHEMA_VERSION);
        requireString(artifact, "family", FAMILY);
        requireString(artifact, "source", SOURCE);
        requireString(artifact, "minecraftVersion", MINECRAFT_VERSION);
        requireString(artifact, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(artifact, "catalogFingerprint", fingerprint);
        requireString(artifact, "runId", runId);
        validateReceipt(receipt, runId, fingerprint);
        String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
        JsonArray entries = requiredArray(artifact, "entries");
        for (JsonElement element : entries) {
            JsonObject existing = element.getAsJsonObject();
            String existingKey = requiredNonBlank(existing, "advancementId") + "#" + requiredNonBlank(existing, "criterion");
            if (key.equals(existingKey)) {
                throw new IllegalStateException("Duplicate interaction runtime evidence receipt: " + key);
            }
        }
        List<JsonObject> ordered = new ArrayList<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("Interaction runtime evidence entry is not an object");
            }
            ordered.add(element.getAsJsonObject());
        }
        ordered.add(receipt.deepCopy());
        ordered.sort(Comparator.comparing(e -> requiredNonBlank(e, "advancementId") + "#" + requiredNonBlank(e, "criterion")));
        JsonArray sorted = new JsonArray();
        ordered.forEach(sorted::add);
        artifact.add("entries", sorted);
        writeJson(root.resolve(TEMPORARY_ARTIFACT), artifact);
    }

    public static RuntimeExecutionArtifact loadValidatedTemporaryArtifact(Path root) throws IOException {
        return loadArtifact(root, TEMPORARY_ARTIFACT, Mode.TEMP_DIAGNOSTIC, true);
    }

    public static RuntimeExecutionArtifact loadValidatedPromotableTemporaryArtifact(Path root) throws IOException {
        return loadArtifact(root, TEMPORARY_ARTIFACT, Mode.TEMP_PROMOTABLE, true);
    }

    public static RuntimeExecutionArtifact loadValidatedPersistentArtifact(Path root) throws IOException {
        return loadArtifact(root, PERSISTENT_ARTIFACT, Mode.PERSISTENT, false);
    }

    public static RuntimeExecutionArtifact promote(Path root) throws IOException {
        RuntimeExecutionArtifact artifact = loadValidatedPromotableTemporaryArtifact(root);
        Path target = root.resolve(PERSISTENT_ARTIFACT);
        if (Files.exists(target)) {
            throw new IllegalStateException("Refusing to overwrite existing persistent ITEM_TAG_PLAYER_INTERACTED_WITH_ENTITY evidence");
        }
        Files.createDirectories(target.getParent());
        Files.copy(root.resolve(TEMPORARY_ARTIFACT), target, StandardCopyOption.COPY_ATTRIBUTES);
        return artifact;
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

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        RuntimeExecutionArtifact artifact = loadValidatedPersistentArtifact(root);
        Map<String, RuntimeEvidenceData> evidence = new LinkedHashMap<>();
        for (JsonObject receipt : artifact.entries()) {
            String advancementId = requiredNonBlank(receipt, "advancementId");
            RuntimeEvidenceData data = evidence.computeIfAbsent(advancementId, RuntimeEvidenceData::empty);
            String criterion = requiredNonBlank(receipt, "criterion");
            String family = requiredNonBlank(receipt, "family");
            String source = requiredNonBlank(receipt, "source");
            data.greenCriteria().add(criterion);
            data.criteriaByFamily().computeIfAbsent(family, ignored -> new TreeSet<>()).add(criterion);
            data.criteriaBySource().computeIfAbsent(source, ignored -> new TreeSet<>()).add(criterion);
            data.families().add(family);
            data.sources().add(source);
        }
        return evidence;
    }

    private static RuntimeExecutionArtifact loadArtifact(
        Path root,
        Path relativePath,
        Mode mode,
        boolean alignActiveRun
    ) throws IOException {
        String activeRunId = alignActiveRun ? currentRunId(root) : null;
        String fingerprint = currentCatalogFingerprint(root);
        JsonObject artifactJson = readJson(root.resolve(relativePath));
        RuntimeExecutionArtifact artifact = parseArtifact(artifactJson, mode);
        requireEquals(artifact.catalogFingerprint(), fingerprint, "catalogFingerprint");
        if (activeRunId != null) {
            requireEquals(artifact.runId(), activeRunId, "runId");
        }
        validateArtifact(artifact, fingerprint, mode != Mode.TEMP_DIAGNOSTIC);
        return artifact;
    }

    private static RuntimeExecutionArtifact parseArtifact(JsonObject json, Mode mode) {
        requireString(json, "snapshot", SNAPSHOT);
        requireInt(json, "schemaVersion", SCHEMA_VERSION);
        requireString(json, "family", FAMILY);
        requireString(json, "source", SOURCE);
        requireString(json, "minecraftVersion", MINECRAFT_VERSION);
        requireString(json, "compatibilityMarker", COMPATIBILITY_MARKER);
        String fingerprint = requiredNonBlank(json, "catalogFingerprint");
        String runId = requiredNonBlank(json, "runId");
        String generatedAt = requiredNonBlank(json, "generatedAt");
        JsonArray entriesJson = requiredArray(json, "entries");
        List<JsonObject> entries = new ArrayList<>();
        for (JsonElement element : entriesJson) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("Interaction evidence entry is not an object");
            }
            entries.add(element.getAsJsonObject());
        }
        return new RuntimeExecutionArtifact(fingerprint, runId, generatedAt, List.copyOf(entries), mode);
    }

    private static void validateArtifact(RuntimeExecutionArtifact artifact, String fingerprint, boolean exactCoverage) {
        requireNonBlank(artifact.catalogFingerprint(), "catalogFingerprint");
        requireEquals(artifact.catalogFingerprint(), fingerprint, "catalogFingerprint");
        requireNonBlank(artifact.runId(), "runId");
        requireNonBlank(artifact.generatedAt(), "generatedAt");
        Map<String, CertifiedItemTagPlayerInteractedWithEntityCatalog.CaseDefinition> catalog = new LinkedHashMap<>();
        for (CertifiedItemTagPlayerInteractedWithEntityCatalog.CaseDefinition definition :
            CertifiedItemTagPlayerInteractedWithEntityCatalog.allCases()) {
            if (catalog.put(definition.key(), definition) != null) {
                throw new IllegalStateException("Duplicate interaction catalog key: " + definition.key());
            }
        }
        Set<String> actualKeys = new LinkedHashSet<>();
        for (JsonObject receipt : artifact.entries()) {
            String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
            if (!actualKeys.add(key)) {
                throw new IllegalStateException("Duplicate interaction runtime evidence receipt key: " + key);
            }
            CertifiedItemTagPlayerInteractedWithEntityCatalog.CaseDefinition definition = catalog.get(key);
            if (definition == null) {
                throw new IllegalStateException("Unknown interaction runtime evidence receipt key: " + key);
            }
            validateReceipt(receipt, artifact.runId(), fingerprint);
        }
        if (exactCoverage && !actualKeys.equals(catalog.keySet())) {
            throw new IllegalStateException("Interaction runtime evidence does not exactly cover the four catalog cases"
                + " | expected=" + catalog.keySet() + " | actual=" + actualKeys);
        }
    }

    private static void validateReceipt(JsonObject receipt, String runId, String fingerprint) {
        String advancementId = requiredNonBlank(receipt, "advancementId");
        String criterion = requiredNonBlank(receipt, "criterion");
        CertifiedItemTagPlayerInteractedWithEntityCatalog.CaseDefinition definition;
        try {
            definition = CertifiedItemTagPlayerInteractedWithEntityCatalog.requiredCase(Identifier.parse(advancementId), criterion);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Unknown interaction receipt case " + advancementId + "#" + criterion, e);
        }
        requireString(receipt, "family", FAMILY);
        requireString(receipt, "source", SOURCE);
        requireString(receipt, "catalogFingerprint", fingerprint);
        requireString(receipt, "runId", runId);
        requireString(receipt, "minecraftVersion", MINECRAFT_VERSION);
        requireString(receipt, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(receipt, "result", GREEN);
        requireInt(receipt, "requirementGroupIndex", definition.requirementGroupIndex());
        requireString(receipt, "trigger", TRIGGER);
        requireString(receipt, "itemTag", definition.itemTag());
        requireString(receipt, "selectedItem", definition.selectedItem());
        requireBoolean(receipt, "runtimeItemTagMembership", true);
        requirePositive(receipt, "runtimeItemTagMemberCount");
        JsonArray tagMembers = requiredArray(receipt, "runtimeItemTagMembers");
        if (tagMembers.size() != requiredInt(receipt, "runtimeItemTagMemberCount")) {
            throw new IllegalStateException("Runtime item-tag member count/list mismatch for " + definition.key());
        }
        boolean selectedTagMember = false;
        for (JsonElement member : tagMembers) {
            String value = requiredStringValue(member, "runtimeItemTagMembers");
            if (definition.selectedItem().equals(value)) {
                selectedTagMember = true;
            }
        }
        if (!selectedTagMember) {
            throw new IllegalStateException("Selected item is absent from runtime HolderSet witness for " + definition.key());
        }
        requireString(receipt, "selectedEntityType", definition.selectedEntityType());
        requireString(receipt, "observedEntityType", definition.selectedEntityType());
        requirePositive(receipt, "targetEntityId");
        requireUuid(receipt, "targetEntityUuid");
        requireBoolean(receipt, "targetAliveBefore", true);
        requireBoolean(receipt, "targetAliveAfter", true);
        requireBoolean(receipt, "entityPredicateSatisfied", true);

        requireString(receipt, "boundary", BOUNDARY);
        requireString(receipt, "packetPath", PACKET_PATH);
        requireString(receipt, "hand", "MAIN_HAND");
        requireString(receipt, "playerClass", "net.minecraft.server.level.ServerPlayer");
        requireUuid(receipt, "playerUuid");
        requireNonBlank(receipt, "profileName");
        requireString(receipt, "gameMode", "SURVIVAL");
        requireBoolean(receipt, "joined", true);
        requireBoolean(receipt, "connectionRegistered", true);
        requireBoolean(receipt, "clientLoaded", true);
        requireBoolean(receipt, "normalScheduler", true);
        requireBoolean(receipt, "criterionBefore", false);
        requireBoolean(receipt, "criterionAfter", true);
        requireBoolean(receipt, "interactionPerformed", true);
        requireBoolean(receipt, "legitimateTrigger", true);
        requireString(receipt, "interactionResult", "SUCCESS");
        requireBoolean(receipt, "interactionConsumesAction", true);
        int ticks = requiredInt(receipt, "ticksToCriterion");
        if (ticks < 0 || ticks > 10) {
            throw new IllegalStateException("Invalid interaction criterion tick count for " + definition.key());
        }

        JsonObject preconditions = requiredObject(receipt, "productionPreconditions");
        requireString(preconditions, "scoreboardObjective", "bac_advancements");
        requireString(preconditions, "ability", definition.productionPrecondition());
        requireBoolean(preconditions, "lockedLandmark", false);
        if ("USE_FLINT_AND_STEEL_UNLOCKED".equals(definition.productionPrecondition())) {
            requireInt(preconditions, "scoreBefore", 0);
            requireInt(preconditions, "scoreAfter", 1000);
            requireBoolean(preconditions, "abilityLockedBefore", true);
            requireBoolean(preconditions, "abilityLockedAfter", false);
        } else {
            requireInt(preconditions, "scoreBefore", 0);
            requireInt(preconditions, "scoreAfter", 0);
            requireBoolean(preconditions, "abilityLockedBefore", false);
            requireBoolean(preconditions, "abilityLockedAfter", false);
        }

        JsonObject stack = requiredObject(receipt, "interactionStack");
        requireString(stack, "itemBefore", definition.selectedItem());
        requireString(stack, "itemAfter", definition.selectedItem().equals(stack.get("itemAfter").getAsString())
            ? definition.selectedItem() : "minecraft:air");
        requireInt(stack, "countBefore", 1);
        int countAfter = requiredInt(stack, "countAfter");
        int damageBefore = requiredInt(stack, "damageBefore");
        int damageAfter = requiredInt(stack, "damageAfter");
        if ("IGNITE_CREEPER".equals(definition.action())) {
            requireInt(stack, "countAfter", 1);
            if (damageAfter != damageBefore + 1) {
                throw new IllegalStateException("Flint-and-steel durability did not increase by one for " + definition.key());
            }
        } else {
            requireInt(stack, "countAfter", 0);
            if (damageAfter != damageBefore) {
                throw new IllegalStateException("Food interaction unexpectedly changed durability for " + definition.key());
            }
        }
        requireNonBlank(stack, "snapshotBefore");
        requireNonBlank(stack, "snapshotAfter");
        requireBooleanField(stack, "sameStackReference");

        JsonObject before = requiredObject(receipt, "entityStateBefore");
        JsonObject after = requiredObject(receipt, "entityStateAfter");
        requireString(before, "type", definition.selectedEntityType());
        requireString(after, "type", definition.selectedEntityType());
        requireBoolean(before, "alive", true);
        requireBoolean(after, "alive", true);
        requirePositive(before, "entityId");
        requirePositive(after, "entityId");
        requireUuid(before, "uuid");
        requireUuid(after, "uuid");
        requireString(before, "uuid", requiredNonBlank(after, "uuid"));
        if ("FEED_DOLPHIN".equals(definition.action())) {
            requireBoolean(after, "gotFish", true);
        } else if ("IGNITE_CREEPER".equals(definition.action())) {
            requireBoolean(before, "ignited", false);
            requireBoolean(after, "ignited", true);
        } else if ("FEED_BABY_SNIFFER".equals(definition.action())) {
            requireBoolean(before, "isBaby", true);
            requireBoolean(after, "isBaby", true);
        }

        JsonObject predicate = requiredObject(receipt, "entityPredicateObservation");
        requireString(predicate, "requiredEntityType", definition.selectedEntityType());
        requireString(predicate, "observedEntityType", definition.selectedEntityType());
        requireBoolean(predicate, "satisfied", true);
        JsonObject proof = requiredObject(receipt, "actionProof");
        requireString(proof, "action", definition.action());
        requireBoolean(proof, "realPacketPath", true);
        requireBoolean(proof, "noDirectCriterionTrigger", true);
        requireBoolean(proof, "noManualAward", true);
        requireBoolean(proof, "itemTagWitness", true);
        requireBoolean(proof, "targetIdentityWitness", true);
        requireBoolean(proof, "criterionBefore", false);
        requireBoolean(proof, "criterionAfter", true);

        JsonObject cleanup = requiredObject(receipt, "cleanup");
        requireBoolean(cleanup, "playerRemoved", true);
        requireBoolean(cleanup, "connectionRemoved", true);
        requireBoolean(cleanup, "channelSettled", true);
        requireInt(cleanup, "settlementMessages", 0, false);
        requireInt(cleanup, "warningCount", 0);
    }

    private static JsonObject emptyArtifact(String runId, String fingerprint) {
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT);
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.addProperty("family", FAMILY);
        root.addProperty("source", SOURCE);
        root.addProperty("minecraftVersion", MINECRAFT_VERSION);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("catalogFingerprint", fingerprint);
        root.addProperty("runId", runId);
        root.addProperty("generatedAt", Instant.now().toString());
        root.add("entries", new JsonArray());
        return root;
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static JsonObject readJson(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing interaction evidence file: " + path);
        }
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in interaction evidence");
        }
        return value.getAsJsonArray();
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in interaction evidence");
        }
        return value.getAsJsonObject();
    }

    private static String requiredNonBlank(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull() || json.get(key).getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in interaction evidence");
        }
        return json.get(key).getAsString();
    }

    private static String requiredStringValue(JsonElement element, String key) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
            || element.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " value in interaction evidence");
        }
        return element.getAsString();
    }

    private static void requireNonBlank(JsonObject json, String key) {
        requiredNonBlank(json, key);
    }

    private static void requireNonBlank(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Expected nonblank " + key + " in interaction evidence");
        }
    }

    private static int requiredInt(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing " + key + " in interaction evidence");
        }
        return json.get(key).getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        requireInt(json, key, expected, true);
    }

    private static void requireInt(JsonObject json, String key, int expected, boolean exact) {
        int actual = requiredInt(json, key);
        if (exact ? actual != expected : actual < expected) {
            throw new IllegalStateException("Unexpected " + key + " in interaction evidence: " + actual);
        }
    }

    private static void requirePositive(JsonObject json, String key) {
        if (requiredInt(json, key) <= 0) {
            throw new IllegalStateException("Expected positive " + key + " in interaction evidence");
        }
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredNonBlank(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in interaction evidence");
        }
    }

    private static void requireEquals(String actual, String expected, String field) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + field + "=" + expected + " but got " + actual);
        }
    }

    private static void requireBoolean(JsonObject json, String key, boolean expected) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isBoolean()
            || json.get(key).getAsBoolean() != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in interaction evidence");
        }
    }

    private static void requireBooleanField(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isBoolean()) {
            throw new IllegalStateException("Expected boolean " + key + " in interaction evidence");
        }
    }

    private static void requireUuid(JsonObject json, String key) {
        try {
            UUID.fromString(requiredNonBlank(json, key));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid UUID in " + key + " in interaction evidence", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xF, 16));
            result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
    }

    public enum Mode {
        TEMP_DIAGNOSTIC,
        TEMP_PROMOTABLE,
        PERSISTENT
    }

    public record RuntimeExecutionArtifact(
        String catalogFingerprint,
        String runId,
        String generatedAt,
        List<JsonObject> entries,
        Mode mode
    ) {
    }

    public record RuntimeEvidenceData(
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
}
