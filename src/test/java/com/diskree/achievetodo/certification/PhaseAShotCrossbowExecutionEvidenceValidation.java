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
import java.util.UUID;

/** Independent fail-closed validator for SHOT_CROSSBOW TEMP and persistent evidence. */
public final class PhaseAShotCrossbowExecutionEvidenceValidation {
    public static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification", "shot_crossbow_execution_evidence.json"
    );
    public static final Path RUN_STATE_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification", "shot_crossbow_execution_evidence.run.json"
    );
    public static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification", "shot_crossbow_execution_evidence.json"
    );
    public static final Path CATALOG_PATH = PhaseAShotCrossbowCertification.SNAPSHOT;
    public static final String SNAPSHOT = "phase_a_shot_crossbow_execution_evidence";
    public static final String FAMILY = "SHOT_CROSSBOW";
    public static final String SOURCE = "PhaseAShotCrossbowGameTest";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String TRIGGER = "minecraft:shot_crossbow";
    public static final String GREEN = "GREEN";
    public static final String BOUNDARY = "ServerGamePacketListenerImpl.handleUseItem";
    public static final String PACKET_PATH =
        "ServerboundUseItemPacket.handle->ServerGamePacketListenerImpl.handleUseItem"
            + "->CrossbowItem.performShooting->CriteriaTriggers.SHOT_CROSSBOW";
    private static final int SCHEMA_VERSION = 1;
    private static final List<String> EXPECTED_KEYS = PhaseAShotCrossbowCertification.EXPECTED_KEYS;

    private PhaseAShotCrossbowExecutionEvidenceValidation() {
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

    /** Persistent evidence is artifact-local and does not depend on a newer active RUN_STATE. */
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
                throw new IllegalStateException("SHOT_CROSSBOW evidence entry is not an object");
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
        Set<String> actualKeys = new LinkedHashSet<>();
        for (JsonObject receipt : artifact.entries()) {
            String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
            if (!actualKeys.add(key)) {
                throw new IllegalStateException("Duplicate SHOT_CROSSBOW evidence key: " + key);
            }
            CatalogCase definition = catalog.byKey().get(key);
            if (definition == null) {
                throw new IllegalStateException("Unknown SHOT_CROSSBOW evidence key: " + key);
            }
            validateReceipt(receipt, definition, artifact.runId(), fingerprint);
        }
        if (exactCoverage && !actualKeys.equals(catalog.keys())) {
            throw new IllegalStateException("SHOT_CROSSBOW evidence does not exactly cover the catalog"
                + " | expected=" + catalog.keys() + " | actual=" + actualKeys);
        }
    }

    private static Catalog loadCatalog(Path projectRoot) throws IOException {
        Path path = projectRoot.resolve(CATALOG_PATH);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing SHOT_CROSSBOW catalog: " + path);
        }
        String actual = Files.readString(path, StandardCharsets.UTF_8);
        String expected = PhaseAShotCrossbowCertification.generateSnapshot(projectRoot);
        if (!actual.equals(expected)) {
            throw new IllegalStateException("SHOT_CROSSBOW catalog is stale or not derived from frozen BACAP semantics");
        }
        JsonObject root = JsonParser.parseString(actual).getAsJsonObject();
        requireString(root, "snapshot", PhaseAShotCrossbowCertification.SNAPSHOT_ID);
        requireInt(root, "schemaVersion", SCHEMA_VERSION);
        requireString(root, "family", FAMILY);
        requireInt(root, "canonicalAdvancementCount", 1152);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "semanticsSource", "frozenBacap");
        requireString(root, "sourceArtifactClassification", "FROZEN_BACAP_DEFINITION");
        requireString(root, "runtimeBoundary", BOUNDARY);
        requireString(root, "runtimePacketPath", PACKET_PATH);
        JsonObject summary = requiredObject(root, "summary");
        requireInt(summary, "totalCases", 2);
        requireInt(summary, "uniqueKeys", 2);
        requireInt(summary, "uniqueAdvancements", 2);
        requireInt(summary, "requirementGroups", 2);
        requireInt(summary, "automationSupported", 2);
        requireInt(summary, "automationDeferred", 0);
        JsonObject modes = requiredObject(summary, "firingModes");
        requireInt(modes, "QUICK_CHARGE", 1);
        requireInt(modes, "MULTISHOT", 1);
        JsonArray casesJson = requiredArray(root, "cases");
        if (casesJson.size() != EXPECTED_KEYS.size()) {
            throw new IllegalStateException("Expected two SHOT_CROSSBOW catalog cases");
        }
        Map<String, CatalogCase> byKey = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>();
        for (JsonElement element : casesJson) {
            CatalogCase definition = CatalogCase.fromJson(element.getAsJsonObject());
            if (byKey.put(definition.key(), definition) != null) {
                throw new IllegalStateException("Duplicate SHOT_CROSSBOW catalog case " + definition.key());
            }
            keys.add(definition.key());
        }
        if (!keys.equals(EXPECTED_KEYS)) {
            throw new IllegalStateException("SHOT_CROSSBOW catalog key order mismatch: " + keys);
        }
        return new Catalog(byKey, new LinkedHashSet<>(keys));
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
        requireString(receipt, "selectedItem", "minecraft:crossbow");
        requireString(receipt, "enchantment", definition.enchantment());
        requireString(receipt, "levelRule", definition.levelRule());
        requireInt(receipt, "configuredEnchantmentLevel", definition.enchantmentLevel());
        int actualLevel = requiredInt(receipt, "actualEnchantmentLevel");
        if ("min".equals(definition.levelRule()) ? actualLevel < definition.enchantmentLevel() : actualLevel < 1) {
            throw new IllegalStateException("Enchantment predicate witness is not satisfied for " + definition.key());
        }
        requireBoolean(receipt, "itemPredicateSatisfied", true);

        requireString(receipt, "boundary", BOUNDARY);
        requireString(receipt, "packetPath", PACKET_PATH);
        requireString(receipt, "hand", "MAIN_HAND");
        requireString(receipt, "playerClass", "net.minecraft.server.level.ServerPlayer");
        requireUuid(receipt, "playerUuid");
        requiredNonBlank(receipt, "profileName");
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
            throw new IllegalStateException("Invalid SHOT_CROSSBOW criterion tick count");
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
        requireInt(loaded, "count", definition.loadedProjectiles());
        requireInt(loaded, "expectedCount", definition.loadedProjectiles());
        requireString(loaded, "itemType", "minecraft:arrow");
        requireBoolean(loaded, "chargedComponentNonEmpty", true);
        requireBoolean(receipt, "loadedProjectileWitness", true);

        JsonObject ammo = requiredObject(receipt, "ammoState");
        requireString(ammo, "item", "minecraft:arrow");
        requireInt(ammo, "countBefore", 1);
        requireInt(ammo, "countAfter", 0);
        requireInt(ammo, "consumed", definition.ammoConsumed());
        requireInt(ammo, "additionalCopiesNotConsumed", Math.max(0, definition.loadedProjectiles() - 1));
        requireBoolean(ammo, "finiteSurvivalConsumption", true);

        JsonObject crossbow = requiredObject(receipt, "crossbowState");
        requireString(crossbow, "itemBefore", "minecraft:crossbow");
        requireString(crossbow, "itemAfter", "minecraft:crossbow");
        requireInt(crossbow, "damageBefore", 0);
        requireInt(crossbow, "damageAfter", definition.durabilityUse());
        requireInt(crossbow, "durabilityUse", definition.durabilityUse());
        requireBoolean(crossbow, "chargedBeforeRelease", true);
        requireBoolean(crossbow, "chargedAfterShoot", false);
        requireBoolean(crossbow, "sameStackReference", true);

        JsonObject projectiles = requiredObject(receipt, "projectileObservation");
        requireInt(projectiles, "expectedCount", definition.firedProjectiles());
        requireInt(projectiles, "spawnedCount", definition.firedProjectiles());
        requireInt(projectiles, "firedProjectileCount", definition.firedProjectiles());
        requireBoolean(projectiles, "allAreArrowProjectiles", true);
        requireBoolean(projectiles, "allHavePlayerOwner", true);
        requireBoolean(projectiles, "ownerIdentityMatches", true);
        requireBoolean(projectiles, "nativeSpawnObserved", true);
        JsonArray ids = requiredArray(projectiles, "entityIds");
        JsonArray uuids = requiredArray(projectiles, "entityUuids");
        JsonArray types = requiredArray(projectiles, "entityTypes");
        JsonArray owners = requiredArray(projectiles, "owners");
        if (ids.size() != definition.firedProjectiles() || uuids.size() != definition.firedProjectiles()
            || types.size() != definition.firedProjectiles() || owners.size() != definition.firedProjectiles()) {
            throw new IllegalStateException("Projectile witness cardinality mismatch for " + definition.key());
        }
        Set<String> seenIds = new LinkedHashSet<>();
        Set<String> seenUuids = new LinkedHashSet<>();
        String playerUuid = requiredNonBlank(receipt, "playerUuid");
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i).getAsInt();
            if (id <= 0 || !seenIds.add(Integer.toString(id))) {
                throw new IllegalStateException("Invalid projectile entity id for " + definition.key());
            }
            String uuid = requiredStringValue(uuids.get(i), "projectile entity UUID");
            requireUuidValue(uuid, "projectile entity UUID");
            if (!seenUuids.add(uuid)) {
                throw new IllegalStateException("Duplicate projectile UUID for " + definition.key());
            }
            if (!"minecraft:arrow".equals(requiredStringValue(types.get(i), "projectile entity type"))) {
                throw new IllegalStateException("Non-arrow projectile witness for " + definition.key());
            }
            if (!playerUuid.equals(requiredStringValue(owners.get(i), "projectile owner"))) {
                throw new IllegalStateException("Projectile owner mismatch for " + definition.key());
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
        if (requiredInt(cleanup, "settlementMessages") < 0) {
            throw new IllegalStateException("Negative channel settlement count");
        }
        requireInt(cleanup, "warningCount", 0);
    }

    private static JsonObject readJson(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing SHOT_CROSSBOW evidence artifact: " + path);
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
        return requiredStringValue(json.get(key), key);
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
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in SHOT_CROSSBOW evidence");
        }
    }

    private static String requiredString(JsonObject json, String key) {
        return requiredStringValue(json.get(key), key);
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredString(json, key))) {
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

    private static void requireUuid(JsonObject json, String key) {
        requireUuidValue(requiredNonBlank(json, key), key);
    }

    private static void requireUuidValue(String value, String key) {
        try {
            UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid UUID in " + key, e);
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
        String enchantment,
        String levelRule,
        int enchantmentLevel,
        String firingMode,
        int loadedProjectiles,
        int firedProjectiles,
        int ammoConsumed,
        int durabilityUse
    ) {
        static CatalogCase fromJson(JsonObject json) {
            String advancementId = requiredString(json, "advancementId");
            String criterion = requiredString(json, "criterion");
            requireInt(json, "requirementGroupIndex", 0);
            requireInt(json, "requirementGroupCount", 1);
            requireString(json, "trigger", TRIGGER);
            requireString(json, "selectedItem", "minecraft:crossbow");
            JsonObject predicate = requiredObject(json, "enchantmentPredicate");
            String enchantment = requiredString(predicate, "enchantment");
            String levelRule = requiredString(predicate, "levelRule");
            int enchantmentLevel = requiredInt(predicate, "level");
            String firingMode = requiredString(json, "firingMode");
            int loaded = requiredInt(json, "expectedLoadedProjectileCount");
            int fired = requiredInt(json, "expectedFiredProjectileCount");
            int ammo = requiredInt(json, "expectedAmmoConsumed");
            int durability = requiredInt(json, "expectedDurabilityUse");
            if ("QUICK_CHARGE".equals(firingMode)) {
                if (!"minecraft:quick_charge".equals(enchantment) || !"min".equals(levelRule)
                    || enchantmentLevel != 3 || loaded != 1 || fired != 1 || ammo != 1 || durability != 1) {
                    throw new IllegalStateException("Invalid QUICK_CHARGE catalog case");
                }
            } else if ("MULTISHOT".equals(firingMode)) {
                if (!"minecraft:multishot".equals(enchantment) || !"present".equals(levelRule)
                    || enchantmentLevel != 1 || loaded != 3 || fired != 3 || ammo != 1 || durability != 3) {
                    throw new IllegalStateException("Invalid MULTISHOT catalog case");
                }
            } else {
                throw new IllegalStateException("Unknown SHOT_CROSSBOW firing mode");
            }
            requireString(json, "boundary", BOUNDARY);
            requireString(json, "packetPath", PACKET_PATH);
            requireString(json, "automationEligibility", "SUPPORTED");
            requireString(json, "semanticsSource", "frozenBacap");
            requireString(json, "sourceArtifactClassification", "FROZEN_BACAP_DEFINITION");
            requiredString(json, "sourceJsonSha256");
            return new CatalogCase(
                advancementId,
                criterion,
                enchantment,
                levelRule,
                enchantmentLevel,
                firingMode,
                loaded,
                fired,
                ammo,
                durability
            );
        }

        String key() {
            return advancementId + "#" + criterion;
        }
    }
}
