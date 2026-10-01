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

/**
 * Fail-closed validator for the item-tag item_used_on_block evidence contract.
 * PERSISTENT validation deliberately reads only the future artifact and the
 * current catalog; it never consults active TEMP run state.
 */
public final class PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation {
    public enum Mode {
        TEMP_DIAGNOSTIC,
        TEMP_PROMOTABLE,
        PERSISTENT
    }

    public static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification",
        "item_tag_item_used_on_block_execution_evidence.json"
    );
    public static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "item_tag_item_used_on_block_execution_evidence.json"
    );
    public static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification",
        "item_tag_item_used_on_block_case_catalog.json"
    );
    public static final String SNAPSHOT = "phase_a_item_tag_item_used_on_block_execution_evidence";
    public static final String FAMILY = PhaseAItemTagItemUsedOnBlockCertification.FAMILY;
    public static final String SOURCE = "PhaseAItemTagItemUsedOnBlockGameTest";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String GREEN = "GREEN";
    public static final String BOUNDARY = "ServerPlayerGameMode.useItemOn";
    public static final String TRIGGER_PATH = "ServerPlayerGameMode.useItemOn->ItemUsedOnLocationTrigger";
    public static final int MAX_POLL_TICKS = 10;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation() {
    }

    public static RuntimeArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, Mode.TEMP_DIAGNOSTIC);
    }

    public static RuntimeArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, Mode.TEMP_PROMOTABLE);
    }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        RuntimeArtifact artifact = loadValidatedArtifact(projectRoot, PERSISTENT_ARTIFACT, Mode.PERSISTENT);
        Map<String, RuntimeEvidenceData> evidence = new LinkedHashMap<>();
        for (RuntimeReceipt receipt : artifact.receipts()) {
            RuntimeEvidenceData data = evidence.computeIfAbsent(receipt.advancementId(), RuntimeEvidenceData::empty);
            data.greenCriteria().add(receipt.criterion());
            data.criteriaByFamily().computeIfAbsent(receipt.family(), ignored -> new TreeSet<>()).add(receipt.criterion());
            data.criteriaBySource().computeIfAbsent(receipt.source(), ignored -> new TreeSet<>()).add(receipt.criterion());
            data.families().add(receipt.family());
            data.sources().add(receipt.source());
        }
        return evidence;
    }

    public static RuntimeArtifact validateArtifact(Path projectRoot, Path relativeArtifact, Mode mode) throws IOException {
        return loadValidatedArtifact(projectRoot, relativeArtifact, mode);
    }

    public static String fingerprint(Path catalogPath) throws IOException {
        try {
            byte[] bytes = Files.readAllBytes(catalogPath);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha-256:" + hex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    /** Test/support helper for an in-memory copy of the pretty-printed catalog. */
    public static String fingerprintFromText(JsonObject catalog) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = (GSON.toJson(catalog) + "\n").getBytes(StandardCharsets.UTF_8);
            return "sha-256:" + hex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    private static RuntimeArtifact loadValidatedArtifact(Path projectRoot, Path relativeArtifact, Mode mode) throws IOException {
        Path artifactPath = projectRoot.resolve(relativeArtifact);
        if (!Files.exists(artifactPath)) {
            throw new IllegalStateException("Missing " + relativeArtifact + " ITEM_TAG_ITEM_USED_ON_BLOCK artifact");
        }
        Path catalogPath = projectRoot.resolve(CATALOG_PATH);
        Catalog catalog = loadCatalog(projectRoot, catalogPath);
        String catalogFingerprint = fingerprint(catalogPath);
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
                throw new IllegalStateException("Runtime evidence entry is not an object");
            }
            JsonObject entry = element.getAsJsonObject();
            String advancementId = requiredNonBlank(entry, "advancementId");
            String criterion = requiredNonBlank(entry, "criterion");
            String key = advancementId + "#" + criterion;
            if (!actualKeys.add(key)) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + key);
            }
            CatalogCase catalogCase = catalog.byKey().get(key);
            if (catalogCase == null) {
                throw new IllegalStateException("Unknown runtime evidence receipt key: " + key);
            }
            validateReceipt(entry, catalogCase, runId, catalogFingerprint);
            receipts.add(new RuntimeReceipt(
                advancementId,
                criterion,
                requiredNonBlank(entry, "family"),
                requiredNonBlank(entry, "source")
            ));
        }

        if (mode != Mode.TEMP_DIAGNOSTIC && !actualKeys.equals(catalog.keys())) {
            throw new IllegalStateException("Runtime evidence keys do not exactly match the current 15-key catalog"
                + " | expected=" + catalog.keys() + " | actual=" + actualKeys);
        }
        return new RuntimeArtifact(runId, requiredNonBlank(root, "generatedAt"), List.copyOf(receipts), mode);
    }

    private static Catalog loadCatalog(Path projectRoot, Path catalogPath) throws IOException {
        if (!Files.exists(catalogPath)) {
            throw new IllegalStateException("Missing ITEM_TAG_ITEM_USED_ON_BLOCK catalog: " + catalogPath);
        }
        String actual = Files.readString(catalogPath, StandardCharsets.UTF_8);
        String expected = PhaseAItemTagItemUsedOnBlockCertification.generateSnapshot(projectRoot);
        if (!actual.equals(expected)) {
            throw new IllegalStateException("ITEM_TAG_ITEM_USED_ON_BLOCK catalog is stale or not derived from frozen BACAP semantics");
        }
        JsonObject root = JsonParser.parseString(actual).getAsJsonObject();
        requireString(root, "snapshot", PhaseAItemTagItemUsedOnBlockCertification.SNAPSHOT_ID);
        requireInt(root, "schemaVersion", 1);
        requireString(root, "family", FAMILY);
        requireInt(root, "canonicalAdvancementCount", 1152);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "semanticsSource", PhaseAItemTagItemUsedOnBlockCertification.SEMANTICS_SOURCE);
        requireString(root, "frozenBacapSha1", PhaseAItemTagItemUsedOnBlockCertification.FROZEN_BACAP_SHA1);
        requireString(root, "frozenBacapSha256", PhaseAItemTagItemUsedOnBlockCertification.FROZEN_BACAP_SHA256);
        JsonObject summary = requiredObject(root, "summary");
        requireInt(summary, "totalCases", 15);
        requireInt(summary, "uniqueKeys", 15);
        requireInt(summary, "uniqueAdvancements", 4);
        requireInt(summary, "requirementGroups", 15);
        requireInt(summary, "automationSupported", 15);
        requireInt(summary, "automationDeferred", 0);

        JsonArray cases = requiredArray(root, "cases");
        if (cases.size() != 15) {
            throw new IllegalStateException("Expected exactly 15 catalog cases but found " + cases.size());
        }
        Map<String, CatalogCase> byKey = new LinkedHashMap<>();
        List<String> orderedKeys = new ArrayList<>();
        for (JsonElement element : cases) {
            JsonObject caseJson = element.getAsJsonObject();
            CatalogCase catalogCase = CatalogCase.fromJson(caseJson);
            if (byKey.put(catalogCase.key(), catalogCase) != null) {
                throw new IllegalStateException("Duplicate catalog key: " + catalogCase.key());
            }
            orderedKeys.add(catalogCase.key());
        }
        if (!orderedKeys.equals(PhaseAItemTagItemUsedOnBlockCertification.EXPECTED_KEYS)) {
            throw new IllegalStateException("Catalog key order/set mismatch: " + orderedKeys);
        }
        return new Catalog(byKey, new LinkedHashSet<>(orderedKeys));
    }

    private static void validateReceipt(JsonObject entry, CatalogCase catalogCase, String runId, String catalogFingerprint) {
        String key = catalogCase.key();
        requireString(entry, "family", FAMILY);
        requireString(entry, "source", SOURCE);
        requireString(entry, "catalogFingerprint", catalogFingerprint);
        requireString(entry, "runId", runId);
        requireString(entry, "minecraftVersion", MINECRAFT_VERSION);
        requireString(entry, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(entry, "result", GREEN);
        requireInt(entry, "requirementGroupIndex", catalogCase.requirementGroupIndex());
        requireString(entry, "trigger", PhaseAItemTagItemUsedOnBlockCertification.TRIGGER);
        requireString(entry, "itemTag", catalogCase.itemTag());
        requireString(entry, "selectedItem", catalogCase.preferredToolItem());
        requireBoolean(entry, "runtimeItemTagMembership", true);
        requirePositive(entry, "runtimeItemTagMemberCount");
        requireString(entry, "boundary", BOUNDARY);
        requireString(entry, "hand", "MAIN_HAND");
        requireString(entry, "playerClass", "net.minecraft.server.level.ServerPlayer");
        requireNonBlank(entry, "playerUuid");
        requireString(entry, "gameMode", "SURVIVAL");
        requireBoolean(entry, "joined", true);
        requireBoolean(entry, "connectionRegistered", true);
        requireBoolean(entry, "clientLoaded", true);
        requireBoolean(entry, "normalScheduler", true);
        requireBoolean(entry, "buildPermission", true);
        requireNonBlank(entry, "clickedPosition");
        requireNonBlank(entry, "blockStateBefore");
        requireNonBlank(entry, "blockStateAfter");
        requireString(entry, "blockIdBefore", catalogCase.fixtureBlockBefore());
        requireString(entry, "blockIdAfter", catalogCase.expectedPostBlock());
        String interactionResult = requiredNonBlank(entry, "interactionResult");
        if (!interactionResult.startsWith("Success[")) {
            throw new IllegalStateException("Expected 26.2 consuming Success interaction for " + key + ": " + interactionResult);
        }
        requireBoolean(entry, "interactionConsumesAction", true);
        requireBoolean(entry, "semanticMutation", true);
        requireBoolean(entry, "criterionBefore", false);
        requireBoolean(entry, "criterionAfter", true);
        int ticksToCriterion = requiredInt(entry, "ticksToCriterion");
        if (ticksToCriterion < 0 || ticksToCriterion > MAX_POLL_TICKS) {
            throw new IllegalStateException("Unexpected criterion polling budget for " + key + ": " + ticksToCriterion);
        }

        JsonObject preProperties = requiredObject(entry, "blockStateBeforeProperties");
        JsonObject postProperties = requiredObject(entry, "blockStateAfterProperties");
        requireStateProperties(preProperties, catalogCase.fixtureStateBefore(), "blockStateBeforeProperties", key);
        requireStateProperties(postProperties, catalogCase.expectedPostState(), "blockStateAfterProperties", key);

        JsonObject preconditions = requiredObject(entry, "productionPreconditions");
        requireString(preconditions, "scoreboardObjective", "bac_advancements");
        requireInt(preconditions, "scoreBefore", 0);
        requireInt(preconditions, "scoreAfter", 1000);
        requireBoolean(preconditions, "abilityLockedBefore", true);
        requireBoolean(preconditions, "abilityLockedAfter", false);
        requireBoolean(preconditions, "lockedLandmark", false);
        requireString(preconditions, "ability", "USE_DIAMOND_TOOLS");

        JsonObject stack = requiredObject(entry, "interactionStack");
        requireString(stack, "itemBefore", catalogCase.preferredToolItem());
        requireString(stack, "itemAfter", catalogCase.preferredToolItem());
        requireInt(stack, "countBefore", 1);
        requireInt(stack, "countAfter", 1);
        int damageBefore = requiredInt(stack, "damageBefore");
        int damageAfter = requiredInt(stack, "damageAfter");
        if (damageBefore < 0 || damageAfter != damageBefore + 1) {
            throw new IllegalStateException("Expected one ordinary tool durability damage for " + key
                + " but got " + damageBefore + " -> " + damageAfter);
        }
        requireBooleanField(stack, "sameStackReference");

        JsonObject proof = requiredObject(entry, "actionProof");
        requireString(proof, "action", catalogCase.action());
        requireBoolean(proof, "realUseOn", true);
        requireString(proof, "triggerPath", TRIGGER_PATH);
        requireString(proof, "preBlock", catalogCase.fixtureBlockBefore());
        requireString(proof, "postBlock", catalogCase.expectedPostBlock());
        requireBoolean(proof, "interactionConsumesAction", true);
        requireInt(proof, "toolDamageDelta", 1);
        requireBooleanField(proof, "sameStackReference");
        requireBoolean(proof, "criterionBefore", false);
        requireBoolean(proof, "criterionAfter", true);
        requireActionProof(proof, catalogCase, key);
    }

    private static void requireActionProof(JsonObject proof, CatalogCase catalogCase, String key) {
        switch (catalogCase.action()) {
            case "STRIP_WOOD", "STRIP_LOG" -> {
                requireString(proof, "operation", "STRIP");
                requireString(proof, "vanillaMethod", "AxeItem.evaluateNewBlockState");
            }
            case "CREATE_PATH" -> {
                requireString(proof, "operation", "FLATTEN");
                requireString(proof, "vanillaMethod", "ShovelItem.useOn");
                requireString(proof, "clickedFace", "UP");
                requireString(proof, "aboveBlock", "minecraft:air");
            }
            case "AXE_COPPER_MUTATION" -> {
                requireString(proof, "operation", "DEWAX");
                requireString(proof, "vanillaMethod", "AxeItem.evaluateNewBlockState");
                requireBoolean(proof, "litBefore", true);
                requireBoolean(proof, "litAfter", true);
                requireBoolean(proof, "waxedBefore", true);
                requireBoolean(proof, "waxedAfter", false);
            }
            default -> throw new IllegalStateException("Unsupported action proof category for " + key + ": " + catalogCase.action());
        }
    }

    private static void requireStateProperties(JsonObject actual, Map<String, String> expected, String field, String key) {
        if (actual.size() != expected.size()) {
            throw new IllegalStateException("State property count mismatch for " + key + " in " + field);
        }
        for (Map.Entry<String, String> property : expected.entrySet()) {
            if (!actual.has(property.getKey()) || !property.getValue().equals(actual.get(property.getKey()).getAsString())) {
                throw new IllegalStateException("State property mismatch for " + key + " in " + field + ": " + property.getKey());
            }
        }
    }

    private static JsonObject readJson(Path path) throws IOException {
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

    private static String requiredNonBlank(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        String value = json.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Expected nonblank " + key);
        }
        return value;
    }

    private static int requiredInt(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        return json.get(key).getAsInt();
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!json.has(key) || !expected.equals(json.get(key).getAsString())) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    private static void requireBoolean(JsonObject json, String key, boolean expected) {
        if (!json.has(key) || json.get(key).getAsBoolean() != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    private static void requireBooleanField(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.getAsJsonPrimitive(key).isBoolean()) {
            throw new IllegalStateException("Expected boolean field " + key);
        }
    }

    private static void requirePositive(JsonObject json, String key) {
        if (requiredInt(json, key) <= 0) {
            throw new IllegalStateException("Expected " + key + ">0");
        }
    }

    private static void requireNonBlank(JsonObject json, String key) {
        requiredNonBlank(json, key);
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(Character.forDigit((value >>> 4) & 0xF, 16));
            result.append(Character.forDigit(value & 0xF, 16));
        }
        return result.toString();
    }

    public record RuntimeArtifact(String runId, String generatedAt, List<RuntimeReceipt> receipts, Mode mode) {
    }

    public record RuntimeReceipt(String advancementId, String criterion, String family, String source) {
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
        int requirementGroupIndex,
        String itemTag,
        String action,
        String fixtureBlockBefore,
        String expectedPostBlock,
        Map<String, String> fixtureStateBefore,
        Map<String, String> expectedPostState,
        String preferredToolItem
    ) {
        static CatalogCase fromJson(JsonObject json) {
            String advancementId = requiredNonBlank(json, "advancementId");
            String criterion = requiredNonBlank(json, "criterion");
            requireString(json, "trigger", PhaseAItemTagItemUsedOnBlockCertification.TRIGGER);
            requireString(json, "automationEligibility", PhaseAItemTagItemUsedOnBlockCertification.SUPPORTED);
            requireString(json, "semanticsSource", PhaseAItemTagItemUsedOnBlockCertification.SEMANTICS_SOURCE);
            JsonObject predicate = requiredObject(json, "blockPredicate");
            if (!predicate.has("blocks") || !predicate.get("blocks").isJsonArray()) {
                throw new IllegalStateException("Missing blockPredicate.blocks for " + advancementId + "#" + criterion);
            }
            requireNonBlank(json, "itemTag");
            requireNonBlank(json, "action");
            requireNonBlank(json, "fixtureBlockBefore");
            requireNonBlank(json, "expectedPostBlock");
            requireNonBlank(json, "preferredToolItem");
            return new CatalogCase(
                advancementId,
                criterion,
                requiredInt(json, "requirementGroupIndex"),
                json.get("itemTag").getAsString(),
                json.get("action").getAsString(),
                json.get("fixtureBlockBefore").getAsString(),
                json.get("expectedPostBlock").getAsString(),
                readState(json, "fixtureStateBefore"),
                readState(json, "expectedPostState"),
                json.get("preferredToolItem").getAsString()
            );
        }

        String key() {
            return advancementId + "#" + criterion;
        }

        private static Map<String, String> readState(JsonObject parent, String key) {
            JsonObject state = requiredObject(parent, key);
            Map<String, String> values = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
                values.put(entry.getKey(), entry.getValue().getAsString());
            }
            return Map.copyOf(values);
        }
    }
}
