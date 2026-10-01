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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** TEMP recorder, fail-closed validator, and persistent promotion adapter for SHOT_CROSSBOW. */
public final class PhaseAShotCrossbowExecutionEvidence {
    public static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification", "shot_crossbow_execution_evidence.json"
    );
    public static final Path RUN_STATE_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification", "shot_crossbow_execution_evidence.run.json"
    );
    public static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification", "shot_crossbow_execution_evidence.json"
    );
    public static final Path CATALOG_PATH = CertifiedShotCrossbowCatalog.CATALOG_PATH;
    public static final String SNAPSHOT = "phase_a_shot_crossbow_execution_evidence";
    public static final String FAMILY = "SHOT_CROSSBOW";
    public static final String SOURCE = "PhaseAShotCrossbowGameTest";
    public static final String GREEN = "GREEN";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String TRIGGER = "minecraft:shot_crossbow";
    public static final String BOUNDARY = "ServerGamePacketListenerImpl.handleUseItem";
    public static final String PACKET_PATH =
        "ServerboundUseItemPacket.handle->ServerGamePacketListenerImpl.handleUseItem"
            + "->CrossbowItem.performShooting->CriteriaTriggers.SHOT_CROSSBOW";
    public static final String SOURCE_ARTIFACT_CLASSIFICATION = "FROZEN_BACAP_DEFINITION";
    private static final int SCHEMA_VERSION = 1;
    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAShotCrossbowExecutionEvidence() {
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
            throw new IllegalStateException("Missing system property " + PROJECT_ROOT_PROPERTY);
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
        validateReceipt(root, receipt, runId, fingerprint);

        String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
        JsonArray entries = requiredArray(artifact, "entries");
        List<JsonObject> ordered = new ArrayList<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("SHOT_CROSSBOW evidence entry is not an object");
            }
            JsonObject existing = element.getAsJsonObject();
            String existingKey = requiredNonBlank(existing, "advancementId") + "#" + requiredNonBlank(existing, "criterion");
            if (key.equals(existingKey)) {
                throw new IllegalStateException("Duplicate SHOT_CROSSBOW runtime evidence receipt: " + key);
            }
            ordered.add(existing);
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
            throw new IllegalStateException("Refusing to overwrite existing persistent SHOT_CROSSBOW evidence");
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
        String fingerprint = currentCatalogFingerprint(root);
        String activeRunId = alignActiveRun ? currentRunId(root) : null;
        JsonObject artifactJson = readJson(root.resolve(relativePath));
        RuntimeExecutionArtifact artifact = parseArtifact(artifactJson, mode);
        requireEquals(artifact.catalogFingerprint(), fingerprint, "catalogFingerprint");
        if (activeRunId != null) {
            requireEquals(artifact.runId(), activeRunId, "runId");
        }
        validateArtifact(root, artifact, fingerprint, mode != Mode.TEMP_DIAGNOSTIC);
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
                throw new IllegalStateException("SHOT_CROSSBOW evidence entry is not an object");
            }
            entries.add(element.getAsJsonObject());
        }
        return new RuntimeExecutionArtifact(fingerprint, runId, generatedAt, List.copyOf(entries), mode);
    }

    private static void validateArtifact(
        Path root,
        RuntimeExecutionArtifact artifact,
        String fingerprint,
        boolean exactCoverage
    ) throws IOException {
        Map<String, CertifiedShotCrossbowCatalog.CaseDefinition> catalog = new LinkedHashMap<>();
        for (CertifiedShotCrossbowCatalog.CaseDefinition definition : CertifiedShotCrossbowCatalog.allCases()) {
            if (catalog.put(definition.key(), definition) != null) {
                throw new IllegalStateException("Duplicate SHOT_CROSSBOW catalog key: " + definition.key());
            }
        }
        Set<String> actualKeys = new LinkedHashSet<>();
        for (JsonObject receipt : artifact.entries()) {
            String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
            if (!actualKeys.add(key)) {
                throw new IllegalStateException("Duplicate SHOT_CROSSBOW evidence key: " + key);
            }
            CertifiedShotCrossbowCatalog.CaseDefinition definition = catalog.get(key);
            if (definition == null) {
                throw new IllegalStateException("Unknown SHOT_CROSSBOW evidence key: " + key);
            }
            validateReceipt(root, receipt, artifact.runId(), fingerprint);
        }
        if (exactCoverage && !actualKeys.equals(catalog.keySet())) {
            throw new IllegalStateException("SHOT_CROSSBOW evidence does not exactly cover the catalog"
                + " | expected=" + catalog.keySet() + " | actual=" + actualKeys);
        }
    }

    private static void validateReceipt(Path root, JsonObject receipt, String runId, String fingerprint) throws IOException {
        String advancementId = requiredNonBlank(receipt, "advancementId");
        String criterion = requiredNonBlank(receipt, "criterion");
        CertifiedShotCrossbowCatalog.CaseDefinition definition = CertifiedShotCrossbowCatalog.requiredCase(advancementId, criterion);
        requireString(receipt, "family", FAMILY);
        requireString(receipt, "source", SOURCE);
        requireString(receipt, "catalogFingerprint", fingerprint);
        requireString(receipt, "runId", runId);
        requireString(receipt, "minecraftVersion", MINECRAFT_VERSION);
        requireString(receipt, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(receipt, "result", GREEN);
        requireInt(receipt, "requirementGroupIndex", definition.requirementGroupIndex());
        requireString(receipt, "trigger", TRIGGER);
        requireString(receipt, "selectedItem", definition.selectedItem());
        requireString(receipt, "enchantment", definition.enchantment());
        requireString(receipt, "levelRule", definition.levelRule());
        requireInt(receipt, "configuredEnchantmentLevel", definition.enchantmentLevel(),
            "configured enchantment level");
        int actualEnchantmentLevel = requiredInt(receipt, "actualEnchantmentLevel");
        if ("min".equals(definition.levelRule())) {
            if (actualEnchantmentLevel < definition.enchantmentLevel()) {
                throw new IllegalStateException("Configured enchantment is below the frozen minimum for " + definition.key());
            }
        } else if (actualEnchantmentLevel < 1) {
            throw new IllegalStateException("Required enchantment is absent for " + definition.key());
        }
        requireBoolean(receipt, "itemPredicateSatisfied", true);

        requireString(receipt, "boundary", BOUNDARY);
        requireString(receipt, "packetPath", PACKET_PATH);
        requireString(receipt, "hand", "MAIN_HAND");
        requireString(receipt, "playerClass", "net.minecraft.server.level.ServerPlayer");
        requireUuid(receipt, "playerUuid");
        requireNonBlank(receipt, "profileName");
        requireString(receipt, "gameMode", "SURVIVAL");
        requireBoolean(receipt, "finiteMaterials", true);
        requireBoolean(receipt, "abilitiesInstabuild", false);
        requireBoolean(receipt, "spectator", false);
        requireBoolean(receipt, "joined", true);
        requireBoolean(receipt, "connectionRegistered", true);
        requireBoolean(receipt, "clientLoaded", true);
        requireBoolean(receipt, "normalScheduler", true);

        requireBoolean(receipt, "usePacketAccepted", true);
        requireBoolean(receipt, "releasePacketAccepted", true);
        requireString(receipt, "releasePacketAction", "RELEASE_USE_ITEM");
        requireBoolean(receipt, "usingAfterRelease", false);
        requireBoolean(receipt, "chargedBeforeRelease", true);
        requireBoolean(receipt, "chargedBeforeShoot", true);
        requireBoolean(receipt, "chargedAfterShoot", false);
        requireBoolean(receipt, "criterionBefore", false);
        requireBoolean(receipt, "criterionAfter", true);
        requireBoolean(receipt, "legitimateTrigger", true);
        requireBoolean(receipt, "noDirectCriterionTrigger", true);
        requireBoolean(receipt, "noManualAward", true);
        requireBoolean(receipt, "nativeTriggerObserved", true);
        int ticks = requiredInt(receipt, "ticksToCriterion");
        if (ticks < 0 || ticks > 10) {
            throw new IllegalStateException("Invalid SHOT_CROSSBOW criterion tick count for " + definition.key());
        }

        JsonObject preconditions = requiredObject(receipt, "productionPreconditions");
        requireString(preconditions, "scoreboardObjective", "bac_advancements");
        requireString(preconditions, "itemRewardsScoreboard", "bac_settings");
        requireInt(preconditions, "itemRewardsScore", 0);
        requireBoolean(preconditions, "itemRewardsDisabled", true);
        requireString(preconditions, "ability", "SHOOT_CROSSBOW");
        requireInt(preconditions, "abilityUnlockThreshold", 269);
        requireBoolean(preconditions, "defaultAdvancementsMode", true);
        requireInt(preconditions, "scoreBefore", 0);
        requireInt(preconditions, "scoreAfter", 269);
        requireBoolean(preconditions, "abilityLockedBefore", true);
        requireBoolean(preconditions, "abilityLockedAfter", false);
        requireBoolean(preconditions, "lockedLandmark", false);

        JsonObject loaded = requiredObject(receipt, "loadedProjectiles");
        int loadedCount = requiredInt(loaded, "count");
        requireInt(loaded, "expectedCount", definition.expectedLoadedProjectiles());
        requireInt(loaded, "count", definition.expectedLoadedProjectiles());
        requireString(loaded, "itemType", "minecraft:arrow");
        requireBoolean(loaded, "chargedComponentNonEmpty", true);
        requireBoolean(receipt, "loadedProjectileWitness", true);

        JsonObject ammo = requiredObject(receipt, "ammoState");
        requireString(ammo, "item", "minecraft:arrow");
        requireInt(ammo, "countBefore", 1);
        requireInt(ammo, "countAfter", 0);
        requireInt(ammo, "consumed", definition.expectedAmmoConsumed());
        requireInt(ammo, "additionalCopiesNotConsumed", Math.max(0, definition.expectedLoadedProjectiles() - 1));
        requireBoolean(ammo, "finiteSurvivalConsumption", true);

        JsonObject crossbow = requiredObject(receipt, "crossbowState");
        requireString(crossbow, "itemBefore", "minecraft:crossbow");
        requireString(crossbow, "itemAfter", "minecraft:crossbow");
        requireInt(crossbow, "damageBefore", 0);
        requireInt(crossbow, "damageAfter", definition.expectedDurabilityUse());
        requireInt(crossbow, "durabilityUse", definition.expectedDurabilityUse());
        requireBoolean(crossbow, "chargedBeforeRelease", true);
        requireBoolean(crossbow, "chargedAfterShoot", false);
        requireBoolean(crossbow, "sameStackReference", true);

        JsonObject projectiles = requiredObject(receipt, "projectileObservation");
        requireInt(projectiles, "expectedCount", definition.expectedFiredProjectiles());
        requireInt(projectiles, "spawnedCount", definition.expectedFiredProjectiles());
        requireInt(projectiles, "firedProjectileCount", definition.expectedFiredProjectiles());
        requireBoolean(projectiles, "allAreArrowProjectiles", true);
        requireBoolean(projectiles, "allHavePlayerOwner", true);
        requireBoolean(projectiles, "ownerIdentityMatches", true);
        requireBoolean(projectiles, "nativeSpawnObserved", true);
        JsonArray ids = requiredArray(projectiles, "entityIds");
        JsonArray uuids = requiredArray(projectiles, "entityUuids");
        JsonArray types = requiredArray(projectiles, "entityTypes");
        JsonArray owners = requiredArray(projectiles, "owners");
        if (ids.size() != definition.expectedFiredProjectiles()
            || uuids.size() != definition.expectedFiredProjectiles()
            || types.size() != definition.expectedFiredProjectiles()
            || owners.size() != definition.expectedFiredProjectiles()) {
            throw new IllegalStateException("Projectile witness cardinality mismatch for " + definition.key());
        }
        Set<String> seenIds = new LinkedHashSet<>();
        Set<String> seenUuids = new LinkedHashSet<>();
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i).getAsInt();
            if (id <= 0 || !seenIds.add(Integer.toString(id))) {
                throw new IllegalStateException("Invalid or duplicate projectile entity id for " + definition.key());
            }
            String uuid = requiredStringValue(uuids.get(i), "projectile entity UUID");
            requireUuidValue(uuid, "projectile entity UUID");
            if (!seenUuids.add(uuid)) {
                throw new IllegalStateException("Duplicate projectile UUID for " + definition.key());
            }
            if (!"minecraft:arrow".equals(requiredStringValue(types.get(i), "projectile entity type"))) {
                throw new IllegalStateException("Non-arrow projectile witness for " + definition.key());
            }
            if (!requiredStringValue(owners.get(i), "projectile owner").equals(requiredNonBlank(receipt, "playerUuid"))) {
                throw new IllegalStateException("Projectile owner witness does not match player for " + definition.key());
            }
        }

        JsonObject proof = requiredObject(receipt, "packetProof");
        requireString(proof, "usePacket", "ServerboundUseItemPacket");
        requireString(proof, "releasePacket", "ServerboundPlayerActionPacket(RELEASE_USE_ITEM)");
        requireString(proof, "serverHandler", "ServerGamePacketListenerImpl.handleUseItem");
        requireString(proof, "nativeItemPath", "CrossbowItem.use->CrossbowItem.performShooting");
        requireString(proof, "nativeCriterionPath", "CriteriaTriggers.SHOT_CROSSBOW");
        requireBoolean(proof, "realPacketPath", true);
        requireBoolean(proof, "noDirectCriterionTrigger", true);
        requireBoolean(proof, "noManualAward", true);
        requireBoolean(proof, "liveAdvancementProgress", true);

        JsonObject cleanup = requiredObject(receipt, "cleanup");
        requireBoolean(cleanup, "playerRemoved", true);
        requireBoolean(cleanup, "connectionRemoved", true);
        requireBoolean(cleanup, "channelSettled", true);
        requireInt(cleanup, "settlementMessages", 0, "cleanup settlement messages", false);
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
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing SHOT_CROSSBOW evidence file: " + path);
        }
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in SHOT_CROSSBOW evidence");
        }
        return value.getAsJsonArray();
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in SHOT_CROSSBOW evidence");
        }
        return value.getAsJsonObject();
    }

    private static String requiredNonBlank(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in SHOT_CROSSBOW evidence");
        }
        return value.getAsString();
    }

    private static String requiredStringValue(JsonElement value, String key) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in SHOT_CROSSBOW evidence");
        }
        return value.getAsString();
    }

    private static int requiredInt(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalStateException("Missing " + key + " in SHOT_CROSSBOW evidence");
        }
        return value.getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        requireInt(json, key, expected, key, true);
    }

    private static void requireInt(JsonObject json, String key, int expected, String context) {
        requireInt(json, key, expected, context, true);
    }

    private static void requireInt(JsonObject json, String key, int expected, String context, boolean exact) {
        int actual = requiredInt(json, key);
        if (exact ? actual != expected : actual < expected) {
            throw new IllegalStateException("Unexpected " + context + " in SHOT_CROSSBOW evidence: " + actual);
        }
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredNonBlank(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in SHOT_CROSSBOW evidence");
        }
    }

    private static void requireBoolean(JsonObject json, String key, boolean expected) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()
            || value.getAsBoolean() != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in SHOT_CROSSBOW evidence");
        }
    }

    private static void requireEquals(String actual, String expected, String field) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + field + "=" + expected + " but got " + actual);
        }
    }

    private static void requireNonBlank(JsonObject json, String key) {
        requiredNonBlank(json, key);
    }

    private static void requireUuid(JsonObject json, String key) {
        requireUuidValue(requiredNonBlank(json, key), key);
    }

    private static void requireUuidValue(String value, String key) {
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid UUID in " + key + " in SHOT_CROSSBOW evidence", e);
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
