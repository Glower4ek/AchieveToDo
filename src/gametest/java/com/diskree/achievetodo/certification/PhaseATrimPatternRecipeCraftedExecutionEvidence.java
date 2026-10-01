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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PhaseATrimPatternRecipeCraftedExecutionEvidence {
    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    public static final Path TEMPORARY_ARTIFACT = Path.of(
        "build", "reports", "phase-a-certification", "trim_pattern_recipe_crafted_execution_evidence.json"
    );
    public static final Path RUN_STATE_ARTIFACT = Path.of(
        "build", "reports", "phase-a-certification", "trim_pattern_recipe_crafted_execution_evidence.run.json"
    );
    public static final Path PERSISTENT_ARTIFACT = Path.of(
        "src", "test", "resources", "phase_a_certification", "trim_pattern_recipe_crafted_execution_evidence.json"
    );
    public static final Path CATALOG_PATH = Path.of(
        "src", "test", "resources", "phase_a_certification", "trim_pattern_recipe_crafted_case_catalog.json"
    );
    public static final String SNAPSHOT = "trim_pattern_recipe_crafted_execution_evidence";
    public static final String FAMILY = "TRIM_PATTERN_RECIPE_CRAFTED";
    public static final String SOURCE = "PhaseATrimPatternRecipeCraftedGameTest";
    public static final String GREEN = "GREEN";
    public static final String TRIGGER = "minecraft:recipe_crafted";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String BASE_ITEM = "minecraft:iron_helmet";
    public static final String ADDITION_ITEM = "minecraft:lapis_lazuli";
    public static final String PRODUCED_TRIM_MATERIAL = "minecraft:lapis";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseATrimPatternRecipeCraftedExecutionEvidence() {
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
        String configuredRoot = System.getProperty(PROJECT_ROOT_PROPERTY);
        if (configuredRoot == null || configuredRoot.isBlank()) {
            throw new IllegalStateException("Missing system property " + PROJECT_ROOT_PROPERTY + " for GameTest runtime");
        }
        return Path.of(configuredRoot).toAbsolutePath().normalize();
    }

    public static String currentCatalogFingerprint(Path projectRoot) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha-256:" + toHex(digest.digest(Files.readAllBytes(projectRoot.resolve(CATALOG_PATH))));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    public static String currentRunId(Path projectRoot) throws IOException {
        JsonObject json = readJson(projectRoot.resolve(RUN_STATE_ARTIFACT));
        requireString(json, "snapshot", SNAPSHOT);
        requireString(json, "minecraftVersion", MINECRAFT_VERSION);
        requireString(json, "compatibilityMarker", COMPATIBILITY_MARKER);
        return requiredNonBlank(json, "runId");
    }

    public static void resetRun(Path projectRoot) throws IOException {
        RuntimeRunState runState = new RuntimeRunState(
            SNAPSHOT,
            MINECRAFT_VERSION,
            COMPATIBILITY_MARKER,
            currentCatalogFingerprint(projectRoot),
            UUID.randomUUID().toString(),
            Instant.now().toString()
        );
        writeJson(projectRoot.resolve(RUN_STATE_ARTIFACT), runState.toJson());
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), emptyArtifact(runState).toJson());
    }

    public static void recordGreen(Path projectRoot, DiagnosticReceipt receipt) throws IOException {
        validateReceiptBasics(receipt);
        RuntimeRunState runState = requireRunState(projectRoot);
        String fingerprint = currentCatalogFingerprint(projectRoot);
        requireEquals(receipt.catalogFingerprint(), fingerprint, "catalogFingerprint");
        requireEquals(receipt.runId(), runState.runId(), "runId");
        CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition caseDefinition =
            CertifiedTrimPatternRecipeCraftedCatalog.requiredCase(Identifier.parse(receipt.advancementId()), receipt.criterion());
        validateReceiptAgainstCase(caseDefinition, receipt);

        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        if (artifact.containsReceiptKey(receipt.key())) {
            throw new IllegalStateException("Duplicate runtime evidence receipt attempt: " + receipt.key());
        }
        RuntimeExecutionArtifact updated = artifact.append(new RuntimeExecutionEntry(
            receipt.advancementId(),
            receipt.criterion(),
            receipt.trigger(),
            receipt.recipeId(),
            receipt.expectedTrimPattern(),
            receipt.templateItem(),
            receipt.baseItem(),
            receipt.additionItem(),
            receipt.producedItem(),
            receipt.producedTrimPattern(),
            receipt.producedTrimMaterial(),
            receipt.criterionBefore(),
            receipt.criterionAfter(),
            receipt.normalResultTaken(),
            receipt.smithingTableInteraction(),
            FAMILY,
            SOURCE,
            GREEN,
            receipt.ticksToCriterion(),
            receipt.catalogFingerprint(),
            receipt.runId()
        ));
        validateArtifact(projectRoot, runState, updated, false);
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), updated.toJson());
    }

    public static RuntimeExecutionArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        Path temporaryArtifactPath = projectRoot.resolve(TEMPORARY_ARTIFACT);
        if (!Files.exists(temporaryArtifactPath)) {
            throw new IllegalStateException("Missing temporary trim-pattern recipe-crafted artifact");
        }
        RuntimeExecutionArtifact artifact = RuntimeExecutionArtifact.fromJson(readJson(temporaryArtifactPath));
        validateArtifact(projectRoot, runState, artifact, false);
        return artifact;
    }

    public static RuntimeExecutionArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        Path temporaryArtifactPath = projectRoot.resolve(TEMPORARY_ARTIFACT);
        if (!Files.exists(temporaryArtifactPath)) {
            throw new IllegalStateException("Missing temporary trim-pattern recipe-crafted artifact");
        }
        RuntimeExecutionArtifact artifact = RuntimeExecutionArtifact.fromJson(readJson(temporaryArtifactPath));
        validateArtifact(projectRoot, runState, artifact, true);
        return artifact;
    }

    public static RuntimeExecutionArtifact promote(Path projectRoot) throws IOException {
        RuntimeExecutionArtifact artifact = loadValidatedPromotableTemporaryArtifact(projectRoot);
        Path temporaryArtifactPath = projectRoot.resolve(TEMPORARY_ARTIFACT);
        Path persistentArtifactPath = projectRoot.resolve(PERSISTENT_ARTIFACT);
        Files.createDirectories(persistentArtifactPath.getParent());
        Files.write(persistentArtifactPath, Files.readAllBytes(temporaryArtifactPath));
        return artifact;
    }

    private static void validateReceiptBasics(DiagnosticReceipt receipt) {
        requireNonBlank(receipt.advancementId(), "advancementId");
        requireNonBlank(receipt.criterion(), "criterion");
        requireEquals(receipt.trigger(), TRIGGER, "trigger");
        requireEquals(receipt.family(), FAMILY, "family");
        requireEquals(receipt.source(), SOURCE, "source");
        requireEquals(receipt.result(), GREEN, "result");
        requireNonBlank(receipt.catalogFingerprint(), "catalogFingerprint");
        requireNonBlank(receipt.runId(), "runId");
        if (receipt.criterionBefore()) {
            throw new IllegalStateException("Expected criterionBefore=false");
        }
        if (!receipt.criterionAfter()) {
            throw new IllegalStateException("Expected criterionAfter=true");
        }
        if (!receipt.normalResultTaken()) {
            throw new IllegalStateException("Expected normalResultTaken=true");
        }
        if (!receipt.smithingTableInteraction()) {
            throw new IllegalStateException("Expected smithingTableInteraction=true");
        }
        if (receipt.ticksToCriterion() <= 0) {
            throw new IllegalStateException("Expected positive ticksToCriterion");
        }
    }

    private static void validateReceiptAgainstCase(
        CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition caseDefinition,
        DiagnosticReceipt receipt
    ) {
        requireEquals(receipt.advancementId(), caseDefinition.advancementId().toString(), "advancementId");
        requireEquals(receipt.criterion(), caseDefinition.criterion(), "criterion");
        requireEquals(receipt.recipeId(), caseDefinition.expectedRecipeId(), "recipeId");
        requireEquals(receipt.expectedTrimPattern(), caseDefinition.expectedTrimPattern(), "expectedTrimPattern");
        requireEquals(receipt.templateItem(), caseDefinition.expectedTemplateItem(), "templateItem");
        requireEquals(receipt.baseItem(), BASE_ITEM, "baseItem");
        requireEquals(receipt.additionItem(), ADDITION_ITEM, "additionItem");
        requireEquals(receipt.producedItem(), BASE_ITEM, "producedItem");
        requireEquals(receipt.producedTrimPattern(), caseDefinition.expectedTrimPattern(), "producedTrimPattern");
        requireEquals(receipt.producedTrimMaterial(), PRODUCED_TRIM_MATERIAL, "producedTrimMaterial");
        requireEquals(caseDefinition.automationEligibility(), CertifiedTrimPatternRecipeCraftedCatalog.AUTOMATION_SUPPORTED, "automationEligibility");
    }

    private static RuntimeRunState requireRunState(Path projectRoot) throws IOException {
        Path path = projectRoot.resolve(RUN_STATE_ARTIFACT);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Missing active trim-pattern recipe-crafted runtime RUN_STATE artifact");
        }
        JsonObject json = readJson(path);
        requireString(json, "snapshot", SNAPSHOT);
        requireString(json, "minecraftVersion", MINECRAFT_VERSION);
        requireString(json, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(json, "catalogFingerprint", currentCatalogFingerprint(projectRoot));
        return RuntimeRunState.fromJson(json);
    }

    private static RuntimeExecutionArtifact loadTemporaryArtifact(Path projectRoot, RuntimeRunState runState) throws IOException {
        Path path = projectRoot.resolve(TEMPORARY_ARTIFACT);
        if (!Files.exists(path)) {
            return emptyArtifact(runState);
        }
        RuntimeExecutionArtifact artifact = RuntimeExecutionArtifact.fromJson(readJson(path));
        validateArtifact(projectRoot, runState, artifact, false);
        return artifact;
    }

    private static void validateArtifact(
        Path projectRoot,
        RuntimeRunState runState,
        RuntimeExecutionArtifact artifact,
        boolean requireExactCoverage
    ) throws IOException {
        requireEquals(artifact.snapshot(), SNAPSHOT, "snapshot");
        requireEquals(artifact.minecraftVersion(), MINECRAFT_VERSION, "minecraftVersion");
        requireEquals(artifact.compatibilityMarker(), COMPATIBILITY_MARKER, "compatibilityMarker");
        requireEquals(artifact.catalogFingerprint(), currentCatalogFingerprint(projectRoot), "catalogFingerprint");
        requireEquals(artifact.runId(), runState.runId(), "runId");
        requireNonBlank(artifact.generatedAt(), "generatedAt");
        Set<String> actualKeys = new LinkedHashSet<>();
        Set<String> expectedKeys = new LinkedHashSet<>();
        for (CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition caseDefinition :
            CertifiedTrimPatternRecipeCraftedCatalog.allCases()) {
            expectedKeys.add(key(caseDefinition.advancementId().toString(), caseDefinition.criterion()));
        }
        for (RuntimeExecutionEntry entry : artifact.entries()) {
            String entryKey = key(entry.advancementId(), entry.criterion());
            if (!actualKeys.add(entryKey)) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + entryKey);
            }
            CertifiedTrimPatternRecipeCraftedCatalog.CaseDefinition caseDefinition =
                CertifiedTrimPatternRecipeCraftedCatalog.requiredCase(Identifier.parse(entry.advancementId()), entry.criterion());
            validateReceiptBasics(entry.asReceipt());
            validateReceiptAgainstCase(caseDefinition, entry.asReceipt());
            requireEquals(entry.family(), FAMILY, "family");
            requireEquals(entry.source(), SOURCE, "source");
            requireEquals(entry.result(), GREEN, "result");
            requireEquals(entry.catalogFingerprint(), artifact.catalogFingerprint(), "entry catalogFingerprint");
            requireEquals(entry.runId(), artifact.runId(), "entry runId");
        }
        if (requireExactCoverage && !actualKeys.equals(expectedKeys)) {
            throw new IllegalStateException("Runtime evidence receipt keys do not exactly match exact18 catalog coverage");
        }
    }

    private static RuntimeExecutionArtifact emptyArtifact(RuntimeRunState runState) {
        return new RuntimeExecutionArtifact(
            SNAPSHOT,
            MINECRAFT_VERSION,
            COMPATIBILITY_MARKER,
            runState.catalogFingerprint(),
            runState.runId(),
            Instant.EPOCH.toString(),
            List.of()
        );
    }

    private static JsonObject readJson(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static String requiredNonBlank(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalStateException("Missing string " + key);
        }
        String value = json.get(key).getAsString();
        requireNonBlank(value, key);
        return value;
    }

    private static void requireString(JsonObject json, String key, String expected) {
        requireEquals(requiredNonBlank(json, key), expected, key);
    }

    private static void requireNonBlank(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Expected nonblank " + key);
        }
    }

    private static void requireEquals(String actual, String expected, String key) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " but got " + actual);
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

    public record DiagnosticReceipt(
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
        String key() {
            return advancementId + "#" + criterion;
        }
    }

    public record RuntimeRunState(
        String snapshot,
        String minecraftVersion,
        String compatibilityMarker,
        String catalogFingerprint,
        String runId,
        String generatedAt
    ) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("snapshot", snapshot);
            json.addProperty("minecraftVersion", minecraftVersion);
            json.addProperty("compatibilityMarker", compatibilityMarker);
            json.addProperty("catalogFingerprint", catalogFingerprint);
            json.addProperty("runId", runId);
            json.addProperty("generatedAt", generatedAt);
            return json;
        }

        static RuntimeRunState fromJson(JsonObject json) {
            return new RuntimeRunState(
                requiredNonBlank(json, "snapshot"),
                requiredNonBlank(json, "minecraftVersion"),
                requiredNonBlank(json, "compatibilityMarker"),
                requiredNonBlank(json, "catalogFingerprint"),
                requiredNonBlank(json, "runId"),
                requiredNonBlank(json, "generatedAt")
            );
        }
    }

    public record RuntimeExecutionArtifact(
        String snapshot,
        String minecraftVersion,
        String compatibilityMarker,
        String catalogFingerprint,
        String runId,
        String generatedAt,
        List<RuntimeExecutionEntry> entries
    ) {
        boolean containsReceiptKey(String receiptKey) {
            return entries.stream().anyMatch(entry -> key(entry.advancementId(), entry.criterion()).equals(receiptKey));
        }

        RuntimeExecutionArtifact append(RuntimeExecutionEntry entry) {
            List<RuntimeExecutionEntry> updated = new ArrayList<>(entries);
            updated.add(entry);
            updated.sort(Comparator.comparing(RuntimeExecutionEntry::advancementId).thenComparing(RuntimeExecutionEntry::criterion));
            return new RuntimeExecutionArtifact(
                snapshot,
                minecraftVersion,
                compatibilityMarker,
                catalogFingerprint,
                runId,
                Instant.now().toString(),
                List.copyOf(updated)
            );
        }

        static RuntimeExecutionArtifact fromJson(JsonObject json) {
            List<RuntimeExecutionEntry> entries = new ArrayList<>();
            JsonArray entriesJson = json.getAsJsonArray("entries");
            if (entriesJson != null) {
                for (JsonElement element : entriesJson) {
                    entries.add(RuntimeExecutionEntry.fromJson(element.getAsJsonObject()));
                }
            }
            return new RuntimeExecutionArtifact(
                requiredNonBlank(json, "snapshot"),
                requiredNonBlank(json, "minecraftVersion"),
                requiredNonBlank(json, "compatibilityMarker"),
                requiredNonBlank(json, "catalogFingerprint"),
                requiredNonBlank(json, "runId"),
                requiredNonBlank(json, "generatedAt"),
                List.copyOf(entries)
            );
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("snapshot", snapshot);
            json.addProperty("minecraftVersion", minecraftVersion);
            json.addProperty("compatibilityMarker", compatibilityMarker);
            json.addProperty("catalogFingerprint", catalogFingerprint);
            json.addProperty("runId", runId);
            json.addProperty("generatedAt", generatedAt);
            JsonArray entriesJson = new JsonArray();
            for (RuntimeExecutionEntry entry : entries) {
                entriesJson.add(entry.toJson());
            }
            json.add("entries", entriesJson);
            return json;
        }
    }

    public record RuntimeExecutionEntry(
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
        private static RuntimeExecutionEntry fromJson(JsonObject json) {
            return new RuntimeExecutionEntry(
                requiredNonBlank(json, "advancementId"),
                requiredNonBlank(json, "criterion"),
                requiredNonBlank(json, "trigger"),
                requiredNonBlank(json, "recipeId"),
                requiredNonBlank(json, "expectedTrimPattern"),
                requiredNonBlank(json, "templateItem"),
                requiredNonBlank(json, "baseItem"),
                requiredNonBlank(json, "additionItem"),
                requiredNonBlank(json, "producedItem"),
                requiredNonBlank(json, "producedTrimPattern"),
                requiredNonBlank(json, "producedTrimMaterial"),
                json.get("criterionBefore").getAsBoolean(),
                json.get("criterionAfter").getAsBoolean(),
                json.get("normalResultTaken").getAsBoolean(),
                json.get("smithingTableInteraction").getAsBoolean(),
                requiredNonBlank(json, "family"),
                requiredNonBlank(json, "source"),
                requiredNonBlank(json, "result"),
                json.get("ticksToCriterion").getAsInt(),
                requiredNonBlank(json, "catalogFingerprint"),
                requiredNonBlank(json, "runId")
            );
        }

        private JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("advancementId", advancementId);
            json.addProperty("criterion", criterion);
            json.addProperty("trigger", trigger);
            json.addProperty("recipeId", recipeId);
            json.addProperty("expectedTrimPattern", expectedTrimPattern);
            json.addProperty("templateItem", templateItem);
            json.addProperty("baseItem", baseItem);
            json.addProperty("additionItem", additionItem);
            json.addProperty("producedItem", producedItem);
            json.addProperty("producedTrimPattern", producedTrimPattern);
            json.addProperty("producedTrimMaterial", producedTrimMaterial);
            json.addProperty("criterionBefore", criterionBefore);
            json.addProperty("criterionAfter", criterionAfter);
            json.addProperty("normalResultTaken", normalResultTaken);
            json.addProperty("smithingTableInteraction", smithingTableInteraction);
            json.addProperty("family", family);
            json.addProperty("source", source);
            json.addProperty("result", result);
            json.addProperty("ticksToCriterion", ticksToCriterion);
            json.addProperty("catalogFingerprint", catalogFingerprint);
            json.addProperty("runId", runId);
            return json;
        }

        private DiagnosticReceipt asReceipt() {
            return new DiagnosticReceipt(
                advancementId,
                criterion,
                trigger,
                recipeId,
                expectedTrimPattern,
                templateItem,
                baseItem,
                additionItem,
                producedItem,
                producedTrimPattern,
                producedTrimMaterial,
                criterionBefore,
                criterionAfter,
                normalResultTaken,
                smithingTableInteraction,
                family,
                source,
                result,
                ticksToCriterion,
                catalogFingerprint,
                runId
            );
        }
    }
}
