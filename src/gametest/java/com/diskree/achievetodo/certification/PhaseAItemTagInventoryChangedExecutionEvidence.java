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
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PhaseAItemTagInventoryChangedExecutionEvidence {
    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    public static final Path TEMPORARY_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "item_tag_inventory_changed_execution_evidence.json");
    public static final Path RUN_STATE_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "item_tag_inventory_changed_execution_evidence.run.json");
    public static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_inventory_changed_execution_evidence.json");
    public static final String SNAPSHOT = "phase_a_item_tag_inventory_changed_execution_evidence";
    public static final String CATALOG_SNAPSHOT = "phase_a_item_tag_inventory_changed_case_catalog";
    public static final String FAMILY = "ITEM_TAG_INVENTORY_CHANGED";
    public static final String GREEN = "GREEN";
    public static final String SOURCE = "PhaseAItemTagInventoryChangedGameTest";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    private static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static final String INVENTORY_CHANGED_TRIGGER = "minecraft:inventory_changed";
    private static final Path CATALOG_PATH = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_inventory_changed_case_catalog.json");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAItemTagInventoryChangedExecutionEvidence() {
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

    public static void recordGreen(
        Path projectRoot,
        String advancementId,
        String criterion,
        String itemTag,
        String selectedItem,
        int requiredCount,
        String source
    ) throws IOException {
        if (selectedItem == null || selectedItem.isBlank()) {
            throw new IllegalStateException("Expected nonblank selectedItem for " + advancementId + "#" + criterion);
        }
        if (!SOURCE.equals(source)) {
            throw new IllegalStateException("Expected source=" + SOURCE + " but got " + source);
        }
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        String currentFingerprint = currentCatalogFingerprint(projectRoot);
        validateRunAlignment(runState, artifact, currentFingerprint);

        CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition =
            CertifiedItemTagInventoryChangedCatalog.requiredCase(net.minecraft.resources.Identifier.parse(advancementId), criterion);
        if (!AUTOMATION_SUPPORTED.equals(caseDefinition.automationEligibility())) {
            throw new IllegalStateException("Cannot record runtime evidence for ineligible case " + advancementId + "#" + criterion);
        }
        if (!INVENTORY_CHANGED_TRIGGER.equals(caseDefinition.trigger())) {
            throw new IllegalStateException("Expected inventory_changed trigger for " + advancementId + "#" + criterion);
        }
        if (!caseDefinition.itemTag().equals(itemTag)) {
            throw new IllegalStateException("Expected itemTag=" + caseDefinition.itemTag() + " but got " + itemTag + " for " + advancementId + "#" + criterion);
        }
        if (caseDefinition.requiredAcquisitionCount() != requiredCount) {
            throw new IllegalStateException(
                "Expected requiredCount=" + caseDefinition.requiredAcquisitionCount() + " but got " + requiredCount + " for " + advancementId + "#" + criterion
            );
        }

        String receiptKey = key(advancementId, criterion);
        List<RuntimeExecutionEntry> updatedEntries = new ArrayList<>(artifact.entries());
        for (RuntimeExecutionEntry entry : updatedEntries) {
            if (receiptKey.equals(key(entry.advancementId(), entry.criterion()))) {
                throw new IllegalStateException("Duplicate runtime evidence receipt attempt: " + receiptKey);
            }
        }
        updatedEntries.add(new RuntimeExecutionEntry(
            advancementId,
            criterion,
            FAMILY,
            itemTag,
            selectedItem,
            requiredCount,
            source,
            GREEN
        ));
        updatedEntries.sort(Comparator.comparing(RuntimeExecutionEntry::advancementId).thenComparing(RuntimeExecutionEntry::criterion));
        RuntimeExecutionArtifact updatedArtifact = new RuntimeExecutionArtifact(
            SNAPSHOT,
            MINECRAFT_VERSION,
            COMPATIBILITY_MARKER,
            currentFingerprint,
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
        validateArtifact(projectRoot, runState, artifact, true);
        return artifact;
    }

    public static RuntimeExecutionArtifact promote(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        validateArtifact(projectRoot, runState, artifact, true);
        writeJson(projectRoot.resolve(PERSISTENT_ARTIFACT), artifact.toJson());
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

    private static void validateRunAlignment(RuntimeRunState runState, RuntimeExecutionArtifact artifact, String currentFingerprint) {
        if (!SNAPSHOT.equals(runState.snapshot())) {
            throw new IllegalStateException("Unexpected run-state snapshot: " + runState.snapshot());
        }
        if (!MINECRAFT_VERSION.equals(runState.minecraftVersion())) {
            throw new IllegalStateException("Unexpected run-state minecraftVersion: " + runState.minecraftVersion());
        }
        if (!COMPATIBILITY_MARKER.equals(runState.compatibilityMarker())) {
            throw new IllegalStateException("Unexpected run-state compatibilityMarker: " + runState.compatibilityMarker());
        }
        if (!currentFingerprint.equals(runState.catalogFingerprint())) {
            throw new IllegalStateException("Run-state catalogFingerprint does not match current catalog");
        }
        if (!SNAPSHOT.equals(artifact.snapshot())) {
            throw new IllegalStateException("Unexpected temporary artifact snapshot: " + artifact.snapshot());
        }
        if (!MINECRAFT_VERSION.equals(artifact.minecraftVersion())) {
            throw new IllegalStateException("Unexpected temporary artifact minecraftVersion: " + artifact.minecraftVersion());
        }
        if (!COMPATIBILITY_MARKER.equals(artifact.compatibilityMarker())) {
            throw new IllegalStateException("Unexpected temporary artifact compatibilityMarker: " + artifact.compatibilityMarker());
        }
        if (!currentFingerprint.equals(artifact.catalogFingerprint())) {
            throw new IllegalStateException("Temporary artifact catalogFingerprint does not match current catalog");
        }
        if (!runState.runId().equals(artifact.runId())) {
            throw new IllegalStateException("Temporary artifact runId does not match active run");
        }
    }

    private static void validateArtifact(Path projectRoot, RuntimeRunState runState, RuntimeExecutionArtifact artifact, boolean requireExactCoverage) throws IOException {
        requireEquals(artifact.snapshot(), SNAPSHOT, "snapshot");
        requireEquals(artifact.minecraftVersion(), MINECRAFT_VERSION, "minecraftVersion");
        requireEquals(artifact.compatibilityMarker(), COMPATIBILITY_MARKER, "compatibilityMarker");
        requireEquals(artifact.catalogFingerprint(), runState.catalogFingerprint(), "catalogFingerprint");
        requireEquals(artifact.runId(), runState.runId(), "runId");
        requireNonBlank(artifact.runId(), "runId");
        requireNonBlank(artifact.generatedAt(), "generatedAt");
        Map<String, CertifiedItemTagInventoryChangedCatalog.CaseDefinition> catalogCases = loadCurrentCatalog(projectRoot);
        Set<String> expectedSupportedKeys = supportedCatalogKeys(catalogCases.values());

        Set<String> actualKeys = new LinkedHashSet<>();
        for (RuntimeExecutionEntry entry : artifact.entries()) {
            requireEquals(entry.result(), GREEN, "result");
            requireEquals(entry.family(), FAMILY, "family");
            requireEquals(entry.source(), SOURCE, "source");
            requireNonBlank(entry.selectedItem(), "selectedItem");
            String receiptKey = key(entry.advancementId(), entry.criterion());
            if (!actualKeys.add(receiptKey)) {
                throw new IllegalStateException("Duplicate runtime evidence receipt key: " + receiptKey);
            }
            CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition = catalogCases.get(receiptKey);
            if (caseDefinition == null) {
                throw new IllegalStateException("Runtime evidence references unsupported or unknown catalog case: " + receiptKey);
            }
            if (!AUTOMATION_SUPPORTED.equals(caseDefinition.automationEligibility())) {
                throw new IllegalStateException("Runtime evidence references unsupported or deferred catalog case: " + receiptKey);
            }
            if (!INVENTORY_CHANGED_TRIGGER.equals(caseDefinition.trigger())) {
                throw new IllegalStateException("Runtime evidence references non-inventory_changed catalog case: " + receiptKey);
            }
            if (!caseDefinition.itemTag().equals(entry.itemTag())) {
                throw new IllegalStateException("Runtime evidence itemTag mismatch for " + receiptKey);
            }
            if (caseDefinition.requiredAcquisitionCount() != entry.requiredCount()) {
                throw new IllegalStateException("Runtime evidence requiredCount mismatch for " + receiptKey);
            }
        }

        if (requireExactCoverage && !actualKeys.equals(expectedSupportedKeys)) {
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
            throw new IllegalStateException("Missing active item-tag runtime RUN_STATE artifact");
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

    private static Map<String, CertifiedItemTagInventoryChangedCatalog.CaseDefinition> loadCurrentCatalog(Path projectRoot) throws IOException {
        Map<String, CertifiedItemTagInventoryChangedCatalog.CaseDefinition> supportedCases = new LinkedHashMap<>();
        for (CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition : CertifiedItemTagInventoryChangedCatalog.allCases()) {
            supportedCases.put(key(caseDefinition.advancementId().toString(), caseDefinition.criterion()), caseDefinition);
        }
        return supportedCases;
    }

    private static Set<String> supportedCatalogKeys(Collection<CertifiedItemTagInventoryChangedCatalog.CaseDefinition> caseDefinitions) {
        Set<String> keys = new LinkedHashSet<>();
        for (CertifiedItemTagInventoryChangedCatalog.CaseDefinition caseDefinition : caseDefinitions) {
            if (AUTOMATION_SUPPORTED.equals(caseDefinition.automationEligibility())) {
                keys.add(key(caseDefinition.advancementId().toString(), caseDefinition.criterion()));
            }
        }
        return keys;
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
                    requiredString(entryJson, "family"),
                    requiredString(entryJson, "itemTag"),
                    requiredString(entryJson, "selectedItem"),
                    entryJson.get("requiredCount").getAsInt(),
                    requiredString(entryJson, "source"),
                    requiredString(entryJson, "result")
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

    private static String key(String advancementId, String criterion) {
        return advancementId + "#" + criterion;
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

    private static void requireString(String actualValue, String key, String expectedValue) {
        requireEquals(actualValue, expectedValue, key);
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
                JsonObject entryJson = new JsonObject();
                entryJson.addProperty("advancementId", entry.advancementId());
                entryJson.addProperty("criterion", entry.criterion());
                entryJson.addProperty("family", entry.family());
                entryJson.addProperty("itemTag", entry.itemTag());
                entryJson.addProperty("selectedItem", entry.selectedItem());
                entryJson.addProperty("requiredCount", entry.requiredCount());
                entryJson.addProperty("source", entry.source());
                entryJson.addProperty("result", entry.result());
                entriesJson.add(entryJson);
            }
            json.add("entries", entriesJson);
            return json;
        }
    }

    public record RuntimeExecutionEntry(
        String advancementId,
        String criterion,
        String family,
        String itemTag,
        String selectedItem,
        int requiredCount,
        String source,
        String result
    ) {
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
