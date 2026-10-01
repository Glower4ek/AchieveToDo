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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PhaseARuntimeExecutionEvidence {
    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    public static final Path TEMPORARY_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "runtime_execution_evidence.json");
    public static final Path RUN_STATE_ARTIFACT = Path.of("build", "reports", "phase-a-certification", "runtime_execution_evidence.run.json");
    public static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "runtime_execution_evidence.json");
    public static final String SNAPSHOT = "phase_a_runtime_execution_evidence";
    public static final String MINECRAFT_VERSION = "26.2";
    public static final String COMPATIBILITY_MARKER = "compat_26_2_r15";
    public static final String LOCATION_MOVEMENT_FAMILY = "LOCATION_MOVEMENT";
    public static final String STRUCTURE_ONLY_FAMILY = "STRUCTURE_ONLY";
    public static final String GREEN = "GREEN";
    public static final String AUTOMATION_SUPPORTED = "SUPPORTED";
    private static final String MATRIX_PATH = "src/test/resources/phase_a_certification/location_movement_matrix.json";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseARuntimeExecutionEvidence() {
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
            currentMatrixFingerprint(projectRoot),
            UUID.randomUUID().toString(),
            Instant.now().toString()
        );
        writeJson(projectRoot.resolve(RUN_STATE_ARTIFACT), runState.toJson());
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), emptyArtifact(runState).toJson());
    }

    public static boolean canRecordGreen(Path projectRoot, String advancementId, String criterion, String family) throws IOException {
        if (!isSupportedFamily(family)) {
            return false;
        }
        RuntimeRunState runState = requireRunState(projectRoot);
        MatrixCase matrixCase = requireMatrixCase(projectRoot, advancementId, criterion, family);
        return isAutomationEligible(matrixCase) && runState.matrixFingerprint().equals(currentMatrixFingerprint(projectRoot));
    }

    public static void recordGreen(Path projectRoot, String advancementId, String criterion, String family, String source) throws IOException {
        if (!isSupportedFamily(family)) {
            throw new IllegalStateException("Unsupported runtime evidence family: " + family);
        }
        RuntimeRunState runState = requireRunState(projectRoot);
        MatrixCase matrixCase = requireMatrixCase(projectRoot, advancementId, criterion, family);
        if (!isAutomationEligible(matrixCase)) {
            throw new IllegalStateException("Matrix case is not eligible for GREEN evidence: " + advancementId + "#" + criterion + "#" + family);
        }
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState).append(new RuntimeExecutionEntry(
            advancementId,
            criterion,
            family,
            source,
            GREEN,
            matrixCase.certificationStatus()
        ));
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), artifact.toJson());
    }

    public static RuntimeExecutionArtifact promoteTemporaryArtifact(Path projectRoot) throws IOException {
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = loadTemporaryArtifact(projectRoot, runState);
        validateArtifact(projectRoot, runState, artifact);
        writeJson(projectRoot.resolve(PERSISTENT_ARTIFACT), artifact.toJson());
        return artifact;
    }

    public static RuntimeExecutionArtifact loadValidatedPersistentArtifact(Path projectRoot) throws IOException {
        Path artifactPath = projectRoot.resolve(PERSISTENT_ARTIFACT);
        if (!Files.exists(artifactPath)) {
            return emptyArtifact(requireRunState(projectRoot));
        }
        RuntimeRunState runState = requireRunState(projectRoot);
        RuntimeExecutionArtifact artifact = RuntimeExecutionArtifact.fromJson(readJson(artifactPath));
        validateArtifact(projectRoot, runState, artifact);
        return artifact;
    }

    public static String currentMatrixFingerprint(Path projectRoot) throws IOException {
        byte[] bytes = Files.readAllBytes(projectRoot.resolve(MATRIX_PATH));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha-256:" + toHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    public static String familyFor(String category, String structureLocationType) {
        if ("BIOME_ONLY".equals(category) || "BIOME_SET".equals(category)) {
            return LOCATION_MOVEMENT_FAMILY;
        }
        if ("STRUCTURE_LOCATION".equals(category) && "STRUCTURE_ONLY".equals(structureLocationType)) {
            return STRUCTURE_ONLY_FAMILY;
        }
        return null;
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
        requireString(json, "matrixFingerprint", currentMatrixFingerprint(projectRoot));
        return RuntimeRunState.fromJson(json);
    }

    private static RuntimeExecutionArtifact loadTemporaryArtifact(Path projectRoot, RuntimeRunState runState) throws IOException {
        Path artifactPath = projectRoot.resolve(TEMPORARY_ARTIFACT);
        if (!Files.exists(artifactPath)) {
            return emptyArtifact(runState);
        }
        RuntimeExecutionArtifact artifact = RuntimeExecutionArtifact.fromJson(readJson(artifactPath));
        validateArtifact(projectRoot, runState, artifact);
        return artifact;
    }

    private static RuntimeExecutionArtifact emptyArtifact(RuntimeRunState runState) {
        return new RuntimeExecutionArtifact(
            SNAPSHOT,
            MINECRAFT_VERSION,
            COMPATIBILITY_MARKER,
            runState.matrixFingerprint(),
            runState.runId(),
            Instant.EPOCH.toString(),
            List.of()
        );
    }

    private static void validateArtifact(Path projectRoot, RuntimeRunState runState, RuntimeExecutionArtifact artifact) throws IOException {
        requireEquals(artifact.snapshot(), SNAPSHOT, "snapshot");
        requireEquals(artifact.minecraftVersion(), MINECRAFT_VERSION, "minecraftVersion");
        requireEquals(artifact.compatibilityMarker(), COMPATIBILITY_MARKER, "compatibilityMarker");
        requireEquals(artifact.matrixFingerprint(), runState.matrixFingerprint(), "matrixFingerprint");
        requireEquals(artifact.runId(), runState.runId(), "runId");
        Map<String, MatrixCase> matrix = loadCurrentMatrix(projectRoot);
        for (RuntimeExecutionEntry entry : artifact.entries()) {
            MatrixCase matrixCase = matrix.get(key(entry.advancementId(), entry.criterion(), entry.family()));
            if (matrixCase == null) {
                throw new IllegalStateException("Runtime receipt references missing matrix case: " + entry);
            }
            requireEquals(entry.matrixCertificationStatus(), matrixCase.certificationStatus(), "matrixCertificationStatus");
            if (!GREEN.equals(entry.result())) {
                throw new IllegalStateException("Runtime receipt is not GREEN: " + entry);
            }
            if (!isAutomationEligible(matrixCase)) {
                throw new IllegalStateException("Runtime receipt references ineligible matrix case: " + entry);
            }
        }
    }

    private static boolean isAutomationEligible(MatrixCase matrixCase) {
        return AUTOMATION_SUPPORTED.equals(matrixCase.automationEligibility());
    }

    private static MatrixCase requireMatrixCase(Path projectRoot, String advancementId, String criterion, String family) throws IOException {
        MatrixCase matrixCase = loadCurrentMatrix(projectRoot).get(key(advancementId, criterion, family));
        if (matrixCase == null) {
            throw new IllegalStateException("Missing current matrix case for " + key(advancementId, criterion, family));
        }
        return matrixCase;
    }

    private static Map<String, MatrixCase> loadCurrentMatrix(Path projectRoot) throws IOException {
        Map<String, MatrixCase> matrix = new LinkedHashMap<>();
        JsonObject root = readJson(projectRoot.resolve(MATRIX_PATH));
        JsonArray cases = root.getAsJsonArray("cases");
        for (JsonElement element : cases) {
            JsonObject caseJson = element.getAsJsonObject();
            String structureLocationType = caseJson.has("structureLocationType") && !caseJson.get("structureLocationType").isJsonNull()
                ? caseJson.get("structureLocationType").getAsString()
                : null;
            String family = familyFor(caseJson.get("category").getAsString(), structureLocationType);
            if (family == null) {
                continue;
            }
            String advancementId = caseJson.get("advancementId").getAsString();
            String criterion = caseJson.get("criterion").getAsString();
            matrix.putIfAbsent(key(advancementId, criterion, family), new MatrixCase(
                advancementId,
                criterion,
                family,
                caseJson.get("automationEligibility").getAsString(),
                caseJson.get("certificationStatus").getAsString()
            ));
        }
        return matrix;
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

    private static boolean isSupportedFamily(String family) {
        return LOCATION_MOVEMENT_FAMILY.equals(family) || STRUCTURE_ONLY_FAMILY.equals(family);
    }

    private static String key(String advancementId, String criterion, String family) {
        return advancementId + "#" + criterion + "#" + family;
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
        String matrixFingerprint,
        String runId,
        String generatedAt,
        List<RuntimeExecutionEntry> entries
    ) {
        RuntimeExecutionArtifact append(RuntimeExecutionEntry entry) {
            List<RuntimeExecutionEntry> updated = new ArrayList<>(entries);
            updated.add(entry);
            updated.sort(Comparator.comparing(RuntimeExecutionEntry::advancementId).thenComparing(RuntimeExecutionEntry::criterion).thenComparing(RuntimeExecutionEntry::family));
            LinkedHashMap<String, RuntimeExecutionEntry> deduped = new LinkedHashMap<>();
            for (RuntimeExecutionEntry current : updated) {
                deduped.putIfAbsent(key(current.advancementId(), current.criterion(), current.family()), current);
            }
            return new RuntimeExecutionArtifact(snapshot, minecraftVersion, compatibilityMarker, matrixFingerprint, runId, Instant.now().toString(), List.copyOf(deduped.values()));
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
                        entryJson.get("source").getAsString(),
                        entryJson.get("result").getAsString(),
                        entryJson.get("matrixCertificationStatus").getAsString()
                    ));
                }
            }
            return new RuntimeExecutionArtifact(
                json.get("snapshot").getAsString(),
                json.get("minecraftVersion").getAsString(),
                json.get("compatibilityMarker").getAsString(),
                json.get("matrixFingerprint").getAsString(),
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
            json.addProperty("matrixFingerprint", matrixFingerprint);
            json.addProperty("runId", runId);
            json.addProperty("generatedAt", generatedAt);
            JsonArray entriesJson = new JsonArray();
            for (RuntimeExecutionEntry entry : entries) {
                JsonObject entryJson = new JsonObject();
                entryJson.addProperty("advancementId", entry.advancementId());
                entryJson.addProperty("criterion", entry.criterion());
                entryJson.addProperty("family", entry.family());
                entryJson.addProperty("source", entry.source());
                entryJson.addProperty("result", entry.result());
                entryJson.addProperty("matrixCertificationStatus", entry.matrixCertificationStatus());
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
        String source,
        String result,
        String matrixCertificationStatus
    ) {
    }

    private record RuntimeRunState(
        String snapshot,
        String minecraftVersion,
        String compatibilityMarker,
        String matrixFingerprint,
        String runId,
        String startedAt
    ) {
        static RuntimeRunState fromJson(JsonObject json) {
            return new RuntimeRunState(
                json.get("snapshot").getAsString(),
                json.get("minecraftVersion").getAsString(),
                json.get("compatibilityMarker").getAsString(),
                json.get("matrixFingerprint").getAsString(),
                json.get("runId").getAsString(),
                json.get("startedAt").getAsString()
            );
        }

        JsonObject toJson() {
            JsonObject json = new JsonObject();
            json.addProperty("snapshot", snapshot);
            json.addProperty("minecraftVersion", minecraftVersion);
            json.addProperty("compatibilityMarker", compatibilityMarker);
            json.addProperty("matrixFingerprint", matrixFingerprint);
            json.addProperty("runId", runId);
            json.addProperty("startedAt", startedAt);
            return json;
        }
    }

    private record MatrixCase(
        String advancementId,
        String criterion,
        String family,
        String automationEligibility,
        String certificationStatus
    ) {
    }
}
