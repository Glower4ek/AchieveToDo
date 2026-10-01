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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** TEMP-first recorder and guarded promoter for the damage-source family. */
public final class PhaseAPlayerHurtEntityDamageSourceExecutionEvidence {
    public static final Path TEMPORARY_ARTIFACT =
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.TEMPORARY_ARTIFACT;
    public static final Path RUN_STATE_ARTIFACT =
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RUN_STATE_ARTIFACT;
    public static final Path PERSISTENT_ARTIFACT =
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.PERSISTENT_ARTIFACT;
    public static final Path CATALOG_PATH =
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.CATALOG_PATH;
    public static final String SNAPSHOT =
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SNAPSHOT;
    public static final String FAMILY =
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.FAMILY;
    public static final String SOURCE =
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SOURCE;
    public static final String MINECRAFT_VERSION =
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.MINECRAFT_VERSION;
    public static final String COMPATIBILITY_MARKER =
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.COMPATIBILITY_MARKER;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAPlayerHurtEntityDamageSourceExecutionEvidence() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2 || !"promote".equals(args[0])) {
            throw new IllegalArgumentException("Expected: promote <project root>");
        }
        Path projectRoot = Path.of(args[1]).toAbsolutePath().normalize();
        RuntimeExecutionArtifact artifact = promote(projectRoot);
        System.out.println("PERSISTENT_PROMOTION=PASS family=" + FAMILY
            + " runId=" + artifact.runId() + " entries=" + artifact.entries().size());
    }

    public static Path projectRoot() {
        String configured = System.getProperty("achievetodo.phaseA.projectRoot");
        Path configuredRoot = Path.of(configured == null || configured.isBlank() ? "." : configured)
            .toAbsolutePath().normalize();
        for (Path candidateRoot = configuredRoot; candidateRoot != null; candidateRoot = candidateRoot.getParent()) {
            if (Files.isRegularFile(candidateRoot.resolve(CATALOG_PATH))) {
                return candidateRoot;
            }
        }
        return configuredRoot;
    }

    public static String beginRun(Path projectRoot) throws IOException {
        String runId = UUID.randomUUID().toString();
        String fingerprint = PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation
            .currentCatalogFingerprint(projectRoot);
        Files.createDirectories(projectRoot.resolve(TEMPORARY_ARTIFACT).getParent());
        JsonObject state = new JsonObject();
        state.addProperty("snapshot", SNAPSHOT);
        state.addProperty("schemaVersion", 1);
        state.addProperty("family", FAMILY);
        state.addProperty("source", SOURCE);
        state.addProperty("minecraftVersion", MINECRAFT_VERSION);
        state.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        state.addProperty("catalogFingerprint", fingerprint);
        state.addProperty("runId", runId);
        state.addProperty("startedAt", Instant.now().toString());
        writeJson(projectRoot.resolve(RUN_STATE_ARTIFACT), state);

        JsonObject artifact = new JsonObject();
        artifact.addProperty("snapshot", SNAPSHOT);
        artifact.addProperty("schemaVersion", 1);
        artifact.addProperty("family", FAMILY);
        artifact.addProperty("source", SOURCE);
        artifact.addProperty("minecraftVersion", MINECRAFT_VERSION);
        artifact.addProperty("compatibilityMarker", COMPATIBILITY_MARKER);
        artifact.addProperty("catalogFingerprint", fingerprint);
        artifact.addProperty("runId", runId);
        artifact.addProperty("generatedAt", Instant.now().toString());
        artifact.add("entries", new JsonArray());
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), artifact);
        return runId;
    }

    public static String currentRunId(Path projectRoot) throws IOException {
        JsonObject state = JsonParser.parseString(Files.readString(
            projectRoot.resolve(RUN_STATE_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        return state.get("runId").getAsString();
    }

    public static void recordGreen(Path projectRoot, JsonObject receipt) throws IOException {
        JsonObject state = JsonParser.parseString(Files.readString(
            projectRoot.resolve(RUN_STATE_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        String runId = state.get("runId").getAsString();
        String fingerprint = state.get("catalogFingerprint").getAsString();
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation
            .validateReceipt(projectRoot, receipt, runId, fingerprint);

        JsonObject artifact = JsonParser.parseString(Files.readString(
            projectRoot.resolve(TEMPORARY_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray entries = artifact.getAsJsonArray("entries");
        String key = receipt.get("advancementId").getAsString() + "#" + receipt.get("criterion").getAsString();
        for (JsonElement element : entries) {
            JsonObject existing = element.getAsJsonObject();
            String existingKey = existing.get("advancementId").getAsString()
                + "#" + existing.get("criterion").getAsString();
            if (key.equals(existingKey)) {
                throw new IllegalStateException("Duplicate " + FAMILY + " receipt: " + key);
            }
        }
        entries.add(receipt);
        List<JsonObject> sorted = new ArrayList<>();
        for (JsonElement element : entries) {
            sorted.add(element.getAsJsonObject());
        }
        sorted.sort(Comparator.comparing(element -> element.get("advancementId").getAsString()
            + "#" + element.get("criterion").getAsString()));
        JsonArray ordered = new JsonArray();
        sorted.forEach(ordered::add);
        artifact.add("entries", ordered);
        artifact.addProperty("generatedAt", Instant.now().toString());
        writeJson(projectRoot.resolve(TEMPORARY_ARTIFACT), artifact);
    }

    public static RuntimeExecutionArtifact loadValidatedTemporaryArtifact(Path projectRoot) throws IOException {
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.loadValidatedTemporaryArtifact(projectRoot);
        return new RuntimeExecutionArtifact(artifact.runId(), artifact.entries());
    }

    public static RuntimeExecutionArtifact loadValidatedPromotableTemporaryArtifact(Path projectRoot)
        throws IOException {
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation
                .loadValidatedPromotableTemporaryArtifact(projectRoot);
        return new RuntimeExecutionArtifact(artifact.runId(), artifact.entries());
    }

    public static RuntimeExecutionArtifact loadValidatedPersistentArtifact(Path projectRoot) throws IOException {
        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.loadValidatedPersistentArtifact(projectRoot);
        return new RuntimeExecutionArtifact(artifact.runId(), artifact.entries());
    }

    public static RuntimeExecutionArtifact promote(Path projectRoot) throws IOException {
        RuntimeExecutionArtifact artifact = loadValidatedPromotableTemporaryArtifact(projectRoot);
        Path persistent = projectRoot.resolve(PERSISTENT_ARTIFACT);
        if (Files.exists(persistent)) {
            throw new IllegalStateException("Refusing to overwrite existing persistent evidence: " + persistent);
        }
        Files.createDirectories(persistent.getParent());
        Files.copy(projectRoot.resolve(TEMPORARY_ARTIFACT), persistent);
        return artifact;
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
    }

    public record RuntimeExecutionArtifact(String runId, List<JsonObject> entries) {
    }
}
