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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** TEMP-first recorder and protected promoter for the native fishing-rod family. */
public final class PhaseAFishingRodHookedExecutionEvidence {
    public static final Path TEMPORARY_ARTIFACT = PhaseAFishingRodHookedExecutionEvidenceValidation.TEMPORARY_ARTIFACT;
    public static final Path RUN_STATE_ARTIFACT = PhaseAFishingRodHookedExecutionEvidenceValidation.RUN_STATE_ARTIFACT;
    public static final Path DIAGNOSTIC_ARTIFACT = PhaseAFishingRodHookedExecutionEvidenceValidation.DIAGNOSTIC_ARTIFACT;
    public static final Path DIAGNOSTIC_RUN_STATE = PhaseAFishingRodHookedExecutionEvidenceValidation.DIAGNOSTIC_RUN_STATE;
    public static final Path PERSISTENT_ARTIFACT = PhaseAFishingRodHookedCertification.PERSISTENT_EVIDENCE;
    public static final String FAMILY = PhaseAFishingRodHookedCertification.FAMILY;
    public static final String SOURCE = PhaseAFishingRodHookedCertification.SOURCE;
    public static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    private static final String SNAPSHOT = "phase_a_fishing_rod_hooked_execution_evidence";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseAFishingRodHookedExecutionEvidence() { }

    public static void main(String[] args) throws Exception {
        Path root = args.length > 1 ? Path.of(args[1]).toAbsolutePath().normalize() : projectRoot();
        String action = args.length == 0 ? "" : args[0];
        switch (action) {
            case "temporary" -> report(root, "PROMOTABLE");
            case "diagnostic" -> report(root, "DIAGNOSTIC");
            case "promotable" -> report(root, "PROMOTABLE");
            case "persistent" -> {
                var artifact = PhaseAFishingRodHookedExecutionEvidenceValidation.loadPersistentArtifact(root);
                System.out.println("PERSISTENT_VALID=PASS family=" + FAMILY + " runId=" + artifact.runId() + " entries=" + artifact.entries().size());
            }
            case "promote" -> promote(root);
            default -> throw new IllegalArgumentException("Expected diagnostic, temporary, promotable, persistent, or promote");
        }
    }

    public static Path projectRoot() {
        String configured = System.getProperty(PROJECT_ROOT_PROPERTY);
        if (configured == null || configured.isBlank()) throw new IllegalStateException("Missing " + PROJECT_ROOT_PROPERTY);
        return Path.of(configured).toAbsolutePath().normalize();
    }

    public static String beginRun(Path root, String runMode) throws Exception {
        require("DIAGNOSTIC".equals(runMode) || "PROMOTABLE".equals(runMode), "Invalid run mode");
        if (Files.exists(root.resolve(PERSISTENT_ARTIFACT))) {
            throw new IllegalStateException("Refusing to start a new fishing-rod run after persistent evidence was accepted");
        }
        String runId = UUID.randomUUID().toString();
        String fingerprint = PhaseAFishingRodHookedExecutionEvidenceValidation.catalogFingerprint(root);
        JsonObject state = common(runId, fingerprint, runMode);
        state.addProperty("startedAt", Instant.now().toString());
        state.addProperty("beginCount", 1);
        state.addProperty("coordinatorCount", 1);
        state.addProperty("receiptCount", 0);
        JsonObject artifact = common(runId, fingerprint, runMode);
        artifact.addProperty("generatedAt", Instant.now().toString());
        artifact.add("entries", new JsonArray());
        write(root.resolve(PhaseAFishingRodHookedExecutionEvidenceValidation.runStatePath(runMode)), state);
        write(root.resolve(PhaseAFishingRodHookedExecutionEvidenceValidation.temporaryPath(runMode)), artifact);
        return runId;
    }

    public static String currentCatalogFingerprint(Path root) throws Exception {
        return PhaseAFishingRodHookedExecutionEvidenceValidation.catalogFingerprint(root);
    }

    public static void recordGreen(Path root, String runMode, JsonObject receipt) throws Exception {
        String fingerprint = currentCatalogFingerprint(root);
        Path statePath = root.resolve(PhaseAFishingRodHookedExecutionEvidenceValidation.runStatePath(runMode));
        Path artifactPath = root.resolve(PhaseAFishingRodHookedExecutionEvidenceValidation.temporaryPath(runMode));
        JsonObject state = read(statePath);
        validateRunState(state, runMode, fingerprint);
        String runId = required(state, "runId");
        JsonObject artifact = read(artifactPath);
        validateArtifactHeader(artifact, runMode, fingerprint, runId);
        JsonArray entries = artifact.getAsJsonArray("entries");
        require(integer(state, "receiptCount") == entries.size(), "Run-state receipt count reset during the run");
        PhaseAFishingRodHookedExecutionEvidenceValidation.validateReceipt(receipt, runId, fingerprint);
        String key = key(receipt);
        for (JsonElement element : entries) {
            require(!key(element.getAsJsonObject()).equals(key), "Duplicate FISHING_ROD_HOOKED receipt " + key);
        }
        List<JsonObject> ordered = new ArrayList<>();
        for (JsonElement element : entries) ordered.add(element.getAsJsonObject());
        ordered.add(receipt.deepCopy());
        ordered.sort(Comparator.comparing(PhaseAFishingRodHookedExecutionEvidence::key));
        JsonArray sorted = new JsonArray();
        ordered.forEach(sorted::add);
        artifact.add("entries", sorted);
        artifact.addProperty("generatedAt", Instant.now().toString());
        state.addProperty("receiptCount", sorted.size());
        state.addProperty("lastRecordedKey", key);
        write(artifactPath, artifact);
        write(statePath, state);
    }

    public static void promote(Path root) throws Exception {
        var artifact = PhaseAFishingRodHookedExecutionEvidenceValidation.loadTemporary(root, "PROMOTABLE");
        Path target = root.resolve(PERSISTENT_ARTIFACT);
        require(!Files.exists(target), "Refusing to overwrite persistent FISHING_ROD_HOOKED evidence");
        Files.createDirectories(target.getParent());
        Files.copy(root.resolve(TEMPORARY_ARTIFACT), target, StandardCopyOption.COPY_ATTRIBUTES);
        require(Files.mismatch(root.resolve(TEMPORARY_ARTIFACT), target) == -1, "Promoted evidence is not byte-identical to exact TEMP");
        PhaseAFishingRodHookedExecutionEvidenceValidation.loadPersistentArtifact(root);
        System.out.println("FISHING_ROD_HOOKED_PROMOTION=PASS runId=" + artifact.runId() + " byteIdentical=true");
    }

    private static void report(Path root, String runMode) throws Exception {
        var artifact = PhaseAFishingRodHookedExecutionEvidenceValidation.loadTemporary(root, runMode);
        String terminal = "DIAGNOSTIC".equals(runMode) ? "TEMP_DIAGNOSTIC" : "TEMP_PROMOTABLE";
        System.out.println(terminal + "=PASS family=" + FAMILY + " runId=" + artifact.runId() + " entries=" + artifact.entries().size());
    }

    private static JsonObject common(String runId, String fingerprint, String runMode) {
        JsonObject json = new JsonObject();
        json.addProperty("snapshot", SNAPSHOT);
        json.addProperty("schemaVersion", 1);
        json.addProperty("family", FAMILY);
        json.addProperty("source", SOURCE);
        json.addProperty("minecraftVersion", "26.2");
        json.addProperty("compatibilityMarker", PhaseAFishingRodHookedCertification.COMPATIBILITY_MARKER);
        json.addProperty("catalogFingerprint", fingerprint);
        json.addProperty("runId", runId);
        json.addProperty("runMode", runMode);
        return json;
    }

    private static void validateRunState(JsonObject state, String runMode, String fingerprint) {
        validateArtifactHeader(state, runMode, fingerprint, required(state, "runId"));
        require(integer(state, "beginCount") == 1 && integer(state, "coordinatorCount") == 1, "Run-state was reset or has multiple coordinators");
    }

    private static void validateArtifactHeader(JsonObject artifact, String runMode, String fingerprint, String runId) {
        require("phase_a_fishing_rod_hooked_execution_evidence".equals(required(artifact, "snapshot")), "Snapshot mismatch");
        require(integer(artifact, "schemaVersion") == 1, "Schema mismatch");
        require(FAMILY.equals(required(artifact, "family")) && SOURCE.equals(required(artifact, "source")), "Family/source mismatch");
        require("26.2".equals(required(artifact, "minecraftVersion")), "Minecraft version mismatch");
        require(PhaseAFishingRodHookedCertification.COMPATIBILITY_MARKER.equals(required(artifact, "compatibilityMarker")), "Compatibility marker mismatch");
        require(fingerprint.equals(required(artifact, "catalogFingerprint")), "Catalog fingerprint mismatch");
        require(runMode.equals(required(artifact, "runMode")), "Run mode mismatch");
        require(runId.equals(required(artifact, "runId")), "Run ID mismatch");
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static void write(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
    }

    private static String key(JsonObject receipt) {
        return required(receipt, "advancementId") + "#" + required(receipt, "criterion");
    }

    private static String required(JsonObject json, String field) {
        require(json.has(field) && !json.get(field).isJsonNull() && !json.get(field).getAsString().isBlank(), "Missing " + field);
        return json.get(field).getAsString();
    }

    private static int integer(JsonObject json, String field) {
        require(json.has(field) && json.get(field).isJsonPrimitive() && json.get(field).getAsJsonPrimitive().isNumber(), "Missing integer " + field);
        return json.get(field).getAsInt();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

}
