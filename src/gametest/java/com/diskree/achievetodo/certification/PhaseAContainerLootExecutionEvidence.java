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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class PhaseAContainerLootExecutionEvidence {
    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    public static final Path TEMPORARY_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "container_loot_execution_evidence.json");
    public static final Path RUN_STATE_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "container_loot_execution_evidence.run.json");
    public static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "container_loot_execution_evidence.json");
    public static final String SNAPSHOT = "phase_a_container_loot_execution_evidence";
    public static final String CATALOG_SNAPSHOT = "phase_a_container_loot_case_catalog";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String PLAYER_GENERATES_CONTAINER_LOOT_FAMILY = "PLAYER_GENERATES_CONTAINER_LOOT";
    public static final String GREEN = "GREEN";
    public static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static final Path CATALOG_PATH = Path.of("src", "test", "resources", "phase_a_certification", "container_loot_case_catalog.json");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAContainerLootExecutionEvidence() {
    }

    public static void main(String[] args) throws Exception {
        Path projectRoot = args.length > 1 ? Path.of(args[1]).toAbsolutePath().normalize() : projectRoot();
        if (args.length > 0 && "promote".equals(args[0])) {
            promoteTemporaryArtifact(projectRoot);
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

    public static void recordGreen(Path projectRoot, String advancementId, String criterion, String lootTable, String family, String source) throws IOException {
        if (!PLAYER_GENERATES_CONTAINER_LOOT_FAMILY.equals(family)) {
            throw new IllegalStateException("Unsupported runtime evidence family: " + family);
        }
        RuntimeRunState runState = requireRunState(projectRoot);
        CatalogCase catalogCase = requireCatalogCase(projectRoot, advancementId, criterion);
        if (!isAutomationEligible(catalogCase)) {
            throw new IllegalStateException("Catalog case is not eligible for GREEN evidence: " + advancementId + "#" + criterion);
        }
        requireEquals(lootTable, catalogCase.lootTable(), "lootTable");
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        String receiptKey = key(advancementId, criterion);
        if (artifact.containsReceiptKey(receiptKey)) {
            throw new IllegalStateException("Duplicate runtime receipt attempted for " + receiptKey);
        }
        RuntimeExecutionArtifact updatedArtifact = artifact.append(new RuntimeExecutionEntry(
            advancementId,
            criterion,
            family,
            lootTable,
            source,
            GREEN
        ));
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), updatedArtifact.toJson());
    }

    public static RuntimeExecutionArtifact promoteTemporaryArtifact(Path projectRoot) throws IOException {
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

    private static RuntimeRunState requireRunState(Path projectRoot) throws IOException {
        Path runStatePath = projectRoot.resolve(RUN_STATE_ARTIFACT);
        if (!Files.exists(runStatePath)) {
            throw new IllegalStateException("Missing runtime evidence run-state: " + runStatePath);
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
        RuntimeExecutionArtifact artifact = RuntimeExecutionArtifact.fromJson(readJson(artifactPath));
        validateArtifact(projectRoot, runState, artifact, false);
        return artifact;
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

    private static void validateArtifact(Path projectRoot, RuntimeRunState runState, RuntimeExecutionArtifact artifact, boolean requireCompleteSupportedCoverage) throws IOException {
        requireEquals(artifact.snapshot(), SNAPSHOT, "snapshot");
        requireEquals(artifact.minecraftVersion(), MINECRAFT_VERSION, "minecraftVersion");
        requireEquals(artifact.compatibilityMarker(), COMPATIBILITY_MARKER, "compatibilityMarker");
        requireEquals(artifact.catalogFingerprint(), runState.catalogFingerprint(), "catalogFingerprint");
        requireEquals(artifact.runId(), runState.runId(), "runId");
        requireNonBlank(artifact.generatedAt(), "generatedAt");
        Map<String, CatalogCase> catalog = loadCurrentCatalog(projectRoot);
        Set<String> expectedSupportedKeys = supportedCatalogKeys(catalog.values());
        Set<String> actualReceiptKeys = new LinkedHashSet<>();
        for (RuntimeExecutionEntry entry : artifact.entries()) {
            String receiptKey = key(entry.advancementId(), entry.criterion());
            if (!actualReceiptKeys.add(receiptKey)) {
                throw new IllegalStateException("Duplicate runtime receipt key: " + receiptKey);
            }
            CatalogCase catalogCase = catalog.get(receiptKey);
            if (catalogCase == null) {
                throw new IllegalStateException("Runtime receipt references missing catalog case: " + entry);
            }
            requireEquals(entry.family(), PLAYER_GENERATES_CONTAINER_LOOT_FAMILY, "family");
            requireEquals(entry.lootTable(), catalogCase.lootTable(), "lootTable");
            if (!GREEN.equals(entry.result())) {
                throw new IllegalStateException("Runtime receipt is not GREEN: " + entry);
            }
            if (!isAutomationEligible(catalogCase)) {
                throw new IllegalStateException("Runtime receipt references ineligible catalog case: " + entry);
            }
        }
        if (requireCompleteSupportedCoverage && !actualReceiptKeys.equals(expectedSupportedKeys)) {
            throw new IllegalStateException("Runtime artifact receipt keys do not exactly match current SUPPORTED catalog keys");
        }
    }

    private static boolean isAutomationEligible(CatalogCase catalogCase) {
        return AUTOMATION_SUPPORTED.equals(catalogCase.automationEligibility());
    }

    private static CatalogCase requireCatalogCase(Path projectRoot, String advancementId, String criterion) throws IOException {
        CatalogCase catalogCase = loadCurrentCatalog(projectRoot).get(key(advancementId, criterion));
        if (catalogCase == null) {
            throw new IllegalStateException("Missing current catalog case for " + key(advancementId, criterion));
        }
        return catalogCase;
    }

    private static Map<String, CatalogCase> loadCurrentCatalog(Path projectRoot) throws IOException {
        Map<String, CatalogCase> catalog = new LinkedHashMap<>();
        JsonObject root = readJson(projectRoot.resolve(CATALOG_PATH));
        requireString(root, "snapshot", CATALOG_SNAPSHOT);
        JsonArray cases = root.getAsJsonArray("cases");
        if (cases == null) {
            return catalog;
        }
        for (JsonElement element : cases) {
            JsonObject caseJson = element.getAsJsonObject();
            String advancementId = caseJson.get("advancementId").getAsString();
            String criterion = caseJson.get("criterion").getAsString();
            String lootTable = caseJson.has("lootTable") && !caseJson.get("lootTable").isJsonNull()
                ? caseJson.get("lootTable").getAsString()
                : null;
            catalog.put(key(advancementId, criterion), new CatalogCase(
                advancementId,
                criterion,
                lootTable,
                caseJson.get("automationEligibility").getAsString()
            ));
        }
        return catalog;
    }

    private static Set<String> supportedCatalogKeys(Collection<CatalogCase> catalogCases) {
        Set<String> keys = new LinkedHashSet<>();
        for (CatalogCase catalogCase : catalogCases) {
            if (isAutomationEligible(catalogCase)) {
                keys.add(key(catalogCase.advancementId(), catalogCase.criterion()));
            }
        }
        return keys;
    }

    private static JsonObject readJson(Path path) throws IOException {
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static void requireString(JsonObject json, String key, String expectedValue) {
        if (!json.has(key)) {
            throw new IllegalStateException("Missing " + key);
        }
        requireEquals(json.get(key).getAsString(), expectedValue, key);
    }

    private static void requireEquals(String actualValue, String expectedValue, String fieldName) {
        if (!expectedValue.equals(actualValue)) {
            throw new IllegalStateException("Expected " + fieldName + "=" + expectedValue + " but got " + actualValue);
        }
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Expected nonblank " + fieldName);
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
        boolean containsReceiptKey(String receiptKey) {
            for (RuntimeExecutionEntry entry : entries) {
                if (key(entry.advancementId(), entry.criterion()).equals(receiptKey)) {
                    return true;
                }
            }
            return false;
        }

        RuntimeExecutionArtifact append(RuntimeExecutionEntry entry) {
            List<RuntimeExecutionEntry> updated = new ArrayList<>(entries);
            updated.add(entry);
            updated.sort(Comparator.comparing(RuntimeExecutionEntry::advancementId).thenComparing(RuntimeExecutionEntry::criterion).thenComparing(RuntimeExecutionEntry::family));
            LinkedHashMap<String, RuntimeExecutionEntry> deduped = new LinkedHashMap<>();
            for (RuntimeExecutionEntry current : updated) {
                deduped.putIfAbsent(key(current.advancementId(), current.criterion()), current);
            }
            return new RuntimeExecutionArtifact(snapshot, minecraftVersion, compatibilityMarker, catalogFingerprint, runId, Instant.now().toString(), List.copyOf(deduped.values()));
        }

        static RuntimeExecutionArtifact fromJson(JsonObject json) {
            List<RuntimeExecutionEntry> entries = new ArrayList<>();
            JsonArray entriesJson = json.getAsJsonArray("entries");
            if (entriesJson != null) {
                for (JsonElement element : entriesJson) {
                    JsonObject entryJson = element.getAsJsonObject();
                    entries.add(new RuntimeExecutionEntry(
                        entryJson.get("advancementId").getAsString(),
                        entryJson.get("criterion").getAsString(),
                        entryJson.get("family").getAsString(),
                        entryJson.get("lootTable").getAsString(),
                        entryJson.get("source").getAsString(),
                        entryJson.get("result").getAsString()
                    ));
                }
            }
            return new RuntimeExecutionArtifact(
                json.get("snapshot").getAsString(),
                json.get("minecraftVersion").getAsString(),
                json.get("compatibilityMarker").getAsString(),
                json.get("catalogFingerprint").getAsString(),
                json.get("runId").getAsString(),
                json.get("generatedAt").getAsString(),
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
                JsonObject entryJson = new JsonObject();
                entryJson.addProperty("advancementId", entry.advancementId());
                entryJson.addProperty("criterion", entry.criterion());
                entryJson.addProperty("family", entry.family());
                entryJson.addProperty("lootTable", entry.lootTable());
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
        String lootTable,
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

    private record CatalogCase(String advancementId, String criterion, String lootTable, String automationEligibility) {
    }
}
