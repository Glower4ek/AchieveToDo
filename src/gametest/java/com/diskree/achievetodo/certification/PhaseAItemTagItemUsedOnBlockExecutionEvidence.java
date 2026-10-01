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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * TEMP recorder and future persistent artifact loader for the exact
 * ITEM_TAG_ITEM_USED_ON_BLOCK family.
 */
public final class PhaseAItemTagItemUsedOnBlockExecutionEvidence {
    public enum Mode {
        TEMP_DIAGNOSTIC,
        TEMP_PROMOTABLE,
        PERSISTENT
    }

    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    public static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification",
        "item_tag_item_used_on_block_execution_evidence.json"
    );
    public static final Path RUN_STATE_ARTIFACT = Path.of(
        "build", "tmp", "phase_a_certification",
        "item_tag_item_used_on_block_execution_evidence.run.json"
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
    public static final String FAMILY = "ITEM_TAG_ITEM_USED_ON_BLOCK";
    public static final String SOURCE = "PhaseAItemTagItemUsedOnBlockGameTest";
    public static final String GREEN = "GREEN";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final int SCHEMA_VERSION = 1;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAItemTagItemUsedOnBlockExecutionEvidence() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
            throw new IllegalArgumentException("Expected reset or promote and optional project root path");
        }
        Path root = args.length == 2 ? Path.of(args[1]).toAbsolutePath().normalize() : projectRoot();
        System.setProperty(PROJECT_ROOT_PROPERTY, root.toString());
        if ("reset".equals(args[0])) {
            resetRun(root);
        } else if ("promote".equals(args[0])) {
            promote(root);
        } else {
            throw new IllegalArgumentException("Expected reset or promote");
        }
    }

    public static Path projectRoot() {
        String configuredRoot = System.getProperty(PROJECT_ROOT_PROPERTY);
        if (configuredRoot == null || configuredRoot.isBlank()) {
            throw new IllegalStateException("Missing system property " + PROJECT_ROOT_PROPERTY + " for GameTest runtime");
        }
        return Path.of(configuredRoot).toAbsolutePath().normalize();
    }

    public static String beginRun(Path projectRoot) throws IOException {
        RuntimeRunState runState = new RuntimeRunState(
            UUID.randomUUID().toString(),
            currentCatalogFingerprint(projectRoot),
            Instant.now().toString()
        );
        writeJson(projectRoot.resolve(RUN_STATE_ARTIFACT), runState.toJson());
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), emptyArtifact(runState).toJson());
        return runState.runId();
    }

    public static void resetRun(Path projectRoot) throws IOException {
        beginRun(projectRoot);
    }

    public static String currentRunId(Path projectRoot) throws IOException {
        return requireRunState(projectRoot).runId();
    }

    public static String currentCatalogFingerprint(Path projectRoot) throws IOException {
        try {
            byte[] bytes = Files.readAllBytes(projectRoot.resolve(CATALOG_PATH));
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest(bytes);
            StringBuilder hex = new StringBuilder(result.length * 2);
            for (byte value : result) {
                hex.append(Character.forDigit((value >>> 4) & 0xF, 16));
                hex.append(Character.forDigit(value & 0xF, 16));
            }
            return "sha-256:" + hex;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    public static void recordGreen(Path projectRoot, JsonObject receipt) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = loadArtifact(projectRoot, TEMPORARY_ARTIFACT, Mode.TEMP_DIAGNOSTIC, runState);
        String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
        CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition = findCase(key);
        JsonObject persistedReceipt = receipt.deepCopy();
        persistedReceipt.addProperty("family", FAMILY);
        persistedReceipt.addProperty("source", SOURCE);
        persistedReceipt.addProperty("catalogFingerprint", runState.catalogFingerprint());
        persistedReceipt.addProperty("runId", runState.runId());
        persistedReceipt.addProperty("minecraftVersion", MINECRAFT_VERSION);
        persistedReceipt.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        persistedReceipt.addProperty("result", GREEN);
        validateReceipt(persistedReceipt, caseDefinition, runState.runId(), runState.catalogFingerprint());
        for (JsonObject existing : artifact.entries()) {
            String existingKey = requiredNonBlank(existing, "advancementId") + "#" + requiredNonBlank(existing, "criterion");
            if (existingKey.equals(key)) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + key);
            }
        }
        List<JsonObject> entries = new ArrayList<>(artifact.entries());
        entries.add(persistedReceipt);
        RuntimeExecutionArtifact updated = new RuntimeExecutionArtifact(
            runState.catalogFingerprint(),
            runState.runId(),
            Instant.now().toString(),
            List.copyOf(entries),
            Mode.TEMP_DIAGNOSTIC
        );
        validateArtifact(updated, currentCatalogFingerprint(projectRoot), false);
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), updated.toJson());
    }

    public static RuntimeExecutionArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        return loadArtifact(projectRoot, TEMPORARY_ARTIFACT, Mode.TEMP_DIAGNOSTIC, runState);
    }

    public static RuntimeExecutionArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        return loadArtifact(projectRoot, TEMPORARY_ARTIFACT, Mode.TEMP_PROMOTABLE, runState);
    }

    /**
     * Artifact-local persistent loader.  It intentionally does not read
     * RUN_STATE_ARTIFACT, TEMPORARY_ARTIFACT, or any GameTest JVM state.
     */
    public static RuntimeExecutionArtifact loadValidatedPersistentArtifact(Path projectRoot) throws IOException {
        return loadArtifact(projectRoot, PERSISTENT_ARTIFACT, Mode.PERSISTENT, null);
    }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        RuntimeExecutionArtifact artifact = loadValidatedPersistentArtifact(projectRoot);
        Map<String, RuntimeEvidenceData> evidence = new LinkedHashMap<>();
        for (JsonObject receipt : artifact.entries()) {
            String advancementId = requiredNonBlank(receipt, "advancementId");
            String criterion = requiredNonBlank(receipt, "criterion");
            RuntimeEvidenceData data = evidence.computeIfAbsent(advancementId, RuntimeEvidenceData::empty);
            data.greenCriteria().add(criterion);
            data.criteriaByFamily().computeIfAbsent(requiredNonBlank(receipt, "family"), ignored -> new TreeSet<>()).add(criterion);
            data.criteriaBySource().computeIfAbsent(requiredNonBlank(receipt, "source"), ignored -> new TreeSet<>()).add(criterion);
            data.families().add(requiredNonBlank(receipt, "family"));
            data.sources().add(requiredNonBlank(receipt, "source"));
        }
        return evidence;
    }

    public static RuntimeExecutionArtifact promote(Path projectRoot) throws IOException {
        RuntimeExecutionArtifact artifact = loadValidatedPromotableTemporaryArtifact(projectRoot);
        Path persistentPath = projectRoot.resolve(PERSISTENT_ARTIFACT);
        if (Files.exists(persistentPath)) {
            throw new IllegalStateException("Refusing to overwrite existing persistent ITEM_TAG_ITEM_USED_ON_BLOCK evidence");
        }
        Files.createDirectories(persistentPath.getParent());
        Files.copy(projectRoot.resolve(TEMPORARY_ARTIFACT), persistentPath, StandardCopyOption.COPY_ATTRIBUTES);
        return artifact;
    }

    private static RuntimeExecutionArtifact loadArtifact(
        Path projectRoot,
        Path relativeArtifact,
        Mode mode,
        RuntimeRunState activeRun
    ) throws IOException {
        Path artifactPath = projectRoot.resolve(relativeArtifact);
        if (!Files.exists(artifactPath)) {
            throw new IllegalStateException("Missing " + relativeArtifact + " ITEM_TAG_ITEM_USED_ON_BLOCK artifact");
        }
        JsonObject root = readJson(artifactPath);
        RuntimeExecutionArtifact artifact = parseArtifact(root, mode);
        String catalogFingerprint = currentCatalogFingerprint(projectRoot);
        requireEquals(artifact.catalogFingerprint(), catalogFingerprint, "catalogFingerprint");
        if (activeRun != null) {
            requireEquals(artifact.runId(), activeRun.runId(), "runId");
            requireEquals(activeRun.catalogFingerprint(), catalogFingerprint, "catalogFingerprint");
        }
        validateArtifact(artifact, catalogFingerprint, mode != Mode.TEMP_DIAGNOSTIC);
        if (activeRun != null) {
            requireEquals(artifact.runId(), activeRun.runId(), "runId");
        }
        return artifact;
    }

    private static void validateArtifact(RuntimeExecutionArtifact artifact, String catalogFingerprint, boolean exactCoverage) {
        requireEquals(artifact.catalogFingerprint(), catalogFingerprint, "catalogFingerprint");
        requireNonBlank(artifact.runId(), "runId");
        requireNonBlank(artifact.generatedAt(), "generatedAt");
        Map<String, CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition> catalog = new LinkedHashMap<>();
        for (CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition : CertifiedItemTagItemUsedOnBlockCatalog.allCases()) {
            if (catalog.put(caseDefinition.key(), caseDefinition) != null) {
                throw new IllegalStateException("Duplicate runtime catalog key: " + caseDefinition.key());
            }
        }
        Set<String> actualKeys = new LinkedHashSet<>();
        for (JsonObject receipt : artifact.entries()) {
            String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
            if (!actualKeys.add(key)) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + key);
            }
            CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition = catalog.get(key);
            if (caseDefinition == null) {
                throw new IllegalStateException("Unknown runtime evidence receipt key: " + key);
            }
            validateReceipt(receipt, caseDefinition, artifact.runId(), catalogFingerprint);
        }
        if (exactCoverage) {
            Set<String> expectedKeys = new LinkedHashSet<>(catalog.keySet());
            if (!actualKeys.equals(expectedKeys)) {
                throw new IllegalStateException("Runtime evidence receipt keys do not exactly match the 15-key catalog");
            }
        }
    }

    private static void validateReceipt(
        JsonObject receipt,
        CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition,
        String runId,
        String catalogFingerprint
    ) {
        String key = caseDefinition.key();
        requireString(receipt, "family", FAMILY);
        requireString(receipt, "source", SOURCE);
        requireString(receipt, "catalogFingerprint", catalogFingerprint);
        requireString(receipt, "runId", runId);
        requireString(receipt, "minecraftVersion", MINECRAFT_VERSION);
        requireString(receipt, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(receipt, "result", GREEN);
        requireInt(receipt, "requirementGroupIndex", caseDefinition.requirementGroupIndex());
        requireString(receipt, "trigger", "minecraft:item_used_on_block");
        requireString(receipt, "itemTag", caseDefinition.itemTag());
        requireString(receipt, "selectedItem", caseDefinition.preferredToolItem());
        requireBoolean(receipt, "runtimeItemTagMembership", true);
        requirePositive(receipt, "runtimeItemTagMemberCount");
        requireString(receipt, "boundary", "ServerPlayerGameMode.useItemOn");
        requireString(receipt, "hand", "MAIN_HAND");
        requireString(receipt, "playerClass", "net.minecraft.server.level.ServerPlayer");
        requiredNonBlank(receipt, "playerUuid");
        requireString(receipt, "gameMode", "SURVIVAL");
        requireBoolean(receipt, "joined", true);
        requireBoolean(receipt, "connectionRegistered", true);
        requireBoolean(receipt, "clientLoaded", true);
        requireBoolean(receipt, "normalScheduler", true);
        requireBoolean(receipt, "buildPermission", true);
        requiredNonBlank(receipt, "clickedPosition");
        requiredNonBlank(receipt, "blockStateBefore");
        requiredNonBlank(receipt, "blockStateAfter");
        requireString(receipt, "blockIdBefore", caseDefinition.fixtureBlockBefore());
        requireString(receipt, "blockIdAfter", caseDefinition.expectedPostBlock());
        String interactionResult = requiredNonBlank(receipt, "interactionResult");
        if (!interactionResult.startsWith("Success[")) {
            throw new IllegalStateException("Expected a consuming 26.2 Success result for " + key);
        }
        requireBoolean(receipt, "interactionConsumesAction", true);
        requireBoolean(receipt, "semanticMutation", true);
        requireBoolean(receipt, "criterionBefore", false);
        requireBoolean(receipt, "criterionAfter", true);
        int ticksToCriterion = requiredInt(receipt, "ticksToCriterion");
        if (ticksToCriterion < 0 || ticksToCriterion > 10) {
            throw new IllegalStateException("Invalid criterion polling ticks for " + key + ": " + ticksToCriterion);
        }
        requireStateProperties(receipt, "blockStateBeforeProperties", caseDefinition.fixtureStateBefore(), key);
        requireStateProperties(receipt, "blockStateAfterProperties", caseDefinition.expectedPostState(), key);

        JsonObject preconditions = requiredObject(receipt, "productionPreconditions");
        requireString(preconditions, "scoreboardObjective", "bac_advancements");
        requireInt(preconditions, "scoreBefore", 0);
        requireInt(preconditions, "scoreAfter", 1000);
        requireBoolean(preconditions, "abilityLockedBefore", true);
        requireBoolean(preconditions, "abilityLockedAfter", false);
        requireBoolean(preconditions, "lockedLandmark", false);
        requireString(preconditions, "ability", "USE_DIAMOND_TOOLS");

        JsonObject stack = requiredObject(receipt, "interactionStack");
        requireString(stack, "itemBefore", caseDefinition.preferredToolItem());
        requireString(stack, "itemAfter", caseDefinition.preferredToolItem());
        requireInt(stack, "countBefore", 1);
        requireInt(stack, "countAfter", 1);
        int damageBefore = requiredInt(stack, "damageBefore");
        int damageAfter = requiredInt(stack, "damageAfter");
        if (damageBefore < 0 || damageAfter != damageBefore + 1) {
            throw new IllegalStateException("Expected one tool durability damage for " + key);
        }
        requireBooleanField(stack, "sameStackReference");

        JsonObject proof = requiredObject(receipt, "actionProof");
        requireString(proof, "action", caseDefinition.action());
        requireBoolean(proof, "realUseOn", true);
        requireString(proof, "triggerPath", "ServerPlayerGameMode.useItemOn->ItemUsedOnLocationTrigger");
        requireString(proof, "preBlock", caseDefinition.fixtureBlockBefore());
        requireString(proof, "postBlock", caseDefinition.expectedPostBlock());
        requireBoolean(proof, "interactionConsumesAction", true);
        requireInt(proof, "toolDamageDelta", 1);
        requireBooleanField(proof, "sameStackReference");
        requireBoolean(proof, "criterionBefore", false);
        requireBoolean(proof, "criterionAfter", true);
        switch (caseDefinition.action()) {
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
            default -> throw new IllegalStateException("Unsupported action category for " + key);
        }
    }

    private static void requireStateProperties(
        JsonObject receipt,
        String field,
        Map<String, String> expected,
        String key
    ) {
        JsonObject actual = requiredObject(receipt, field);
        if (actual.size() != expected.size()) {
            throw new IllegalStateException("State property count mismatch in " + field + " for " + key);
        }
        for (Map.Entry<String, String> property : expected.entrySet()) {
            if (!actual.has(property.getKey()) || !property.getValue().equals(actual.get(property.getKey()).getAsString())) {
                throw new IllegalStateException("State property mismatch in " + field + " for " + key);
            }
        }
    }

    private static CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition findCase(String key) {
        for (CertifiedItemTagItemUsedOnBlockCatalog.CaseDefinition caseDefinition : CertifiedItemTagItemUsedOnBlockCatalog.allCases()) {
            if (caseDefinition.key().equals(key)) {
                return caseDefinition;
            }
        }
        throw new IllegalStateException("Unknown ITEM_TAG_ITEM_USED_ON_BLOCK case: " + key);
    }

    private static RuntimeRunState requireRunState(Path projectRoot) throws IOException {
        Path path = projectRoot.resolve(RUN_STATE_ARTIFACT);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing active ITEM_TAG_ITEM_USED_ON_BLOCK run state");
        }
        JsonObject json = readJson(path);
        requireString(json, "snapshot", SNAPSHOT);
        requireInt(json, "schemaVersion", SCHEMA_VERSION);
        requireString(json, "family", FAMILY);
        requireString(json, "minecraftVersion", MINECRAFT_VERSION);
        requireString(json, "compatibilityMarker", COMPATIBILITY_MARKER);
        return new RuntimeRunState(
            requiredNonBlank(json, "runId"),
            requiredNonBlank(json, "catalogFingerprint"),
            requiredNonBlank(json, "startedAt")
        );
    }

    private static RuntimeExecutionArtifact parseArtifact(JsonObject root, Mode mode) {
        requireString(root, "snapshot", SNAPSHOT);
        requireInt(root, "schemaVersion", SCHEMA_VERSION);
        requireString(root, "family", FAMILY);
        requireString(root, "source", SOURCE);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requiredNonBlank(root, "catalogFingerprint");
        List<JsonObject> entries = new ArrayList<>();
        JsonArray entriesJson = requiredArray(root, "entries");
        for (JsonElement element : entriesJson) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("Runtime evidence entry is not an object");
            }
            entries.add(element.getAsJsonObject());
        }
        return new RuntimeExecutionArtifact(
            requiredNonBlank(root, "catalogFingerprint"),
            requiredNonBlank(root, "runId"),
            requiredNonBlank(root, "generatedAt"),
            List.copyOf(entries),
            mode
        );
    }

    private static RuntimeExecutionArtifact emptyArtifact(RuntimeRunState runState) {
        return new RuntimeExecutionArtifact(
            runState.catalogFingerprint(),
            runState.runId(),
            Instant.EPOCH.toString(),
            List.of(),
            Mode.TEMP_DIAGNOSTIC
        );
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
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

    private static void requireNonBlank(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Expected nonblank " + key);
        }
    }

    private static int requiredInt(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        return json.get(key).getAsInt();
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    private static void requirePositive(JsonObject json, String key) {
        if (requiredInt(json, key) <= 0) {
            throw new IllegalStateException("Expected " + key + ">0");
        }
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredNonBlank(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    private static void requireEquals(String actual, String expected, String field) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + field + "=" + expected + " but got " + actual);
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

    public record RuntimeExecutionArtifact(
        String catalogFingerprint,
        String runId,
        String generatedAt,
        List<JsonObject> entries,
        Mode mode
    ) {
        JsonObject toJson() {
            JsonObject root = new JsonObject();
            root.addProperty("snapshot", SNAPSHOT);
            root.addProperty("schemaVersion", SCHEMA_VERSION);
            root.addProperty("family", FAMILY);
            root.addProperty("source", SOURCE);
            root.addProperty("minecraftVersion", MINECRAFT_VERSION);
            root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
            root.addProperty("catalogFingerprint", catalogFingerprint);
            root.addProperty("runId", runId);
            root.addProperty("generatedAt", generatedAt);
            JsonArray entriesJson = new JsonArray();
            for (JsonObject entry : entries) {
                entriesJson.add(entry);
            }
            root.add("entries", entriesJson);
            return root;
        }
    }

    private record RuntimeRunState(String runId, String catalogFingerprint, String startedAt) {
        JsonObject toJson() {
            JsonObject root = new JsonObject();
            root.addProperty("snapshot", SNAPSHOT);
            root.addProperty("schemaVersion", SCHEMA_VERSION);
            root.addProperty("family", FAMILY);
            root.addProperty("minecraftVersion", MINECRAFT_VERSION);
            root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
            root.addProperty("catalogFingerprint", catalogFingerprint);
            root.addProperty("runId", runId);
            root.addProperty("startedAt", startedAt);
            return root;
        }
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
