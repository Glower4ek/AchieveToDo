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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** TEMP recorder and persistent promotion adapter for USING_ITEM. */
public final class PhaseAUsingItemExecutionEvidence {
    public static final Path TEMPORARY_ARTIFACT = PhaseAUsingItemExecutionEvidenceValidation.TEMPORARY_ARTIFACT;
    public static final Path RUN_STATE_ARTIFACT = PhaseAUsingItemExecutionEvidenceValidation.RUN_STATE_ARTIFACT;
    public static final Path PERSISTENT_ARTIFACT = PhaseAUsingItemExecutionEvidenceValidation.PERSISTENT_ARTIFACT;
    public static final Path CATALOG_PATH = PhaseAUsingItemExecutionEvidenceValidation.CATALOG_PATH;
    public static final String SNAPSHOT = PhaseAUsingItemExecutionEvidenceValidation.SNAPSHOT;
    public static final String FAMILY = PhaseAUsingItemExecutionEvidenceValidation.FAMILY;
    public static final String SOURCE = PhaseAUsingItemExecutionEvidenceValidation.SOURCE;
    public static final String GREEN = PhaseAUsingItemExecutionEvidenceValidation.GREEN;
    public static final String MINECRAFT_VERSION = PhaseAUsingItemExecutionEvidenceValidation.MINECRAFT_VERSION;
    public static final String COMPATIBILITY_MARKER = PhaseAUsingItemExecutionEvidenceValidation.COMPATIBILITY_MARKER;
    public static final String TRIGGER = PhaseAUsingItemExecutionEvidenceValidation.TRIGGER;
    public static final String BOUNDARY = PhaseAUsingItemExecutionEvidenceValidation.BOUNDARY;
    public static final String PACKET_PATH = PhaseAUsingItemExecutionEvidenceValidation.PACKET_PATH;
    private static final int SCHEMA_VERSION = 1;
    private static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAUsingItemExecutionEvidence() {
    }

    public static void main(String[] args) throws Exception {
        Path root = args.length > 1 ? Path.of(args[1]).toAbsolutePath().normalize() : projectRoot();
        System.setProperty(PROJECT_ROOT_PROPERTY, root.toString());
        if (args.length > 0 && "reset".equals(args[0])) {
            resetRun(root);
            return;
        }
        if (args.length > 0 && "promote".equals(args[0])) {
            promote(root);
            return;
        }
        throw new IllegalArgumentException("Expected promote or reset command");
    }

    public static Path projectRoot() {
        String configured = System.getProperty(PROJECT_ROOT_PROPERTY);
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Missing system property " + PROJECT_ROOT_PROPERTY);
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    public static void resetRun(Path root) throws IOException {
        beginRun(root);
    }

    public static String beginRun(Path root) throws IOException {
        String runId = UUID.randomUUID().toString();
        String fingerprint = currentCatalogFingerprint(root);
        JsonObject state = new JsonObject();
        state.addProperty("snapshot", SNAPSHOT);
        state.addProperty("schemaVersion", SCHEMA_VERSION);
        state.addProperty("family", FAMILY);
        state.addProperty("source", SOURCE);
        state.addProperty("minecraftVersion", MINECRAFT_VERSION);
        state.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        state.addProperty("catalogFingerprint", fingerprint);
        state.addProperty("runId", runId);
        state.addProperty("startedAt", Instant.now().toString());
        writeJson(root.resolve(RUN_STATE_ARTIFACT), state);
        writeJson(root.resolve(TEMPORARY_ARTIFACT), emptyArtifact(runId, fingerprint));
        return runId;
    }

    public static String currentRunId(Path root) throws IOException {
        JsonObject state = readJson(root.resolve(RUN_STATE_ARTIFACT));
        requireString(state, "snapshot", SNAPSHOT);
        requireInt(state, "schemaVersion", SCHEMA_VERSION);
        requireString(state, "family", FAMILY);
        requireString(state, "source", SOURCE);
        requireString(state, "minecraftVersion", MINECRAFT_VERSION);
        requireString(state, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(state, "catalogFingerprint", currentCatalogFingerprint(root));
        String runId = requiredNonBlank(state, "runId");
        UUID.fromString(runId);
        return runId;
    }

    public static void recordGreen(Path root, JsonObject receipt) throws IOException {
        String runId = currentRunId(root);
        String fingerprint = currentCatalogFingerprint(root);
        JsonObject artifact = Files.exists(root.resolve(TEMPORARY_ARTIFACT))
            ? readJson(root.resolve(TEMPORARY_ARTIFACT))
            : emptyArtifact(runId, fingerprint);
        requireString(artifact, "snapshot", SNAPSHOT);
        requireInt(artifact, "schemaVersion", SCHEMA_VERSION);
        requireString(artifact, "family", FAMILY);
        requireString(artifact, "source", SOURCE);
        requireString(artifact, "minecraftVersion", MINECRAFT_VERSION);
        requireString(artifact, "compatibilityMarker", COMPATIBILITY_MARKER);
        requireString(artifact, "catalogFingerprint", fingerprint);
        requireString(artifact, "runId", runId);
        PhaseAUsingItemExecutionEvidenceValidation.validateReceipt(root, receipt, runId, fingerprint);

        String key = requiredNonBlank(receipt, "advancementId") + "#" + requiredNonBlank(receipt, "criterion");
        JsonArray entries = requiredArray(artifact, "entries");
        List<JsonObject> ordered = new ArrayList<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                throw new IllegalStateException("USING_ITEM evidence entry is not an object");
            }
            JsonObject existing = element.getAsJsonObject();
            String existingKey = requiredNonBlank(existing, "advancementId") + "#" + requiredNonBlank(existing, "criterion");
            if (key.equals(existingKey)) {
                throw new IllegalStateException("Duplicate USING_ITEM runtime evidence receipt: " + key);
            }
            ordered.add(existing);
        }
        ordered.add(receipt.deepCopy());
        ordered.sort(Comparator.comparing(e -> requiredNonBlank(e, "advancementId") + "#" + requiredNonBlank(e, "criterion")));
        JsonArray sorted = new JsonArray();
        ordered.forEach(sorted::add);
        artifact.add("entries", sorted);
        writeJson(root.resolve(TEMPORARY_ARTIFACT), artifact);
    }

    public static PhaseAUsingItemExecutionEvidenceValidation.RuntimeArtifact loadValidatedTemporaryArtifact(Path root)
        throws IOException {
        return PhaseAUsingItemExecutionEvidenceValidation.loadValidatedTemporaryArtifact(root);
    }

    public static PhaseAUsingItemExecutionEvidenceValidation.RuntimeArtifact loadValidatedPromotableTemporaryArtifact(Path root)
        throws IOException {
        return PhaseAUsingItemExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(root);
    }

    public static PhaseAUsingItemExecutionEvidenceValidation.RuntimeArtifact promote(Path root) throws IOException {
        var artifact = PhaseAUsingItemExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(root);
        Path target = root.resolve(PERSISTENT_ARTIFACT);
        if (Files.exists(target)) {
            throw new IllegalStateException("Refusing to overwrite existing persistent USING_ITEM evidence");
        }
        Files.createDirectories(target.getParent());
        Files.copy(root.resolve(TEMPORARY_ARTIFACT), target, StandardCopyOption.COPY_ATTRIBUTES);
        return artifact;
    }

    public static String currentCatalogFingerprint(Path root) throws IOException {
        try {
            return "sha-256:" + hex(MessageDigest.getInstance("SHA-256").digest(
                Files.readAllBytes(root.resolve(CATALOG_PATH))
            ));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Missing SHA-256 support", e);
        }
    }

    private static JsonObject emptyArtifact(String runId, String fingerprint) {
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT);
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.addProperty("family", FAMILY);
        root.addProperty("source", SOURCE);
        root.addProperty("minecraftVersion", MINECRAFT_VERSION);
        root.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        root.addProperty("catalogFingerprint", fingerprint);
        root.addProperty("runId", runId);
        root.addProperty("generatedAt", Instant.now().toString());
        root.add("entries", new JsonArray());
        return root;
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static JsonObject readJson(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing USING_ITEM evidence file: " + path);
        }
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonArray requiredArray(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonArray()) {
            throw new IllegalStateException("Missing array " + key + " in USING_ITEM evidence");
        }
        return value.getAsJsonArray();
    }

    private static String requiredNonBlank(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()
            || value.getAsString().isBlank()) {
            throw new IllegalStateException("Missing or blank " + key + " in USING_ITEM evidence");
        }
        return value.getAsString();
    }

    private static void requireString(JsonObject json, String key, String expected) {
        if (!expected.equals(requiredNonBlank(json, key))) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in USING_ITEM evidence");
        }
    }

    private static void requireInt(JsonObject json, String key, int expected) {
        JsonElement value = json.get(key);
        if (value == null || !value.isJsonPrimitive() || value.getAsInt() != expected) {
            throw new IllegalStateException("Expected " + key + "=" + expected + " in USING_ITEM evidence");
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
}
