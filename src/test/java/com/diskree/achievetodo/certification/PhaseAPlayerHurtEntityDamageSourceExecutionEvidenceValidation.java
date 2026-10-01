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
import java.util.UUID;
import java.util.regex.Pattern;

/** Independent fail-closed validator for PLAYER_HURT_ENTITY_DAMAGE_SOURCE evidence. */
public final class PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation {
    public static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification", "player_hurt_entity_damage_source_execution_evidence.json"
    );
    public static final Path RUN_STATE_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification",
        "player_hurt_entity_damage_source_execution_evidence.run.json"
    );
    public static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "player_hurt_entity_damage_source_execution_evidence.json"
    );
    public static final Path CATALOG_PATH = PhaseAPlayerHurtEntityDamageSourceCertification.SNAPSHOT;
    public static final String SNAPSHOT = "phase_a_player_hurt_entity_damage_source_execution_evidence";
    public static final String FAMILY = PhaseAPlayerHurtEntityDamageSourceCertification.FAMILY;
    public static final String SOURCE = "PhaseAPlayerHurtEntityDamageSourceGameTest";
    public static final String MINECRAFT_VERSION = PhaseAPlayerHurtEntityDamageSourceCertification.MINECRAFT_VERSION;
    public static final String COMPATIBILITY_MARKER = PhaseAPlayerHurtEntityDamageSourceCertification.COMPATIBILITY_MARKER;
    public static final String TRIGGER = PhaseAPlayerHurtEntityDamageSourceCertification.TRIGGER;
    public static final String GREEN = "GREEN";
    public static final String BOUNDARY_WIND = PhaseAPlayerHurtEntityDamageSourceCertification.WIND_BOUNDARY;
    public static final String BOUNDARY_ATTACK = PhaseAPlayerHurtEntityDamageSourceCertification.ATTACK_BOUNDARY;
    public static final String PACKET_PATH_WIND = PhaseAPlayerHurtEntityDamageSourceCertification.WIND_PACKET_PATH;
    public static final String PACKET_PATH_ATTACK = PhaseAPlayerHurtEntityDamageSourceCertification.ATTACK_PACKET_PATH;
    private static final int SCHEMA_VERSION = 1;
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"
    );

    private PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Expected mode and optional project root path");
        }
        Path root = args.length == 2
            ? Path.of(args[1]).toAbsolutePath().normalize()
            : Path.of("").toAbsolutePath().normalize();
        RuntimeArtifact artifact = switch (args[0]) {
            case "temporary" -> loadValidatedTemporaryArtifact(root);
            case "promotable" -> loadValidatedPromotableTemporaryArtifact(root);
            case "persistent" -> loadValidatedPersistentArtifact(root);
            default -> throw new IllegalArgumentException("Unknown evidence validation mode: " + args[0]);
        };
        System.out.println("" + args[0].toUpperCase() + "_DIAGNOSTIC=PASS"
            + " family=" + FAMILY + " runId=" + artifact.runId()
            + " entries=" + artifact.entries().size());
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

    /** Persistent evidence is artifact-local and does not depend on a newer active run state. */
    public static RuntimeArtifact loadValidatedPersistentArtifact(Path projectRoot) throws IOException {
        return loadArtifact(projectRoot, PERSISTENT_ARTIFACT, Mode.PERSISTENT, false);
    }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        RuntimeArtifact artifact = loadValidatedPersistentArtifact(projectRoot);
        Map<String, RuntimeEvidenceData> result = new LinkedHashMap<>();
        for (JsonObject receipt : artifact.entries()) {
            String advancementId = requiredNonBlank(receipt, "advancementId");
            String criterion = requiredNonBlank(receipt, "criterion");
            String family = requiredNonBlank(receipt, "family");
            String source = requiredNonBlank(receipt, "source");
            RuntimeEvidenceData data = result.computeIfAbsent(advancementId, RuntimeEvidenceData::empty);
            data.greenCriteria().add(criterion);
            data.criteriaByFamily().computeIfAbsent(family, ignored -> new LinkedHashSet<>()).add(criterion);
            data.criteriaBySource().computeIfAbsent(source, ignored -> new LinkedHashSet<>()).add(criterion);
            data.families().add(family);
            data.sources().add(source);
        }
        return result;
    }

    /** The recorder uses the same independent receipt checks before accepting a GREEN record. */
    public static void validateReceipt(Path projectRoot, JsonObject receipt, String runId, String fingerprint)
        throws IOException {
        Catalog catalog = loadCatalog(projectRoot);
        String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
        CatalogCase definition = catalog.byKey().get(key);
        if (definition == null) {
            throw new IllegalStateException("Unknown " + FAMILY + " evidence key: " + key);
        }
        validateReceipt(receipt, definition, runId, fingerprint);
    }

    private static RuntimeArtifact loadArtifact(
        Path projectRoot,
        Path relativePath,
        Mode mode,
        boolean alignActiveRun
    ) throws IOException {
        String fingerprint = currentCatalogFingerprint(projectRoot);
        RuntimeRunState activeRun = alignActiveRun ? requireRunState(projectRoot, fingerprint) : null;
        RuntimeArtifact artifact = parseArtifact(readJson(projectRoot.resolve(relativePath)), mode);
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
        String runId = requiredNonBlank(state, "runId");
        requireUuid(runId, "run-state runId");
        return new RuntimeRunState(runId, requiredNonBlank(state, "catalogFingerprint"),
            requiredNonBlank(state, "startedAt"));
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
        requireUuid(runId, "runId");
        String generatedAt = requiredNonBlank(root, "generatedAt");
        JsonArray entriesJson = requiredArray(root, "entries");
        List<JsonObject> entries = new ArrayList<>();
        for (JsonElement element : entriesJson) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException(FAMILY + " evidence entry is not an object");
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
        if (artifact.entries().isEmpty()) {
            throw new IllegalStateException(FAMILY + " evidence is empty");
        }
        Set<String> actualKeys = new LinkedHashSet<>();
        Set<String> playerUuids = new LinkedHashSet<>();
        for (JsonObject receipt : artifact.entries()) {
            String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
            if (!actualKeys.add(key)) {
                throw new IllegalStateException("Duplicate " + FAMILY + " evidence key: " + key);
            }
            CatalogCase definition = catalog.byKey().get(key);
            if (definition == null) {
                throw new IllegalStateException("Unknown " + FAMILY + " evidence key: " + key);
            }
            validateReceipt(receipt, definition, artifact.runId(), fingerprint);
            playerUuids.add(requiredNonBlank(receipt, "playerUuid"));
        }
        if (exactCoverage && !actualKeys.equals(catalog.keys())) {
            throw new IllegalStateException(FAMILY + " evidence does not exactly cover the catalog"
                + " | expected=" + catalog.keys() + " | actual=" + actualKeys);
        }
        if (playerUuids.size() != actualKeys.size()) {
            throw new IllegalStateException(FAMILY + " evidence did not use fresh player identities per case");
        }
    }

    private static Catalog loadCatalog(Path projectRoot) throws IOException {
        Path path = projectRoot.resolve(CATALOG_PATH);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing " + FAMILY + " catalog: " + path);
        }
        String actual = Files.readString(path, StandardCharsets.UTF_8);
        String expected = PhaseAPlayerHurtEntityDamageSourceCertification.generateSnapshot(projectRoot);
        if (!actual.equals(expected)) {
            throw new IllegalStateException(FAMILY + " catalog is stale or not derived from frozen BACAP semantics");
        }
        JsonObject root = JsonParser.parseString(actual).getAsJsonObject();
        requireString(root, "snapshot", PhaseAPlayerHurtEntityDamageSourceCertification.SNAPSHOT_ID);
        requireInt(root, "schemaVersion", SCHEMA_VERSION);
        requireString(root, "family", FAMILY);
        requireInt(root, "canonicalAdvancementCount", 1152);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "semanticsSource", "frozenBacap");
        requireString(root, "sourceArtifactClassification", "FROZEN_BACAP_DEFINITION");
        JsonObject summary = requiredObject(root, "summary");
        requireInt(summary, "totalCases", 3);
        requireInt(summary, "uniqueKeys", 3);
        requireInt(summary, "uniqueAdvancements", 3);
        requireInt(summary, "requirementGroups", 3);
        requireInt(summary, "automationSupported", 3);
        requireInt(summary, "automationDeferred", 0);
        JsonArray casesJson = requiredArray(root, "cases");
        if (casesJson.size() != PhaseAPlayerHurtEntityDamageSourceCertification.EXPECTED_KEYS.size()) {
            throw new IllegalStateException("Expected three " + FAMILY + " catalog cases");
        }
        Map<String, CatalogCase> byKey = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>();
        for (JsonElement element : casesJson) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException(FAMILY + " catalog case is not an object");
            }
            CatalogCase definition = CatalogCase.fromJson(element.getAsJsonObject(), projectRoot);
            if (byKey.put(definition.key(), definition) != null) {
                throw new IllegalStateException("Duplicate " + FAMILY + " catalog key: " + definition.key());
            }
            keys.add(definition.key());
        }
        if (!keys.equals(PhaseAPlayerHurtEntityDamageSourceCertification.EXPECTED_KEYS)) {
            throw new IllegalStateException(FAMILY + " catalog key/order mismatch: " + keys);
        }
        return new Catalog(byKey, new LinkedHashSet<>(keys));
    }

    private static void validateReceipt(JsonObject receipt, CatalogCase definition, String runId, String fingerprint) {
        requireString(receipt, "family", FAMILY);
        requireString(receipt, "source", SOURCE);
        requireString(receipt, "catalogFingerprint", fingerprint);
        requireString(receipt, "runId", runId);
        requireUuid(runId, "runId");
        requireString(receipt, "minecraftVersion", MINECRAFT_VERSION);
        requireString(receipt, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(receipt, "result", GREEN);
        requireInt(receipt, "requirementGroupIndex", 0);
        requireString(receipt, "advancementId", definition.advancementId());
        requireString(receipt, "criterion", definition.criterion());
        requireString(receipt, "trigger", TRIGGER);
        requireString(receipt, "boundary", definition.boundary());
        requireString(receipt, "packetPath", definition.packetPath());

        requireString(receipt, "playerClass", "net.minecraft.server.level.ServerPlayer");
        String playerUuid = requiredNonBlank(receipt, "playerUuid");
        requireUuid(playerUuid, "playerUuid");
        String profileName = requiredNonBlank(receipt, "profileName");
        if (profileName.length() > 16) {
            throw new IllegalStateException("Profile name exceeds 16 characters");
        }
        requireString(receipt, "gameMode", "SURVIVAL");
        requireBoolean(receipt, "finiteMaterials", true);
        requireBoolean(receipt, "abilitiesInstabuild", false);
        requireBoolean(receipt, "abilitiesMayBuild", true);
        requireBoolean(receipt, "abilitiesInvulnerable", false);
        requireBoolean(receipt, "spectator", false);
        requireBoolean(receipt, "joined", true);
        requireBoolean(receipt, "connectionRegistered", true);
        requireBoolean(receipt, "clientLoaded", true);
        requireBoolean(receipt, "normalScheduler", true);

        requireString(receipt, "hand", "MAIN_HAND");
        JsonObject production = requiredObject(receipt, "productionPreconditions");
        requireString(production, "scoreboardObjective", "bac_advancements");
        requireString(production, "itemRewardsScoreboard", "bac_settings");
        requireInt(production, "itemRewardsScore", 0);
        requireBoolean(production, "itemRewardsDisabled", true);
        requireBoolean(production, "defaultAdvancementsMode", true);
        requireInt(production, "scoreBefore", 0);
        requireInt(production, "abilityUnlockThreshold", definition.abilityUnlockThreshold());
        requireString(production, "ability", definition.ability());
        requireBoolean(production, "lockedLandmark", false);
        if (definition.abilityUnlockThreshold() > 0) {
            requireInt(production, "scoreAfter", definition.abilityUnlockThreshold());
            requireBoolean(production, "abilityLockedBefore", true);
            requireBoolean(production, "abilityLockedAfter", false);
        } else {
            requireInt(production, "scoreAfter", 0);
            requireBoolean(production, "abilityLockedBefore", false);
            requireBoolean(production, "abilityLockedAfter", false);
        }

        requireBoolean(receipt, "criterionBefore", false);
        requireBoolean(receipt, "criterionAfter", true);
        requireBoolean(receipt, "legitimateTrigger", true);
        requireBoolean(receipt, "nativeTriggerObserved", true);
        requireIntAtLeast(receipt, "ticksToCriterion", 0);

        JsonObject weapon = requiredObject(receipt, "weapon");
        requireString(weapon, "itemBefore", definition.selectedItem());
        String expectedItemAfter = "minecraft:wind_charge".equals(definition.selectedItem())
            ? "minecraft:air" : definition.selectedItem();
        requireString(weapon, "itemAfter", expectedItemAfter);
        requireString(weapon, "hand", "MAIN_HAND");
        int slot = requiredInt(weapon, "slot");
        if (slot < 0 || slot > 8) {
            throw new IllegalStateException("Main-hand slot is outside the hotbar");
        }
        requireInt(weapon, "countBefore", 1);
        int expectedAfter = "minecraft:wind_charge".equals(definition.selectedItem()) ? 0 : 1;
        requireInt(weapon, "countAfter", expectedAfter);
        requireBoolean(weapon, "componentSnapshotRecorded", true);
        requireBoolean(weapon, "sameStackReference", true);

        JsonObject target = requiredObject(receipt, "target");
        requireString(target, "entityType", definition.targetEntityType());
        String targetUuid = requiredNonBlank(target, "entityUuid");
        requireUuid(targetUuid, "targetEntityUuid");
        requireIntAtLeast(target, "entityId", 1);
        requireBoolean(target, "aliveBefore", true);
        requireBoolean(target, "aliveAfter", true);
        requireString(target, "targetWitnessType", definition.targetEntityType());
        requireBoolean(target, "fixtureDamageBeforeAction", false);
        requireBoolean(target, "fixtureCriterionBeforeAction", false);
        double healthBefore = requiredDouble(target, "healthBefore");
        double healthAfter = requiredDouble(target, "healthAfter");
        double actualDamage = requiredDouble(target, "actualDamage");
        if (!(healthBefore > healthAfter) || !(actualDamage > 0.0)
            || Math.abs((healthBefore - healthAfter) - actualDamage) > 0.001) {
            throw new IllegalStateException("Target health mutation is not consistent with actual damage");
        }
        if (definition.targetBlockTag().isBlank()) {
            requireBoolean(target, "distanceWithinPredicate", true);
            if (requiredDouble(target, "distanceToPlayer") > definition.distanceMax() + 0.001) {
                throw new IllegalStateException("Target exceeded the catalog distance predicate");
            }
        } else {
            requireBoolean(target, "onGround", true);
            requireString(target, "steppingBlockTag", definition.targetBlockTag());
            requireBoolean(target, "steppingOnPredicateSatisfied", true);
        }

        JsonObject damage = requiredObject(receipt, "damageSource");
        requireString(damage, "type", definition.expectedDamageType());
        requireString(damage, "typeHolder", definition.expectedDamageType());
        requireBoolean(damage, "observedFromTargetLastDamageSource", true);
        JsonArray typeTags = requiredArray(damage, "typeTags");
        for (String expectedTag : definition.requiredDamageTypeTags()) {
            if (!containsString(typeTags, expectedTag)) {
                throw new IllegalStateException("Missing required damage type tag " + expectedTag);
            }
        }
        requireBoolean(damage, "isProjectile", !definition.requiredDamageTypeTags().isEmpty());
        JsonObject source = requiredObject(damage, "sourceEntity");
        requireString(source, "type", definition.expectedSourceEntityType());
        requireString(source, "uuid", playerUuid);
        requireBoolean(source, "isAttacker", true);
        JsonObject observedTarget = requiredObject(damage, "targetEntity");
        requireString(observedTarget, "type", definition.targetEntityType());
        requireString(observedTarget, "uuid", targetUuid);
        if (definition.expectedDirectEntityType().isBlank()) {
            requireBoolean(damage, "directEntityPresent", false);
            requireString(damage, "directEntityType", "minecraft:none");
        } else {
            requireBoolean(damage, "directEntityPresent", true);
            requireString(damage, "directEntityType", definition.expectedDirectEntityType());
            requireUuid(requiredNonBlank(damage, "directEntityUuid"), "directEntityUuid");
        }
        requireString(damage, "weaponItem", "minecraft:wind_charge".equals(definition.selectedItem())
            ? "minecraft:none" : definition.selectedItem());
        requireDoubleClose(damage, "actualDamage", actualDamage);

        JsonObject action = requiredObject(receipt, "actionResult");
        requireBoolean(action, "packetAccepted", true);
        requireBoolean(action, "legitimateGameplayAction", true);
        requireBoolean(action, "targetHealthChanged", true);
        requireString(action, "packetClass", "minecraft:wind_charge".equals(definition.selectedItem())
            ? "ServerboundUseItemPacket" : "ServerboundAttackPacket");
        requireString(action, "serverBoundary", definition.boundary());

        JsonObject proof = requiredObject(receipt, "packetProof");
        requireBoolean(proof, "realPacketPath", true);
        requireBoolean(proof, "noDirectCriterionTrigger", true);
        requireBoolean(proof, "noManualAward", true);
        requireBoolean(proof, "noFakeDamageSource", true);
        requireBoolean(proof, "noManualListenerInvocation", true);
        requireBoolean(proof, "liveAdvancementProgress", true);
        requireString(proof, "nativeCriterionPath", "CriteriaTriggers.PLAYER_HURT_ENTITY");

        JsonObject cleanup = requiredObject(receipt, "cleanup");
        requireBoolean(cleanup, "playerRemoved", true);
        requireBoolean(cleanup, "connectionRemoved", true);
        requireBoolean(cleanup, "channelSettled", true);
        requireIntAtLeast(cleanup, "settlementMessages", 0);
        requireInt(cleanup, "warningCount", 0);
    }

    private static boolean containsString(JsonArray values, String expected) {
        for (JsonElement value : values) {
            if (value.isJsonPrimitive() && expected.equals(value.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static JsonObject readJson(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing JSON artifact: " + path);
        }
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key);
        }
        return value.getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key);
        }
        return value.getAsJsonArray();
    }

    private static String requiredNonBlank(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key);
        }
        return value.getAsString();
    }

    private static void requireString(JsonObject parent, String key, String expected) {
        if (!expected.equals(requiredNonBlank(parent, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    private static int requiredInt(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalStateException("Missing number " + key);
        }
        return value.getAsInt();
    }

    private static void requireInt(JsonObject parent, String key, int expected) {
        if (requiredInt(parent, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    private static void requireIntAtLeast(JsonObject parent, String key, int minimum) {
        if (requiredInt(parent, key) < minimum) {
            throw new IllegalStateException("Expected " + key + ">=" + minimum);
        }
    }

    private static double requiredDouble(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalStateException("Missing number " + key);
        }
        return value.getAsDouble();
    }

    private static void requireDoubleClose(JsonObject parent, String key, double expected) {
        if (Math.abs(requiredDouble(parent, key) - expected) > 0.001) {
            throw new IllegalStateException("Unexpected " + key);
        }
    }

    private static void requireBoolean(JsonObject parent, String key, boolean expected) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonPrimitive() || value.getAsBoolean() != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    private static void requireUuid(String value, String context) {
        if (!UUID_PATTERN.matcher(value).matches()) {
            throw new IllegalStateException("Invalid UUID for " + context + ": " + value);
        }
    }

    private static void requireEquals(String actual, String expected, String field) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Unexpected " + field + ": expected=" + expected + ", actual=" + actual);
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

    private static CatalogCase catalogCase(PhaseAPlayerHurtEntityDamageSourceCertification.CaseDefinition definition) {
        return new CatalogCase(
            definition.advancementId(), definition.criterion(), definition.expectedDamageType(),
            definition.requiredDamageTypeTags(), definition.expectedDirectEntityType(),
            definition.expectedSourceEntityType(), definition.targetEntityType(), definition.targetBlockTag(),
            definition.sourceEquipmentTag(), definition.distanceMax(), definition.selectedItem(),
            definition.ability(), definition.abilityUnlockThreshold(), definition.boundary(), definition.packetPath()
        );
    }

    public record RuntimeArtifact(
        String catalogFingerprint,
        String runId,
        String generatedAt,
        List<JsonObject> entries,
        Mode mode
    ) {
    }

    public enum Mode {
        TEMP_DIAGNOSTIC,
        TEMP_PROMOTABLE,
        PERSISTENT
    }

    public record RuntimeEvidenceData(
        String advancementId,
        Set<String> greenCriteria,
        Map<String, Set<String>> criteriaByFamily,
        Map<String, Set<String>> criteriaBySource,
        Set<String> families,
        Set<String> sources
    ) {
        public static RuntimeEvidenceData empty(String advancementId) {
            return new RuntimeEvidenceData(advancementId, new LinkedHashSet<>(), new LinkedHashMap<>(),
                new LinkedHashMap<>(), new LinkedHashSet<>(), new LinkedHashSet<>());
        }
    }

    private record RuntimeRunState(String runId, String catalogFingerprint, String startedAt) {
    }

    private record Catalog(Map<String, CatalogCase> byKey, Set<String> keys) {
    }

    private record CatalogCase(
        String advancementId,
        String criterion,
        String expectedDamageType,
        List<String> requiredDamageTypeTags,
        String expectedDirectEntityType,
        String expectedSourceEntityType,
        String targetEntityType,
        String targetBlockTag,
        String sourceEquipmentTag,
        int distanceMax,
        String selectedItem,
        String ability,
        int abilityUnlockThreshold,
        String boundary,
        String packetPath
    ) {
        static CatalogCase fromJson(JsonObject json, Path projectRoot) {
            String advancementId = requiredNonBlank(json, "advancementId");
            String criterion = requiredNonBlank(json, "criterion");
            PhaseAPlayerHurtEntityDamageSourceCertification.CaseDefinition derived = null;
            try {
                for (PhaseAPlayerHurtEntityDamageSourceCertification.CaseDefinition candidate :
                    PhaseAPlayerHurtEntityDamageSourceCertification.deriveCases(projectRoot)) {
                    if (candidate.advancementId().equals(advancementId)
                        && candidate.criterion().equals(criterion)) {
                        derived = candidate;
                        break;
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException("Could not derive catalog case", e);
            }
            if (derived == null) {
                throw new IllegalStateException("Unknown catalog case " + advancementId + "#" + criterion);
            }
            requireInt(json, "requirementGroupIndex", 0);
            requireInt(json, "requirementGroupCount", 1);
            requireString(json, "trigger", TRIGGER);
            requireString(json, "expectedDamageType", derived.expectedDamageType());
            requireString(json, "targetEntityType", derived.targetEntityType());
            requireString(json, "selectedItem", derived.selectedItem());
            requireString(json, "boundary", derived.boundary());
            requireString(json, "packetPath", derived.packetPath());
            return catalogCase(derived);
        }

        String key() {
            return advancementId + "#" + criterion;
        }
    }
}
