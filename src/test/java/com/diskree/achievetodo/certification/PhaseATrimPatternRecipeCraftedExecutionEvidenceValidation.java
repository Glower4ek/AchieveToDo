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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation {
    static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "reports", "phase-a-certification", "trim_pattern_recipe_crafted_execution_evidence.json"
    );
    static final Path RUN_STATE_ARTIFACT = Path.of(
        "build", "reports", "phase-a-certification", "trim_pattern_recipe_crafted_execution_evidence.run.json"
    );
    static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification", "trim_pattern_recipe_crafted_execution_evidence.json"
    );
    static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification", "trim_pattern_recipe_crafted_case_catalog.json"
    );
    static final String SNAPSHOT = "trim_pattern_recipe_crafted_execution_evidence";
    static final String CATALOG_SNAPSHOT = "trim_pattern_recipe_crafted_case_catalog";
    static final String MINECRAFT_VERSION = "26.2";
    static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    static final String FAMILY = "TRIM_PATTERN_RECIPE_CRAFTED";
    static final String SOURCE = "PhaseATrimPatternRecipeCraftedGameTest";
    private static final String ADVANCEMENT_ID = "minecraft:adventure/trim_with_any_armor_pattern";
    private static final String SOURCE_PATH = "data/minecraft/advancement/adventure/trim_with_any_armor_pattern.json";
    private static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static final String BASE_ITEM = "minecraft:iron_helmet";
    private static final String ADDITION_ITEM = "minecraft:lapis_lazuli";
    private static final String PRODUCED_TRIM_MATERIAL = "minecraft:lapis";
    static final String GREEN = "GREEN";
    static final String TRIGGER = "minecraft:recipe_crafted";
    private static final List<String> EXPECTED_CRITERIA = List.of(
        "coast_armor_trim", "dune_armor_trim", "eye_armor_trim", "host_armor_trim", "raiser_armor_trim",
        "rib_armor_trim", "sentry_armor_trim", "shaper_armor_trim", "silence_armor_trim", "snout_armor_trim",
        "spire_armor_trim", "tide_armor_trim", "vex_armor_trim", "ward_armor_trim", "wayfinder_armor_trim",
        "wild_armor_trim", "bolt_armor_trim", "flow_armor_trim"
    );

    private PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation() {
    }

    static RuntimeArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, false, true);
    }

    static RuntimeArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot) throws IOException {
        return loadValidatedArtifact(projectRoot, TEMPORARY_ARTIFACT, true, true);
    }

    static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path projectRoot) throws IOException {
        RuntimeArtifact artifact = loadValidatedArtifact(projectRoot, PERSISTENT_ARTIFACT, true, false);
        Map<String, RuntimeEvidenceData> evidence = new LinkedHashMap<>();
        for (RuntimeReceipt receipt : artifact.receipts()) {
            RuntimeEvidenceData runtimeEvidence = evidence.computeIfAbsent(
                receipt.advancementId(),
                RuntimeEvidenceData::empty
            );
            runtimeEvidence.greenCriteria().add(receipt.criterion());
            runtimeEvidence.criteriaByFamily().computeIfAbsent(receipt.family(), ignored -> new TreeSet<>()).add(receipt.criterion());
            runtimeEvidence.criteriaBySource().computeIfAbsent(receipt.source(), ignored -> new TreeSet<>()).add(receipt.criterion());
            runtimeEvidence.families().add(receipt.family());
            runtimeEvidence.sources().add(receipt.source());
        }
        return evidence;
    }

    static Map<String, CatalogCase> loadCatalog(Path catalogPath) throws IOException {
        JsonObject root = readJson(catalogPath);
        requireString(root, "snapshot", CATALOG_SNAPSHOT);
        requireInt(root, "canonicalAdvancementCount", 1152);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "canonicalSemanticsSource", "reference/phase_a_preservation/files/final/bacap.zip");
        JsonObject summary = requiredObject(root, "summary");
        requireInt(summary, "totalCases", 18);
        requireInt(summary, "uniqueAdvancements", 1);
        requireInt(summary, "requirementGroups", 1);
        requireInt(summary, "automationSupported", 18);
        requireInt(summary, "automationDeferred", 0);
        JsonArray casesJson = requiredArray(root, "cases");
        if (casesJson.size() != EXPECTED_CRITERIA.size()) {
            throw new IllegalStateException("Expected exact18 trim-pattern catalog");
        }
        Map<String, CatalogCase> catalog = new LinkedHashMap<>();
        for (int index = 0; index < casesJson.size(); index++) {
            JsonObject caseJson = casesJson.get(index).getAsJsonObject();
            requireExactCaseKeys(caseJson);
            CatalogCase caseDefinition = new CatalogCase(
                requiredString(caseJson, "advancementId"),
                requiredString(caseJson, "sourcePath"),
                requiredString(caseJson, "criterion"),
                requiredString(caseJson, "trigger"),
                requiredString(caseJson, "expectedRecipeId"),
                requiredString(caseJson, "expectedTrimPattern"),
                requiredString(caseJson, "expectedTemplateItem"),
                requiredInt(caseJson, "requirementGroup"),
                requiredString(caseJson, "automationEligibility")
            );
            String key = key(caseDefinition.advancementId(), caseDefinition.criterion());
            if (catalog.putIfAbsent(key, caseDefinition) != null) {
                throw new IllegalStateException("Duplicate catalog case " + key);
            }
            if (!ADVANCEMENT_ID.equals(caseDefinition.advancementId())
                || !SOURCE_PATH.equals(caseDefinition.sourcePath())
                || !EXPECTED_CRITERIA.get(index).equals(caseDefinition.criterion())
                || !TRIGGER.equals(caseDefinition.trigger())
                || caseDefinition.requirementGroup() != 0
                || !AUTOMATION_SUPPORTED.equals(caseDefinition.automationEligibility())) {
                throw new IllegalStateException("Catalog semantic drift for " + key);
            }
            String pattern = caseDefinition.criterion().substring(0, caseDefinition.criterion().length() - "_armor_trim".length());
            requireEquals(caseDefinition.expectedRecipeId(), "minecraft:" + caseDefinition.criterion() + "_smithing_template_smithing_trim", "recipeId");
            requireEquals(caseDefinition.expectedTrimPattern(), "minecraft:" + pattern, "trim pattern");
            requireEquals(caseDefinition.expectedTemplateItem(), "minecraft:" + caseDefinition.criterion() + "_smithing_template", "template item");
        }
        if (!new HashSet<>(catalog.keySet()).equals(new HashSet<>(EXPECTED_CRITERIA.stream()
            .map(criterion -> ADVANCEMENT_ID + "#" + criterion).toList()))) {
            throw new IllegalStateException("Catalog does not contain the exact18 key set");
        }
        return catalog;
    }

    static String fingerprint(Path catalogPath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha-256:" + toHex(digest.digest(Files.readAllBytes(catalogPath)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    private static RuntimeArtifact loadValidatedArtifact(
        Path projectRoot,
        Path relativeArtifactPath,
        boolean requireExactCoverage,
        boolean requireActiveRunStateAlignment
    ) throws IOException {
        Path artifactPath = projectRoot.resolve(relativeArtifactPath);
        if (!Files.exists(artifactPath)) {
            throw new IllegalStateException("Missing " + relativeArtifactPath + " trim-pattern recipe-crafted artifact");
        }
        Path catalogPath = projectRoot.resolve(CATALOG_PATH);
        Map<String, CatalogCase> catalog = loadCatalog(catalogPath);
        String catalogFingerprint = fingerprint(catalogPath);
        JsonObject root = readJson(artifactPath);
        requireString(root, "snapshot", SNAPSHOT);
        requireString(root, "minecraftVersion", MINECRAFT_VERSION);
        requireString(root, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(root, "catalogFingerprint", catalogFingerprint);
        String runId = requiredString(root, "runId");
        requiredString(root, "generatedAt");
        if (requireActiveRunStateAlignment) {
            Path runStatePath = projectRoot.resolve(RUN_STATE_ARTIFACT);
            if (!Files.exists(runStatePath)) {
                throw new IllegalStateException("Missing active trim-pattern recipe-crafted runtime RUN_STATE artifact");
            }
            JsonObject runState = readJson(runStatePath);
            requireString(runState, "snapshot", SNAPSHOT);
            requireString(runState, "minecraftVersion", MINECRAFT_VERSION);
            requireString(runState, "compatibilityMarker", COMPATIBILITY_MARKER);
            requireString(runState, "catalogFingerprint", catalogFingerprint);
            requiredString(runState, "generatedAt");
            requireEquals(runId, requiredString(runState, "runId"), "runId");
        }
        JsonArray entriesJson = requiredArray(root, "entries");
        Set<String> actualKeys = new LinkedHashSet<>();
        List<RuntimeReceipt> receipts = new ArrayList<>();
        for (JsonElement element : entriesJson) {
            RuntimeReceipt receipt = RuntimeReceipt.fromJson(element.getAsJsonObject());
            String receiptKey = key(receipt.advancementId(), receipt.criterion());
            if (!actualKeys.add(receiptKey)) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + receiptKey);
            }
            CatalogCase caseDefinition = catalog.get(receiptKey);
            if (caseDefinition == null) {
                throw new IllegalStateException("Runtime evidence references unknown catalog case: " + receiptKey);
            }
            validateReceipt(receipt, caseDefinition, catalogFingerprint, runId);
            receipts.add(receipt);
        }
        if (requireExactCoverage && !actualKeys.equals(catalog.keySet())) {
            throw new IllegalStateException("Exact18 runtime evidence must cover every catalog case");
        }
        return new RuntimeArtifact(runId, requiredString(root, "generatedAt"), receipts);
    }

    private static void validateReceipt(
        RuntimeReceipt receipt,
        CatalogCase caseDefinition,
        String catalogFingerprint,
        String runId
    ) {
        requireEquals(receipt.advancementId(), caseDefinition.advancementId(), "advancementId");
        requireEquals(receipt.criterion(), caseDefinition.criterion(), "criterion");
        requireEquals(receipt.trigger(), TRIGGER, "trigger");
        requireEquals(receipt.recipeId(), caseDefinition.expectedRecipeId(), "recipeId");
        requireEquals(receipt.expectedTrimPattern(), caseDefinition.expectedTrimPattern(), "expectedTrimPattern");
        requireEquals(receipt.templateItem(), caseDefinition.expectedTemplateItem(), "templateItem");
        requireEquals(receipt.baseItem(), BASE_ITEM, "baseItem");
        requireEquals(receipt.additionItem(), ADDITION_ITEM, "additionItem");
        requireEquals(receipt.producedItem(), BASE_ITEM, "producedItem");
        requireEquals(receipt.producedTrimPattern(), caseDefinition.expectedTrimPattern(), "producedTrimPattern");
        requireEquals(receipt.producedTrimMaterial(), PRODUCED_TRIM_MATERIAL, "producedTrimMaterial");
        if (receipt.criterionBefore() || !receipt.criterionAfter() || !receipt.normalResultTaken() || !receipt.smithingTableInteraction()) {
            throw new IllegalStateException("Runtime receipt did not prove the required false-to-true normal-click boundary");
        }
        requireEquals(receipt.family(), FAMILY, "family");
        requireEquals(receipt.source(), SOURCE, "source");
        requireEquals(receipt.result(), GREEN, "result");
        if (receipt.ticksToCriterion() <= 0) {
            throw new IllegalStateException("Expected positive ticksToCriterion");
        }
        requireEquals(receipt.catalogFingerprint(), catalogFingerprint, "entry catalogFingerprint");
        requireEquals(receipt.runId(), runId, "entry runId");
    }

    private static void requireExactCaseKeys(JsonObject json) {
        Set<String> actual = new TreeSet<>(json.keySet());
        Set<String> expected = Set.of(
            "advancementId", "sourcePath", "criterion", "trigger", "expectedRecipeId",
            "expectedTrimPattern", "expectedTemplateItem", "requirementGroup", "automationEligibility"
        );
        if (!actual.equals(expected)) {
            throw new IllegalStateException("Unexpected catalog case fields: " + actual);
        }
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonObject requiredObject(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing object " + key);
        }
        return json.getAsJsonObject(key);
    }

    private static JsonArray requiredArray(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            throw new IllegalStateException("Missing array " + key);
        }
        return json.getAsJsonArray(key);
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing string " + key);
        }
        String value = json.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Blank string " + key);
        }
        return value;
    }

    private static int requiredInt(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive() || !json.get(key).getAsJsonPrimitive().isNumber()) {
            throw new IllegalStateException("Missing integer " + key);
        }
        return json.get(key).getAsInt();
    }

    private static void requireString(JsonObject json, String key, String expected) {
        requireEquals(requiredString(json, key), expected, key);
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        if (requiredInt(json, key) != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected);
        }
    }

    private static void requireEquals(String actual, String expected, String field) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + field + "=" + expected + " but got " + actual);
        }
    }

    private static String key(String advancementId, String criterion) {
        return advancementId + "#" + criterion;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            int b = value & 0xFF;
            builder.append(Character.forDigit((b >>> 4) & 0xF, 16));
            builder.append(Character.forDigit(b & 0xF, 16));
        }
        return builder.toString();
    }

    record CatalogCase(
        String advancementId,
        String sourcePath,
        String criterion,
        String trigger,
        String expectedRecipeId,
        String expectedTrimPattern,
        String expectedTemplateItem,
        int requirementGroup,
        String automationEligibility
    ) {
    }

    record RuntimeReceipt(
        String advancementId,
        String criterion,
        String trigger,
        String recipeId,
        String expectedTrimPattern,
        String templateItem,
        String baseItem,
        String additionItem,
        String producedItem,
        String producedTrimPattern,
        String producedTrimMaterial,
        boolean criterionBefore,
        boolean criterionAfter,
        boolean normalResultTaken,
        boolean smithingTableInteraction,
        String family,
        String source,
        String result,
        int ticksToCriterion,
        String catalogFingerprint,
        String runId
    ) {
        static RuntimeReceipt fromJson(JsonObject json) {
            return new RuntimeReceipt(
                requiredString(json, "advancementId"),
                requiredString(json, "criterion"),
                requiredString(json, "trigger"),
                requiredString(json, "recipeId"),
                requiredString(json, "expectedTrimPattern"),
                requiredString(json, "templateItem"),
                requiredString(json, "baseItem"),
                requiredString(json, "additionItem"),
                requiredString(json, "producedItem"),
                requiredString(json, "producedTrimPattern"),
                requiredString(json, "producedTrimMaterial"),
                requiredBoolean(json, "criterionBefore"),
                requiredBoolean(json, "criterionAfter"),
                requiredBoolean(json, "normalResultTaken"),
                requiredBoolean(json, "smithingTableInteraction"),
                requiredString(json, "family"),
                requiredString(json, "source"),
                requiredString(json, "result"),
                requiredInt(json, "ticksToCriterion"),
                requiredString(json, "catalogFingerprint"),
                requiredString(json, "runId")
            );
        }
    }

    record RuntimeArtifact(String runId, String generatedAt, List<RuntimeReceipt> receipts) {
    }

    record RuntimeEvidenceData(
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

    private static boolean requiredBoolean(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing boolean " + key);
        }
        return json.get(key).getAsBoolean();
    }
}
