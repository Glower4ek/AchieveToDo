package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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

/** Independent fail-closed validator for interaction TEMP and persistent evidence. */
public final class PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation {
    public static final Path TEMPORARY_ARTIFACT = PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidationPath.TEMPORARY;
    public static final Path RUN_STATE_ARTIFACT = PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidationPath.RUN_STATE;
    public static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "item_tag_player_interacted_with_entity_execution_evidence.json"
    );
    public static final Path CATALOG_PATH = PhaseAItemTagPlayerInteractedWithEntityCertification.SNAPSHOT;
    public static final String SNAPSHOT = "phase_a_item_tag_player_interacted_with_entity_execution_evidence";
    public static final String FAMILY = "ITEM_TAG_PLAYER_INTERACTED_WITH_ENTITY";
    public static final String SOURCE = "PhaseAItemTagPlayerInteractedWithEntityGameTest";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String TRIGGER = "minecraft:player_interacted_with_entity";
    public static final String GREEN = "GREEN";
    public static final String BOUNDARY = "ServerGamePacketListenerImpl.handleInteract";
    public static final String PACKET_PATH = "ServerboundInteractPacket.handle->ServerGamePacketListenerImpl.handleInteract->CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY";
    private static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation() {
    }

    public static String currentCatalogFingerprint(Path projectRoot) throws IOException {
        try {
            return "sha-256:" + hex(MessageDigest.getInstance("SHA-256").digest(
                Files.readAllBytes(projectRoot.resolve(CATALOG_PATH))
            ));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    public static RuntimeArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        return loadArtifact(projectRoot, TEMPORARY_ARTIFACT, Mode.TEMP_DIAGNOSTIC, true);
    }

    public static RuntimeArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot) throws IOException {
        return loadArtifact(projectRoot, TEMPORARY_ARTIFACT, Mode.TEMP_PROMOTABLE, true);
    }

    /** Persistent evidence is artifact-local: it must not depend on a newer active RUN_STATE. */
    public static RuntimeArtifact loadValidatedPersistentArtifact(Path projectRoot) throws IOException {
        return loadArtifact(projectRoot, PERSISTENT_ARTIFACT, Mode.PERSISTENT, false);
    }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        RuntimeArtifact artifact = loadValidatedPersistentArtifact(projectRoot);
        Map<String, RuntimeEvidenceData> result = new LinkedHashMap<>();
        for (JsonObject receipt : artifact.entries()) {
            String advancementId = requiredNonBlank(receipt, "advancementId");
            RuntimeEvidenceData data = result.computeIfAbsent(advancementId, RuntimeEvidenceData::empty);
            String criterion = requiredNonBlank(receipt, "criterion");
            String family = requiredNonBlank(receipt, "family");
            String source = requiredNonBlank(receipt, "source");
            data.greenCriteria().add(criterion);
            data.criteriaByFamily().computeIfAbsent(family, ignored -> new TreeSet<>()).add(criterion);
            data.criteriaBySource().computeIfAbsent(source, ignored -> new TreeSet<>()).add(criterion);
            data.families().add(family);
            data.sources().add(source);
        }
        return result;
    }

    private static RuntimeArtifact loadArtifact(
        Path projectRoot,
        Path relativePath,
        Mode mode,
        boolean alignActiveRun
    ) throws IOException {
        String fingerprint = currentCatalogFingerprint(projectRoot);
        RuntimeRunState activeRun = alignActiveRun ? requireRunState(projectRoot, fingerprint) : null;
        JsonObject root = readJson(projectRoot.resolve(relativePath));
        RuntimeArtifact artifact = parseArtifact(root, mode);
        requireEquals(artifact.catalogFingerprint(), fingerprint, "catalogFingerprint");
        if (activeRun != null) {
            requireEquals(artifact.runId(), activeRun.runId(), "runId");
            requireEquals(activeRun.catalogFingerprint(), fingerprint, "run-state catalogFingerprint");
        }
        validateArtifact(projectRoot, artifact, fingerprint, mode != Mode.TEMP_DIAGNOSTIC);
        return artifact;
    }

    private static RuntimeRunState requireRunState(Path projectRoot, String fingerprint) throws IOException {
        JsonObject state = readJson(projectRoot.resolve(RUN_STATE_ARTIFACT));
        requireString(state, "snapshot", SNAPSHOT);
        requireInt(state, "schemaVersion", SCHEMA_VERSION);
        requireString(state, "family", FAMILY);
        requireString(state, "source", SOURCE);
        requireString(state, "minecraftVersion", MINECRAFT_VERSION);
        requireString(state, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(state, "catalogFingerprint", fingerprint);
        return new RuntimeRunState(
            requiredNonBlank(state, "runId"),
            requiredNonBlank(state, "catalogFingerprint"),
            requiredNonBlank(state, "startedAt")
        );
    }

    private static RuntimeArtifact parseArtifact(JsonObject root, Mode mode) {
        requireString(root, "snapshot", SNAPSHOT);
        requireInt(root, "schemaVersion", SCHEMA_VERSION);
        requireString(root, "family", FAMILY);
        requireString(root, "source", SOURCE);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        String fingerprint = requiredNonBlank(root, "catalogFingerprint");
        String runId = requiredNonBlank(root, "runId");
        String generatedAt = requiredNonBlank(root, "generatedAt");
        JsonArray entriesJson = requiredArray(root, "entries");
        List<JsonObject> entries = new ArrayList<>();
        for (JsonElement element : entriesJson) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("Interaction evidence entry is not an object");
            }
            entries.add(element.getAsJsonObject());
        }
        return new RuntimeArtifact(fingerprint, runId, generatedAt, List.copyOf(entries), mode);
    }

    private static void validateArtifact(
        Path projectRoot,
        RuntimeArtifact artifact,
        String fingerprint,
        boolean exactCoverage
    ) throws IOException {
        Catalog catalog = loadCatalog(projectRoot);
        requireEquals(artifact.catalogFingerprint(), fingerprint, "catalogFingerprint");
        requireNonBlank(artifact.runId(), "runId");
        requireNonBlank(artifact.generatedAt(), "generatedAt");
        Set<String> actualKeys = new LinkedHashSet<>();
        for (JsonObject receipt : artifact.entries()) {
            String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
            if (!actualKeys.add(key)) {
                throw new IllegalStateException("Duplicate interaction evidence key: " + key);
            }
            CatalogCase definition = catalog.byKey().get(key);
            if (definition == null) {
                throw new IllegalStateException("Unknown interaction evidence key: " + key);
            }
            validateReceipt(receipt, definition, artifact.runId(), fingerprint);
        }
        if (exactCoverage && !actualKeys.equals(catalog.keys())) {
            throw new IllegalStateException("Interaction evidence does not exactly cover the catalog"
                + " | expected=" + catalog.keys() + " | actual=" + actualKeys);
        }
    }

    private static Catalog loadCatalog(Path projectRoot) throws IOException {
        Path path = projectRoot.resolve(CATALOG_PATH);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing interaction catalog: " + path);
        }
        String actual = Files.readString(path, StandardCharsets.UTF_8);
        String expected = PhaseAItemTagPlayerInteractedWithEntityCertification.generateSnapshot(projectRoot);
        if (!actual.equals(expected)) {
            throw new IllegalStateException("Interaction catalog is stale or not derived from frozen BACAP semantics");
        }
        JsonObject root = JsonParser.parseString(actual).getAsJsonObject();
        requireString(root, "snapshot", PhaseAItemTagPlayerInteractedWithEntityCertification.SNAPSHOT_ID);
        requireInt(root, "schemaVersion", 1);
        requireString(root, "family", FAMILY);
        requireInt(root, "canonicalAdvancementCount", 1152);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "semanticsSource", "frozenBacap");
        requireString(root, "runtimeBoundary", BOUNDARY);
        requireString(root, "runtimePacketPath", PACKET_PATH);
        JsonObject summary = requiredObject(root, "summary");
        for (String key : List.of("totalCases", "uniqueKeys", "uniqueAdvancements", "requirementGroups", "automationSupported")) {
            requireInt(summary, key, 4);
        }
        requireInt(summary, "automationDeferred", 0);
        JsonArray casesJson = requiredArray(root, "cases");
        if (casesJson.size() != 4) {
            throw new IllegalStateException("Expected four interaction catalog cases");
        }
        Map<String, CatalogCase> cases = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>();
        for (JsonElement element : casesJson) {
            CatalogCase definition = CatalogCase.fromJson(element.getAsJsonObject());
            if (cases.put(definition.key(), definition) != null) {
                throw new IllegalStateException("Duplicate interaction catalog case " + definition.key());
            }
            keys.add(definition.key());
        }
        if (!keys.equals(PhaseAItemTagPlayerInteractedWithEntityCertification.EXPECTED_KEYS)) {
            throw new IllegalStateException("Interaction catalog key order mismatch: " + keys);
        }
        return new Catalog(cases, new LinkedHashSet<>(keys));
    }

    private static void validateReceipt(JsonObject receipt, CatalogCase definition, String runId, String fingerprint) {
        requireString(receipt, "family", FAMILY);
        requireString(receipt, "source", SOURCE);
        requireString(receipt, "catalogFingerprint", fingerprint);
        requireString(receipt, "runId", runId);
        requireString(receipt, "minecraftVersion", MINECRAFT_VERSION);
        requireString(receipt, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(receipt, "result", GREEN);
        requireInt(receipt, "requirementGroupIndex", 0);
        requireString(receipt, "trigger", TRIGGER);
        requireString(receipt, "itemTag", definition.itemTag());
        requireString(receipt, "selectedItem", definition.selectedItem());
        requireBoolean(receipt, "runtimeItemTagMembership", true);
        int memberCount = requiredInt(receipt, "runtimeItemTagMemberCount");
        if (memberCount <= 0) {
            throw new IllegalStateException("Runtime item-tag member count must be positive");
        }
        JsonArray members = requiredArray(receipt, "runtimeItemTagMembers");
        if (members.size() != memberCount) {
            throw new IllegalStateException("Runtime item-tag member witness count mismatch");
        }
        boolean selectedMember = false;
        for (JsonElement member : members) {
            if (definition.selectedItem().equals(requiredStringValue(member, "runtimeItemTagMembers"))) {
                selectedMember = true;
            }
        }
        if (!selectedMember) {
            throw new IllegalStateException("Selected concrete item is absent from runtime HolderSet witness");
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
        requiredNonBlank(receipt, "profileName");
        requireString(receipt, "gameMode", "SURVIVAL");
        requireBoolean(receipt, "finiteMaterials", true);
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
            throw new IllegalStateException("Invalid interaction criterion tick count");
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
        String itemAfter = requiredNonBlank(stack, "itemAfter");
        if ("IGNITE_CREEPER".equals(definition.action())) {
            requireString(stack, "itemAfter", definition.selectedItem());
            requireInt(stack, "countAfter", 1);
        } else {
            if (!"minecraft:air".equals(itemAfter)) {
                throw new IllegalStateException("Food interaction did not empty the used hand");
            }
            requireInt(stack, "countAfter", 0);
        }
        String selectedHandAfter = requiredNonBlank(stack, "selectedHandAfter");
        int selectedHandCountAfter = requiredInt(stack, "selectedHandCountAfter");
        if (selectedHandCountAfter < 0) {
            throw new IllegalStateException("Negative selected-hand count after interaction");
        }
        requireInt(stack, "countBefore", 1);
        int damageBefore = requiredInt(stack, "damageBefore");
        int damageAfter = requiredInt(stack, "damageAfter");
        if (damageBefore < 0) {
            throw new IllegalStateException("Negative item damage before interaction");
        }
        if ("IGNITE_CREEPER".equals(definition.action()) ? damageAfter != damageBefore + 1 : damageAfter != damageBefore) {
            throw new IllegalStateException("Unexpected item durability mutation for " + definition.key());
        }
        requiredNonBlank(stack, "snapshotBefore");
        requiredNonBlank(stack, "snapshotAfter");
        requireBooleanField(stack, "sameStackReference");
        if (stack.get("sameStackReference").getAsBoolean()) {
            requireString(stack, "selectedHandAfter", itemAfter);
            requireInt(stack, "selectedHandCountAfter", requiredInt(stack, "countAfter"));
        }

        JsonObject entityBefore = requiredObject(receipt, "entityStateBefore");
        JsonObject entityAfter = requiredObject(receipt, "entityStateAfter");
        requireString(entityBefore, "type", definition.selectedEntityType());
        requireString(entityAfter, "type", definition.selectedEntityType());
        requireBoolean(entityBefore, "alive", true);
        requireBoolean(entityAfter, "alive", true);
        requirePositive(entityBefore, "entityId");
        requirePositive(entityAfter, "entityId");
        String stateUuid = requiredNonBlank(entityAfter, "uuid");
        requireUuid(entityBefore, "uuid");
        requireUuid(entityAfter, "uuid");
        requireString(entityBefore, "uuid", stateUuid);
        requireString(receipt, "targetEntityUuid", stateUuid);
        if ("FEED_DOLPHIN".equals(definition.action())) {
            requireBoolean(entityAfter, "gotFish", true);
        } else if ("IGNITE_CREEPER".equals(definition.action())) {
            requireBoolean(entityBefore, "ignited", false);
            requireBoolean(entityAfter, "ignited", true);
        } else if ("FEED_BABY_SNIFFER".equals(definition.action())) {
            requireBoolean(entityBefore, "isBaby", true);
            requireBoolean(entityAfter, "isBaby", true);
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
        if (requiredInt(cleanup, "settlementMessages") < 0) {
            throw new IllegalStateException("Negative channel settlement count");
        }
        requireInt(cleanup, "warningCount", 0);
    }

    private static JsonObject readJson(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing interaction evidence artifact: " + path);
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

    private static int requiredInt(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing " + key + " in interaction evidence");
        }
        return json.get(key).getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in interaction evidence");
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
            throw new IllegalStateException("Invalid UUID in " + key, e);
        }
    }

    private static void requireNonBlank(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Expected nonblank " + key + " in interaction evidence");
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

    public record RuntimeArtifact(
        String catalogFingerprint,
        String runId,
        String generatedAt,
        List<JsonObject> entries,
        Mode mode
    ) {
    }

    public record RuntimeRunState(String runId, String catalogFingerprint, String startedAt) {
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

    private record Catalog(Map<String, CatalogCase> byKey, Set<String> keys) {
    }

    private record CatalogCase(
        String advancementId,
        String criterion,
        String itemTag,
        String selectedItem,
        String selectedEntityType,
        String action,
        String productionPrecondition
    ) {
        static CatalogCase fromJson(JsonObject json) {
            String advancementId = requiredNonBlank(json, "advancementId");
            String criterion = requiredNonBlank(json, "criterion");
            JsonObject predicate = requiredObject(json, "entityPredicate");
            return new CatalogCase(
                advancementId,
                criterion,
                requiredNonBlank(json, "itemTag"),
                requiredNonBlank(json, "selectedItem"),
                requiredNonBlank(predicate, "entityType"),
                requiredNonBlank(json, "action"),
                requiredNonBlank(json, "productionPrecondition")
            );
        }

        String key() {
            return advancementId + "#" + criterion;
        }
    }

    /** Keeps test and GameTest artifact locations explicit without importing the gametest source set. */
    private static final class PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidationPath {
        private static final Path TEMPORARY = Path.of("build", "tmp", "phase_a_certification", "item_tag_player_interacted_with_entity_execution_evidence.json");
        private static final Path RUN_STATE = Path.of("build", "tmp", "phase_a_certification", "item_tag_player_interacted_with_entity_execution_evidence.run.json");
    }
}
