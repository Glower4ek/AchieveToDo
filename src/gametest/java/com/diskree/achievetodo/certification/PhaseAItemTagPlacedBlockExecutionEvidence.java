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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class PhaseAItemTagPlacedBlockExecutionEvidence {
    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    public static final Path TEMPORARY_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "item_tag_placed_block_execution_evidence.json");
    public static final Path RUN_STATE_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "item_tag_placed_block_execution_evidence.run.json");
    public static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_placed_block_execution_evidence.json");
    public static final String SNAPSHOT = "phase_a_item_tag_placed_block_execution_evidence";
    public static final String CATALOG_SNAPSHOT = "phase_a_item_tag_placed_block_case_catalog";
    public static final String FAMILY = "ITEM_TAG_PLACED_BLOCK";
    public static final String GREEN = "GREEN";
    public static final String SOURCE = "PhaseAItemTagPlacedBlockGameTest";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final Path CATALOG_PATH = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_placed_block_case_catalog.json");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAItemTagPlacedBlockExecutionEvidence() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = args.length > 1 ? Path.of(args[1]).toAbsolutePath().normalize() : projectRoot();
        System.setProperty(PROJECT_ROOT_PROPERTY, projectRoot.toString());
        if (args.length > 0 && "promote".equals(args[0])) {
            promote(projectRoot);
            return;
        }
        if (args.length > 0 && "reset".equals(args[0])) {
            resetRun(projectRoot);
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

    public static void recordGreen(Path projectRoot, RuntimeExecutionEntry entry) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        validateRunAlignment(runState, artifact, currentCatalogFingerprint(projectRoot));
        validateEntry(projectRoot, entry);

        List<RuntimeExecutionEntry> updatedEntries = new ArrayList<>(artifact.entries());
        String key = key(entry.advancementId(), entry.criterion());
        for (RuntimeExecutionEntry existingEntry : updatedEntries) {
            if (key(existingEntry.advancementId(), existingEntry.criterion()).equals(key)) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + key);
            }
        }
        updatedEntries.add(entry);
        updatedEntries.sort(Comparator.comparing(RuntimeExecutionEntry::advancementId).thenComparing(RuntimeExecutionEntry::criterion));
        RuntimeExecutionArtifact updatedArtifact = new RuntimeExecutionArtifact(
            SNAPSHOT,
            MINECRAFT_VERSION,
            COMPATIBILITY_MARKER,
            runState.catalogFingerprint(),
            runState.runId(),
            Instant.now().toString(),
            List.copyOf(updatedEntries)
        );
        validateArtifact(projectRoot, runState, updatedArtifact, false);
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), updatedArtifact.toJson());
    }

    public static RuntimeExecutionArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        validateArtifact(projectRoot, runState, artifact, false);
        return artifact;
    }

    public static RuntimeExecutionArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        validateArtifact(projectRoot, runState, artifact, true);
        return artifact;
    }

    public static RuntimeExecutionArtifact promote(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        Path temporaryArtifactPath = projectRoot.resolve(TEMPORARY_ARTIFACT);
        if (!Files.exists(temporaryArtifactPath)) {
            throw new IllegalStateException("Missing temporary ITEM_TAG_PLACED_BLOCK artifact");
        }
        RuntimeExecutionArtifact artifact = parseArtifact(readJson(temporaryArtifactPath));
        validateArtifact(projectRoot, runState, artifact, true);
        Path persistentArtifactPath = projectRoot.resolve(PERSISTENT_ARTIFACT);
        Files.createDirectories(persistentArtifactPath.getParent());
        Files.write(persistentArtifactPath, Files.readAllBytes(temporaryArtifactPath));
        return artifact;
    }

    public static String currentCatalogFingerprint(Path projectRoot) throws IOException {
        byte[] bytes = Files.readAllBytes(projectRoot.resolve(CATALOG_PATH));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha-256:" + toHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    private static void validateEntry(Path projectRoot, RuntimeExecutionEntry entry) throws IOException {
        requireEquals(entry.family(), FAMILY, "family");
        requireEquals(entry.source(), SOURCE, "source");
        requireEquals(entry.result(), GREEN, "result");
        requireNonBlank(entry.advancementId(), "advancementId");
        requireNonBlank(entry.criterion(), "criterion");
        requireNonBlank(entry.itemTag(), "itemTag");
        requireNonBlank(entry.selectedItem(), "selectedItem");
        requireNonBlank(entry.placedBlock(), "placedBlock");
        requireNonBlank(entry.interactionResult(), "interactionResult");
        requireNonBlank(entry.targetPos(), "targetPos");
        requireNonBlank(entry.supportPos(), "supportPos");
        if (entry.criterionBefore()) {
            throw new IllegalStateException("Expected criterionBefore=false for " + key(entry.advancementId(), entry.criterion()));
        }
        if (!entry.criterionAfter()) {
            throw new IllegalStateException("Expected criterionAfter=true for " + key(entry.advancementId(), entry.criterion()));
        }
        if (entry.handCountBefore() != 1) {
            throw new IllegalStateException("Expected handCountBefore=1 for " + key(entry.advancementId(), entry.criterion()));
        }
        if (entry.handCountAfter() != 0) {
            throw new IllegalStateException("Expected handCountAfter=0 for " + key(entry.advancementId(), entry.criterion()));
        }
        if (!entry.tagMembership()) {
            throw new IllegalStateException("Expected tagMembership=true for " + key(entry.advancementId(), entry.criterion()));
        }
        if (entry.tagMemberCount() <= 0) {
            throw new IllegalStateException("Expected tagMemberCount>0 for " + key(entry.advancementId(), entry.criterion()));
        }
        if (entry.ticksToCriterion() < 0) {
            throw new IllegalStateException("Expected ticksToCriterion>=0 for " + key(entry.advancementId(), entry.criterion()));
        }
        requireEquals(entry.placedBlockBefore(), "minecraft:air", "placedBlockBefore");
        requireNonBlank(entry.placedBlockAfter(), "placedBlockAfter");
        if ("minecraft:air".equals(entry.placedBlockAfter())) {
            throw new IllegalStateException("Expected placedBlockAfter!=minecraft:air for " + key(entry.advancementId(), entry.criterion()));
        }
        requireEquals(entry.placedBlockAfter(), entry.placedBlock(), "placedBlockAfter");
        CertifiedItemTagPlacedBlockCatalog.CaseDefinition caseDefinition =
            CertifiedItemTagPlacedBlockCatalog.requiredCase(net.minecraft.resources.Identifier.parse(entry.advancementId()), entry.criterion());
        requireEquals(caseDefinition.itemTag(), entry.itemTag(), "itemTag");
        requireEquals(currentCatalogFingerprint(projectRoot), entry.catalogFingerprint(), "catalogFingerprint");
        requireEquals(MINECRAFT_VERSION, entry.minecraftVersion(), "minecraftVersion");
        requireEquals(COMPATIBILITY_MARKER, entry.compatibilityMarker(), "compatibilityMarker");
    }

    private static void validateRunAlignment(RuntimeRunState runState, RuntimeExecutionArtifact artifact, String currentFingerprint) {
        requireEquals(runState.snapshot(), SNAPSHOT, "snapshot");
        requireEquals(runState.minecraftVersion(), MINECRAFT_VERSION, "minecraftVersion");
        requireEquals(runState.compatibilityMarker(), COMPATIBILITY_MARKER, "compatibilityMarker");
        requireEquals(runState.catalogFingerprint(), currentFingerprint, "catalogFingerprint");
        requireEquals(artifact.snapshot(), SNAPSHOT, "snapshot");
        requireEquals(artifact.minecraftVersion(), MINECRAFT_VERSION, "minecraftVersion");
        requireEquals(artifact.compatibilityMarker(), COMPATIBILITY_MARKER, "compatibilityMarker");
        requireEquals(artifact.catalogFingerprint(), currentFingerprint, "catalogFingerprint");
        requireEquals(artifact.runId(), runState.runId(), "runId");
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
        requireEquals(artifact.catalogFingerprint(), runState.catalogFingerprint(), "catalogFingerprint");
        requireEquals(artifact.runId(), runState.runId(), "runId");
        requireNonBlank(artifact.runId(), "runId");
        requireNonBlank(artifact.generatedAt(), "generatedAt");
        Set<String> actualKeys = new LinkedHashSet<>();
        Set<String> expectedKeys = new LinkedHashSet<>();
        for (CertifiedItemTagPlacedBlockCatalog.CaseDefinition caseDefinition : CertifiedItemTagPlacedBlockCatalog.allCases()) {
            expectedKeys.add(key(caseDefinition.advancementId().toString(), caseDefinition.criterion()));
        }
        for (RuntimeExecutionEntry entry : artifact.entries()) {
            validateEntry(projectRoot, entry);
            requireEquals(entry.runId(), runState.runId(), "runId");
            if (!actualKeys.add(key(entry.advancementId(), entry.criterion()))) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + key(entry.advancementId(), entry.criterion()));
            }
        }
        if (requireExactCoverage && !actualKeys.equals(expectedKeys)) {
            throw new IllegalStateException("Runtime evidence receipt keys do not exactly match current SUPPORTED catalog keys");
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

    private static RuntimeRunState requireRunState(Path projectRoot) throws IOException {
        Path runStatePath = projectRoot.resolve(RUN_STATE_ARTIFACT);
        if (!Files.exists(runStatePath)) {
            throw new IllegalStateException("Missing active item-tag placed_block runtime RUN_STATE artifact");
        }
        JsonObject json = readJson(runStatePath);
        requireString(json, "snapshot", SNAPSHOT);
        requireString(json, "minecraftVersion", MINECRAFT_VERSION);
        requireString(json, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(json, "catalogFingerprint", currentCatalogFingerprint(projectRoot));
        return RuntimeRunState.fromJson(json);
    }

    private static RuntimeExecutionArtifact loadTemporaryArtifact(Path projectRoot, RuntimeRunState runState) throws IOException {
        Path artifactPath = projectRoot.resolve(TEMPORARY_ARTIFACT);
        if (!Files.exists(artifactPath)) {
            return emptyArtifact(runState);
        }
        RuntimeExecutionArtifact artifact = parseArtifact(readJson(artifactPath));
        validateArtifact(projectRoot, runState, artifact, false);
        return artifact;
    }

    private static RuntimeExecutionArtifact parseArtifact(JsonObject json) {
        List<RuntimeExecutionEntry> entries = new ArrayList<>();
        JsonArray entriesJson = json.getAsJsonArray("entries");
        if (entriesJson != null) {
            for (JsonElement entryElement : entriesJson) {
                JsonObject entryJson = entryElement.getAsJsonObject();
                entries.add(new RuntimeExecutionEntry(
                    requiredString(entryJson, "advancementId"),
                    requiredString(entryJson, "criterion"),
                    requiredString(entryJson, "itemTag"),
                    requiredString(entryJson, "selectedItem"),
                    requiredString(entryJson, "placedBlock"),
                    requiredString(entryJson, "family"),
                    requiredString(entryJson, "source"),
                    requiredString(entryJson, "result"),
                    entryJson.get("criterionBefore").getAsBoolean(),
                    entryJson.get("criterionAfter").getAsBoolean(),
                    entryJson.get("handCountBefore").getAsInt(),
                    entryJson.get("handCountAfter").getAsInt(),
                    requiredString(entryJson, "interactionResult"),
                    requiredString(entryJson, "targetPos"),
                    requiredString(entryJson, "supportPos"),
                    requiredString(entryJson, "placedBlockBefore"),
                    requiredString(entryJson, "placedBlockAfter"),
                    entryJson.get("tagMembership").getAsBoolean(),
                    entryJson.get("tagMemberCount").getAsInt(),
                    entryJson.get("ticksToCriterion").getAsInt(),
                    requiredString(entryJson, "catalogFingerprint"),
                    requiredString(entryJson, "runId"),
                    requiredString(entryJson, "minecraftVersion"),
                    requiredString(entryJson, "compatibilityMarker")
                ));
            }
        }
        return new RuntimeExecutionArtifact(
            requiredString(json, "snapshot"),
            requiredString(json, "minecraftVersion"),
            requiredString(json, "compatibilityMarker"),
            requiredString(json, "catalogFingerprint"),
            requiredString(json, "runId"),
            requiredString(json, "generatedAt"),
            List.copyOf(entries)
        );
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        return json.get(key).getAsString();
    }

    private static void requireString(JsonObject json, String key, String expectedValue) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        requireEquals(json.get(key).getAsString(), expectedValue, key);
    }

    private static void requireNonBlank(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Expected nonblank " + key);
        }
    }

    private static void requireEquals(String actualValue, String expectedValue, String fieldName) {
        if (!expectedValue.equals(actualValue)) {
            throw new IllegalStateException("Expected " + fieldName + "=" + expectedValue + " but got " + actualValue);
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

    public record RuntimeExecutionArtifact(
        String snapshot,
        String minecraftVersion,
        String compatibilityMarker,
        String catalogFingerprint,
        String runId,
        String generatedAt,
        List<RuntimeExecutionEntry> entries
    ) {
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
        String itemTag,
        String selectedItem,
        String placedBlock,
        String family,
        String source,
        String result,
        boolean criterionBefore,
        boolean criterionAfter,
        int handCountBefore,
        int handCountAfter,
        String interactionResult,
        String targetPos,
        String supportPos,
        String placedBlockBefore,
        String placedBlockAfter,
        boolean tagMembership,
        int tagMemberCount,
        int ticksToCriterion,
        String catalogFingerprint,
        String runId,
        String minecraftVersion,
        String compatibilityMarker
    ) {
        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("advancementId", advancementId);
            json.addProperty("criterion", criterion);
            json.addProperty("itemTag", itemTag);
            json.addProperty("selectedItem", selectedItem);
            json.addProperty("placedBlock", placedBlock);
            json.addProperty("family", family);
            json.addProperty("source", source);
            json.addProperty("result", result);
            json.addProperty("criterionBefore", criterionBefore);
            json.addProperty("criterionAfter", criterionAfter);
            json.addProperty("handCountBefore", handCountBefore);
            json.addProperty("handCountAfter", handCountAfter);
            json.addProperty("interactionResult", interactionResult);
            json.addProperty("targetPos", targetPos);
            json.addProperty("supportPos", supportPos);
            json.addProperty("placedBlockBefore", placedBlockBefore);
            json.addProperty("placedBlockAfter", placedBlockAfter);
            json.addProperty("tagMembership", tagMembership);
            json.addProperty("tagMemberCount", tagMemberCount);
            json.addProperty("ticksToCriterion", ticksToCriterion);
            json.addProperty("catalogFingerprint", catalogFingerprint);
            json.addProperty("runId", runId);
            json.addProperty("minecraftVersion", minecraftVersion);
            json.addProperty("compatibilityMarker", compatibilityMarker);
            return json;
        }
    }

    private record RuntimeRunState(
        String snapshot,
        String minecraftVersion,
        String compatibilityMarker,
        String catalogFingerprint,
        String runId,
        String startedAt
    ) {
        static RuntimeRunState fromJson(JsonObject json) {
            return new RuntimeRunState(
                json.get("snapshot").getAsString(),
                json.get("minecraftVersion").getAsString(),
                json.get("compatibilityMarker").getAsString(),
                json.get("catalogFingerprint").getAsString(),
                json.get("runId").getAsString(),
                json.get("startedAt").getAsString()
            );
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("snapshot", snapshot);
            json.addProperty("minecraftVersion", minecraftVersion);
            json.addProperty("compatibilityMarker", compatibilityMarker);
            json.addProperty("catalogFingerprint", catalogFingerprint);
            json.addProperty("runId", runId);
            json.addProperty("startedAt", startedAt);
            return json;
        }
    }
}
