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

/** Independent fail-closed validator for USING_ITEM TEMP and persistent evidence. */
public final class PhaseAUsingItemExecutionEvidenceValidation {
    public static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification", "using_item_execution_evidence.json"
    );
    public static final Path RUN_STATE_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification", "using_item_execution_evidence.run.json"
    );
    public static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification", "using_item_execution_evidence.json"
    );
    public static final Path CATALOG_PATH = PhaseAUsingItemCertification.SNAPSHOT;
    public static final String SNAPSHOT = "phase_a_using_item_execution_evidence";
    public static final String FAMILY = PhaseAUsingItemCertification.FAMILY;
    public static final String SOURCE = "PhaseAUsingItemGameTest";
    public static final String MINECRAFT_VERSION = PhaseAUsingItemCertification.MINECRAFT_VERSION;
    public static final String COMPATIBILITY_MARKER = PhaseAUsingItemCertification.COMPATIBILITY_MARKER;
    public static final String TRIGGER = PhaseAUsingItemCertification.TRIGGER;
    public static final String GREEN = "GREEN";
    public static final String BOUNDARY = PhaseAUsingItemCertification.BOUNDARY;
    public static final String PACKET_PATH = PhaseAUsingItemCertification.PACKET_PATH;
    public static final String NATIVE_ACTIVE_USE_ACCEPTED = "NATIVE_ACTIVE_USE_ACCEPTED";
    private static final int SCHEMA_VERSION = 1;
    private static final List<String> EXPECTED_KEYS = PhaseAUsingItemCertification.EXPECTED_KEYS;

    private PhaseAUsingItemExecutionEvidenceValidation() {
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
        System.out.println(args[0].toUpperCase() + "_DIAGNOSTIC=PASS family=" + FAMILY
            + " runId=" + artifact.runId() + " entries=" + artifact.entries().size());
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
            data.criteriaByFamily().computeIfAbsent(family, ignored -> new TreeSet<>()).add(criterion);
            data.criteriaBySource().computeIfAbsent(source, ignored -> new TreeSet<>()).add(criterion);
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
            throw new IllegalStateException("Unknown USING_ITEM evidence key: " + key);
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
        requireUuidValue(runId, "run-state runId");
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
        requireUuidValue(runId, "runId");
        String generatedAt = requiredNonBlank(root, "generatedAt");
        JsonArray entriesJson = requiredArray(root, "entries");
        List<JsonObject> entries = new ArrayList<>();
        for (JsonElement element : entriesJson) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("USING_ITEM evidence entry is not an object");
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
        Set<String> playerUuids = new LinkedHashSet<>();
        for (JsonObject receipt : artifact.entries()) {
            String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
            if (!actualKeys.add(key)) {
                throw new IllegalStateException("Duplicate USING_ITEM evidence key: " + key);
            }
            CatalogCase definition = catalog.byKey().get(key);
            if (definition == null) {
                throw new IllegalStateException("Unknown USING_ITEM evidence key: " + key);
            }
            validateReceipt(receipt, definition, artifact.runId(), fingerprint);
            playerUuids.add(requiredNonBlank(receipt, "playerUuid"));
        }
        if (exactCoverage && !actualKeys.equals(catalog.keys())) {
            throw new IllegalStateException("USING_ITEM evidence does not exactly cover the catalog"
                + " | expected=" + catalog.keys() + " | actual=" + actualKeys);
        }
        if (playerUuids.size() != actualKeys.size()) {
            throw new IllegalStateException("USING_ITEM evidence did not use fresh player identities per case");
        }
    }

    private static Catalog loadCatalog(Path projectRoot) throws IOException {
        Path path = projectRoot.resolve(CATALOG_PATH);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing USING_ITEM catalog: " + path);
        }
        String actual = Files.readString(path, StandardCharsets.UTF_8);
        String expected = PhaseAUsingItemCertification.generateSnapshot(projectRoot);
        if (!actual.equals(expected)) {
            throw new IllegalStateException("USING_ITEM catalog is stale or not derived from frozen BACAP semantics");
        }
        JsonObject root = JsonParser.parseString(actual).getAsJsonObject();
        requireString(root, "snapshot", PhaseAUsingItemCertification.SNAPSHOT_ID);
        requireInt(root, "schemaVersion", SCHEMA_VERSION);
        requireString(root, "family", FAMILY);
        requireInt(root, "canonicalAdvancementCount", 1152);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "semanticsSource", "frozenBacap");
        requireString(root, "sourceArtifactClassification", "FROZEN_BACAP_DEFINITION");
        requireString(root, "runtimeBoundary", BOUNDARY);
        requireString(root, "runtimePacketPath", PACKET_PATH);
        requireString(root, "runtimeCriterionBoundary",
            "ServerPlayer.updateUsingItem->CriteriaTriggers.USING_ITEM");
        JsonObject summary = requiredObject(root, "summary");
        requireInt(summary, "totalCases", 2);
        requireInt(summary, "uniqueKeys", 2);
        requireInt(summary, "uniqueAdvancements", 2);
        requireInt(summary, "requirementGroups", 2);
        requireInt(summary, "automationSupported", 2);
        requireInt(summary, "automationDeferred", 0);
        JsonObject actions = requiredObject(summary, "actions");
        requireInt(actions, "GOAT_HORN_USE", 1);
        requireInt(actions, "RIPTIDE_TRIDENT_USE", 1);
        JsonArray casesJson = requiredArray(root, "cases");
        if (casesJson.size() != 2) {
            throw new IllegalStateException("Expected exactly two USING_ITEM catalog cases");
        }
        Map<String, CatalogCase> byKey = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>();
        for (JsonElement element : casesJson) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("USING_ITEM catalog case is not an object");
            }
            CatalogCase definition = CatalogCase.fromJson(element.getAsJsonObject());
            if (byKey.put(definition.key(), definition) != null) {
                throw new IllegalStateException("Duplicate USING_ITEM catalog key: " + definition.key());
            }
            keys.add(definition.key());
        }
        if (!keys.equals(PhaseAUsingItemCertification.EXPECTED_KEYS)) {
            throw new IllegalStateException("USING_ITEM catalog key/order mismatch: " + keys);
        }
        return new Catalog(byKey, new LinkedHashSet<>(keys));
    }

    private static void validateReceipt(JsonObject receipt, CatalogCase definition, String runId, String fingerprint) {
        requireString(receipt, "family", FAMILY);
        requireString(receipt, "source", SOURCE);
        requireString(receipt, "catalogFingerprint", fingerprint);
        requireString(receipt, "runId", runId);
        requireUuidValue(runId, "runId");
        requireString(receipt, "minecraftVersion", MINECRAFT_VERSION);
        requireString(receipt, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(receipt, "result", GREEN);
        requireString(receipt, "advancementId", definition.advancementId());
        requireString(receipt, "criterion", definition.criterion());
        requireInt(receipt, "requirementGroupIndex", 0);
        requireString(receipt, "trigger", TRIGGER);

        requireString(receipt, "playerClass", "net.minecraft.server.level.ServerPlayer");
        String playerUuid = requiredNonBlank(receipt, "playerUuid");
        requireUuidValue(playerUuid, "playerUuid");
        String profileName = requiredNonBlank(receipt, "profileName");
        if (profileName.length() > 16) {
            throw new IllegalStateException("Profile name exceeds 16 characters");
        }
        requireString(receipt, "gameMode", "SURVIVAL");
        requireBoolean(receipt, "finiteMaterials", true);
        requireBoolean(receipt, "abilitiesInstabuild", false);
        requireBoolean(receipt, "spectator", false);
        requireBoolean(receipt, "joined", true);
        requireBoolean(receipt, "connectionRegistered", true);
        requireBoolean(receipt, "clientLoaded", true);
        requireBoolean(receipt, "normalScheduler", true);

        requireString(receipt, "selectedItem", definition.selectedItem());
        requireString(receipt, "hand", "MAIN_HAND");
        int slot = requiredInt(receipt, "slot");
        if (slot < 0 || slot > 8) {
            throw new IllegalStateException("Selected slot is outside the hotbar");
        }
        requireInt(receipt, "itemCountBefore", 1);
        requireInt(receipt, "itemCountAfter", 1);
        requireNonBlank(receipt, "componentsBefore");
        requireNonBlank(receipt, "componentsAfter");
        requireBoolean(receipt, "sameStackReference", true);
        requireBoolean(receipt, "itemPredicateSatisfied", true);

        JsonObject enchantment = requiredObject(receipt, "enchantmentWitness");
        boolean riptide = definition.riptide();
        requireBoolean(enchantment, "required", riptide);
        requireString(enchantment, "id", riptide ? "minecraft:riptide" : "minecraft:none");
        int level = requiredInt(enchantment, "level");
        if (riptide && level < 1) {
            throw new IllegalStateException("Riptide enchantment level is absent");
        }
        if (!riptide && level != 0) {
            throw new IllegalStateException("Goat horn unexpectedly has an enchantment");
        }
        requireBoolean(enchantment, "componentPresent", riptide);
        requireBoolean(enchantment, "beforeMatches", true);
        requireBoolean(enchantment, "afterMatches", true);

        JsonObject production = requiredObject(receipt, "productionUnlockWitness");
        requireString(production, "scoreboardObjective", "bac_advancements");
        requireString(production, "itemRewardsScoreboard", "bac_settings");
        requireInt(production, "itemRewardsScore", 0);
        requireBoolean(production, "itemRewardsDisabled", true);
        requireBoolean(production, "defaultAdvancementsMode", true);
        requireBoolean(production, "gatePresent", definition.gatePresent());
        requireString(production, "ability", definition.ability());
        requireInt(production, "unlockThreshold", definition.unlockThreshold());
        requireInt(production, "scoreBefore", 0);
        requireInt(production, "scoreAfter", definition.unlockThreshold());
        requireBoolean(production, "scoreStable", true);
        requireBoolean(production, "abilityLockedBefore", definition.gatePresent());
        requireBoolean(production, "abilityLockedAfter", false);
        requireBoolean(production, "lockedLandmark", false);

        JsonObject environment = requiredObject(receipt, "environmentWitness");
        requireBoolean(environment, "predicateSatisfied", true);
        if (riptide) {
            requireBoolean(environment, "required", true);
            requireBoolean(environment, "waterOrRainRequired", true);
            requireBoolean(environment, "wetBeforeUse", true);
            requireBoolean(environment, "wetAtUse", true);
            requireBoolean(environment, "wetStateObserved", true);
            requireString(environment, "wetStateSource", "Player.isInWaterOrRain");
        } else {
            requireBoolean(environment, "required", true);
            requireString(environment, "biomeId", "minecraft:deep_dark");
            requireBoolean(environment, "biomePredicateSatisfied", true);
            requireBoolean(environment, "structureAlternativeSatisfied", false);
            requireString(environment, "predicate", "minecraft:deep_dark OR minecraft:ancient_city");
        }

        requireString(receipt, "boundary", BOUNDARY);
        requireString(receipt, "packetPath", definition.packetPath());
        requireBoolean(receipt, "criterionBefore", false);
        requireBoolean(receipt, "criterionAfter", true);
        requireBoolean(receipt, "fixtureDidNotTrigger", true);
        requireBoolean(receipt, "legitimateTrigger", true);
        requireBoolean(receipt, "usingItemBefore", false);
        requireBoolean(receipt, "usingItemAfterOrObserved", true);
        requireString(receipt, "actualUseItem", definition.selectedItem());
        int actualUseTicks = requiredInt(receipt, "actualUseTicks");
        if (actualUseTicks < 1) {
            throw new IllegalStateException("USING_ITEM did not observe a scheduler use tick");
        }
        int actualUseDuration = requiredInt(receipt, "actualUseDuration");
        if (riptide ? actualUseDuration != 72000 : actualUseDuration <= 0) {
            throw new IllegalStateException("Unexpected native use duration for " + definition.key());
        }
        requireString(receipt, "actualUseAnimation", riptide ? "TRIDENT" : "TOOT_HORN");
        requireString(receipt, "actualActionBoundary", BOUNDARY);
        requireString(receipt, "actualPacketPath", definition.packetPath());
        requireString(receipt, "actualActionResult", NATIVE_ACTIVE_USE_ACCEPTED);
        requireBoolean(receipt, "usePacketAccepted", true);
        requireBoolean(receipt, "nativeUseStateObserved", true);
        requireBoolean(receipt, "releasePacketAccepted", true);
        requireString(receipt, "releasePacketAction", "RELEASE_USE_ITEM");
        requireBoolean(receipt, "usingAfterRelease", false);
        requireBoolean(receipt, "useStoppedAfterRelease", true);

        requireBoolean(receipt, "noDirectTrigger", true);
        requireBoolean(receipt, "noManualAward", true);
        requireBoolean(receipt, "nativeTriggerObserved", true);
        requireIntAtLeast(receipt, "ticksToCriterion", 1);

        JsonObject proof = requiredObject(receipt, "packetProof");
        requireString(proof, "usePacket", "ServerboundUseItemPacket");
        requireString(proof, "releasePacket", "ServerboundPlayerActionPacket(RELEASE_USE_ITEM)");
        requireString(proof, "serverHandler", BOUNDARY);
        requireString(proof, "nativeCriterionPath", "ServerPlayer.updateUsingItem->CriteriaTriggers.USING_ITEM");
        requireBoolean(proof, "realPacketPath", true);
        requireBoolean(proof, "liveAdvancementProgress", true);
        requireBoolean(proof, "nativeActiveUseState", true);
        requireBoolean(proof, "noDirectTrigger", true);
        requireBoolean(proof, "noManualAward", true);
        requireBoolean(proof, "noManualListenerInvocation", true);

        JsonObject cleanup = requiredObject(receipt, "cleanup");
        requireBoolean(cleanup, "playerRemoved", true);
        requireBoolean(cleanup, "connectionRemoved", true);
        requireBoolean(cleanup, "channelSettled", true);
        requireIntAtLeast(cleanup, "settlementMessages", 0);
        requireInt(cleanup, "warningCount", 0);
    }

    private static JsonObject readJson(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing USING_ITEM evidence file: " + path);
        }
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in USING_ITEM evidence");
        }
        return value.getAsJsonArray();
    }

    private static JsonObject requiredObject(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        if (value == null || !value.isJsonObject()) {
            throw new IllegalStateException("Missing object " + key + " in USING_ITEM evidence");
        }
        return value.getAsJsonObject();
    }

    private static String requiredNonBlank(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in USING_ITEM evidence");
        }
        return value.getAsString();
    }

    private static int requiredInt(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive()) {
            throw new IllegalStateException("Missing number " + key + " in USING_ITEM evidence");
        }
        return value.getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in USING_ITEM evidence");
        }
    }

    private static void requireIntAtLeast(JsonObject json, String key, int minimum) {
        if (requiredInt(json, key) < minimum) {
            throw new IllegalStateException("Expected " + key + ">=" + minimum + " in USING_ITEM evidence");
        }
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredNonBlank(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in USING_ITEM evidence");
        }
    }

    private static void requireBoolean(JsonObject json, String key, boolean expected) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()
            || value.getAsBoolean() != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in USING_ITEM evidence");
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
        String selectedItem,
        String action,
        String packetPath,
        boolean gatePresent,
        String ability,
        int unlockThreshold,
        boolean riptide
    ) {
        static CatalogCase fromJson(JsonObject json) {
            String advancementId = requiredString(json, "advancementId");
            String criterion = requiredString(json, "criterion");
            requireInt(json, "requirementGroupIndex", 0);
            requireInt(json, "requirementGroupCount", 1);
            requireString(json, "trigger", TRIGGER);
            String selectedItem = requiredString(json, "selectedItem");
            String summary = requiredString(json, "itemPredicate");
            requiredArray(json, "requiredItemTags");
            requiredArray(json, "requiredEnchantments");
            requiredObject(json, "requiredComponents");
            requiredObject(json, "environmentalPreconditions");
            JsonObject gate = requiredObject(json, "productionUnlockGate");
            String action = requiredString(json, "action");
            requireString(json, "boundary", BOUNDARY);
            String packetPath = requiredString(json, "packetPath");
            JsonObject useState = requiredObject(json, "expectedUseStateSemantics");
            requireString(json, "automationEligibility", "SUPPORTED");
            requireString(json, "semanticsSource", "frozenBacap");
            requireString(json, "sourceArtifactClassification", "FROZEN_BACAP_DEFINITION");
            requiredNonBlank(json, "sourceJsonSha256");
            boolean riptide = EXPECTED_KEYS.get(1).equals(advancementId + "#" + criterion);
            if (!riptide) {
                if (!EXPECTED_KEYS.get(0).equals(advancementId + "#" + criterion)
                    || !"minecraft:goat_horn".equals(selectedItem)
                    || !"GOAT_HORN_USE".equals(action)
                    || !summary.contains("minecraft:goat_horn")) {
                    throw new IllegalStateException("Invalid goat-horn USING_ITEM catalog case");
                }
            } else if (!"minecraft:trident".equals(selectedItem)
                || !"RIPTIDE_TRIDENT_USE".equals(action)
                || !summary.contains("minecraft:riptide")) {
                throw new IllegalStateException("Invalid Riptide USING_ITEM catalog case");
            }
            boolean gatePresent = requiredBoolean(gate, "present");
            String ability = requiredString(gate, "ability");
            int threshold = requiredInt(gate, "unlockThreshold");
            if (riptide != gatePresent || (riptide && (!"ATTACK_WITH_TRIDENT".equals(ability) || threshold != 244))
                || (!riptide && (!"NONE".equals(ability) || threshold != 0))) {
                throw new IllegalStateException("Invalid USING_ITEM production gate catalog semantics");
            }
            if (riptide) {
                requireInt(useState, "useDurationTicks", 72000);
            } else {
                requireInt(useState, "minimumObservedTicks", 1);
            }
            return new CatalogCase(advancementId, criterion, selectedItem, action, packetPath,
                gatePresent, ability, threshold, riptide);
        }

        String key() {
            return advancementId + "#" + criterion;
        }
    }

    private static String requiredString(JsonObject json, String key) {
        return requiredNonBlank(json, key);
    }

    private static boolean requiredBoolean(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalStateException("Missing boolean " + key + " in USING_ITEM catalog");
        }
        return value.getAsBoolean();
    }
}
