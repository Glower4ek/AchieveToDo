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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Isolated TEMP recorder and fail-closed promotion adapter for llama tame evidence. */
public final class PhaseATameAnimalLlamaExecutionEvidence {
    public static final Path TEMPORARY_ARTIFACT = Path.of("build", "tmp", "phase_a_certification",
        "tame_animal_llama_execution_evidence.json");
    public static final Path RUN_STATE_ARTIFACT = Path.of("build", "tmp", "phase_a_certification",
        "tame_animal_llama_execution_evidence.run.json");
    public static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification",
        "tame_animal_llama_execution_evidence.json");
    public static final String FAMILY = "TAME_ANIMAL_LLAMA";
    public static final String SOURCE = "PhaseATameAnimalLlamaGameTest";
    public static final String SNAPSHOT = "phase_a_tame_animal_llama_execution_evidence";
    public static final String PROJECT_ROOT_PROPERTY = "achievetodo.phaseA.projectRoot";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    private PhaseATameAnimalLlamaExecutionEvidence() { }

    public static void main(String[] args) throws Exception {
        Path root = args.length > 1 ? Path.of(args[1]).toAbsolutePath().normalize() : projectRoot();
        if (args.length > 0 && "promote".equals(args[0])) {
            promote(root);
            return;
        }
        if (args.length > 0 && "temporary".equals(args[0])) {
            loadTemporary(root, false);
            return;
        }
        if (args.length > 0 && "promotable".equals(args[0])) {
            loadTemporary(root, true);
            return;
        }
        if (args.length > 0 && "persistent".equals(args[0])) {
            loadPersistent(root);
            return;
        }
        throw new IllegalArgumentException("Expected promote, temporary, promotable, or persistent");
    }

    public static Path projectRoot() {
        String configured = System.getProperty(PROJECT_ROOT_PROPERTY);
        if (configured == null || configured.isBlank()) {
            throw new IllegalStateException("Missing " + PROJECT_ROOT_PROPERTY);
        }
        return Path.of(configured).toAbsolutePath().normalize();
    }

    public static String beginRun(Path root) throws IOException {
        String runId = UUID.randomUUID().toString();
        String fingerprint = catalogFingerprint(root);
        JsonObject state = common(runId, fingerprint);
        state.addProperty("startedAt", Instant.now().toString());
        write(root.resolve(RUN_STATE_ARTIFACT), state);
        JsonObject temporary = common(runId, fingerprint);
        temporary.addProperty("generatedAt", Instant.now().toString());
        temporary.add("entries", new JsonArray());
        write(root.resolve(TEMPORARY_ARTIFACT), temporary);
        return runId;
    }

    public static String currentRunId(Path root) throws IOException {
        JsonObject state = read(root.resolve(RUN_STATE_ARTIFACT));
        validateCommon(state, catalogFingerprint(root));
        return required(state, "runId");
    }

    public static void recordGreen(Path root, JsonObject receipt) throws IOException {
        String runId = currentRunId(root);
        String fingerprint = catalogFingerprint(root);
        JsonObject artifact = read(root.resolve(TEMPORARY_ARTIFACT));
        validateCommon(artifact, fingerprint);
        if (!runId.equals(required(artifact, "runId"))) throw new IllegalStateException("TEMP runId collision");
        validateReceipt(receipt, runId, fingerprint);
        JsonArray entries = array(artifact, "entries");
        String key = key(receipt);
        for (JsonElement element : entries) {
            if (key(element.getAsJsonObject()).equals(key)) throw new IllegalStateException("Duplicate llama receipt " + key);
        }
        List<JsonObject> ordered = new ArrayList<>();
        entries.forEach(element -> ordered.add(element.getAsJsonObject()));
        ordered.add(receipt.deepCopy());
        ordered.sort(Comparator.comparing(PhaseATameAnimalLlamaExecutionEvidence::key));
        JsonArray sorted = new JsonArray();
        ordered.forEach(sorted::add);
        artifact.add("entries", sorted);
        write(root.resolve(TEMPORARY_ARTIFACT), artifact);
    }

    public static Artifact loadTemporary(Path root, boolean exact) throws IOException {
        String runId = currentRunId(root);
        Artifact artifact = parse(read(root.resolve(TEMPORARY_ARTIFACT)), catalogFingerprint(root));
        if (!runId.equals(artifact.runId())) throw new IllegalStateException("TEMP runId mismatch");
        validateArtifact(artifact, exact, catalogFingerprint(root));
        return artifact;
    }

    public static Artifact loadPersistent(Path root) throws IOException {
        Artifact artifact = parse(read(root.resolve(PERSISTENT_ARTIFACT)), catalogFingerprint(root));
        validateArtifact(artifact, true, catalogFingerprint(root));
        return artifact;
    }

    public static void promote(Path root) throws IOException {
        loadTemporary(root, true);
        Path target = root.resolve(PERSISTENT_ARTIFACT);
        if (Files.exists(target)) throw new IllegalStateException("Refusing to overwrite persistent llama evidence");
        Files.createDirectories(target.getParent());
        Files.copy(root.resolve(TEMPORARY_ARTIFACT), target, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private static Artifact parse(JsonObject json, String fingerprint) {
        validateCommon(json, fingerprint);
        required(json, "generatedAt");
        List<JsonObject> entries = new ArrayList<>();
        array(json, "entries").forEach(element -> {
            if (!element.isJsonObject()) throw new IllegalStateException("Non-object llama evidence receipt");
            entries.add(element.getAsJsonObject());
        });
        return new Artifact(required(json, "runId"), List.copyOf(entries));
    }

    private static void validateArtifact(Artifact artifact, boolean exact, String fingerprint) {
        Set<String> expected = new LinkedHashSet<>();
        for (PhaseATameAnimalLlamaCertification.Case definition : PhaseATameAnimalLlamaCertification.CASES) expected.add(definition.key());
        Set<String> actual = new LinkedHashSet<>();
        for (JsonObject receipt : artifact.entries()) {
            if (!actual.add(key(receipt))) throw new IllegalStateException("Duplicate llama evidence key " + key(receipt));
            validateReceipt(receipt, artifact.runId(), fingerprint);
        }
        if (exact && !expected.equals(actual)) {
            throw new IllegalStateException("Llama evidence exact coverage mismatch expected=" + expected + " actual=" + actual);
        }
    }

    private static void validateReceipt(JsonObject receipt, String runId, String fingerprint) {
        String receiptKey = key(receipt);
        PhaseATameAnimalLlamaCertification.Case definition = PhaseATameAnimalLlamaCertification.CASES.stream()
            .filter(candidate -> candidate.key().equals(receiptKey)).findFirst()
            .orElseThrow(() -> new IllegalStateException("Unknown llama receipt " + receiptKey));
        requiredEquals(receipt, "family", FAMILY);
        requiredEquals(receipt, "source", SOURCE);
        if (fingerprint != null) requiredEquals(receipt, "catalogFingerprint", fingerprint);
        requiredEquals(receipt, "runId", runId);
        requiredEquals(receipt, "result", "GREEN");
        requiredEquals(receipt, "trigger", PhaseATameAnimalLlamaCertification.TRIGGER);
        requiredEquals(receipt, "boundary", PhaseATameAnimalLlamaCertification.BOUNDARY);
        requiredEquals(receipt, "packetPath", PhaseATameAnimalLlamaCertification.PACKET_PATH);
        requiredEquals(receipt, "gameMode", "SURVIVAL");
        requiredBoolean(receipt, "joined", true);
        requiredBoolean(receipt, "connectionRegistered", true);
        requiredBoolean(receipt, "clientLoaded", true);
        requiredBoolean(receipt, "normalScheduler", true);
        requiredBoolean(receipt, "criterionBefore", false);
        requiredBoolean(receipt, "criterionAfter", true);
        requiredBoolean(receipt, "nativeTameTransition", true);
        requiredBoolean(receipt, "noDirectCriterionTrigger", true);
        requiredBoolean(receipt, "noManualAward", true);
        requiredBoolean(receipt, "untamedBefore", true);
        requiredBoolean(receipt, "tamedAfter", true);
        requiredEquals(receipt, "observedEntityType", "minecraft:llama");
        if (integer(receipt, "ticksToCriterion") < 1 || integer(receipt, "ticksToCriterion") > 600) {
            throw new IllegalStateException("Unbounded llama tame wait for " + receiptKey);
        }
        JsonObject predicate = object(receipt, "predicateObservation");
        requiredEquals(predicate, "kind", definition.predicateKind());
        requiredBoolean(predicate, "satisfied", true);
        if ("strength".equals(definition.predicateKind())) requiredInt(predicate, "strength", definition.predicateValue());
        if ("variant".equals(definition.predicateKind())) requiredEquals(predicate, "variant", definition.variant());
        JsonObject cleanup = object(receipt, "cleanup");
        requiredBoolean(cleanup, "playerRemoved", true);
        requiredBoolean(cleanup, "connectionRemoved", true);
        requiredBoolean(cleanup, "channelSettled", true);
        requiredInt(cleanup, "warningCount", 0);
    }

    private static JsonObject common(String runId, String fingerprint) {
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", SNAPSHOT);
        root.addProperty("schemaVersion", 1);
        root.addProperty("family", FAMILY);
        root.addProperty("source", SOURCE);
        root.addProperty("minecraftVersion", "26.2");
        root.addProperty("compatibilityMarker", PhaseATameAnimalLlamaCertification.COMPATIBILITY_MARKER);
        root.addProperty("catalogFingerprint", fingerprint);
        root.addProperty("runId", runId);
        return root;
    }

    private static void validateCommon(JsonObject json, String fingerprint) {
        requiredEquals(json, "snapshot", SNAPSHOT);
        requiredInt(json, "schemaVersion", 1);
        requiredEquals(json, "family", FAMILY);
        requiredEquals(json, "source", SOURCE);
        requiredEquals(json, "minecraftVersion", "26.2");
        requiredEquals(json, "compatibilityMarker", PhaseATameAnimalLlamaCertification.COMPATIBILITY_MARKER);
        requiredEquals(json, "catalogFingerprint", fingerprint);
        required(json, "runId");
    }

    private static String catalogFingerprint(Path root) throws IOException {
        try {
            return "sha-256:" + java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(root.resolve(PhaseATameAnimalLlamaCertification.SNAPSHOT))));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static String key(JsonObject receipt) { return required(receipt, "advancementId") + "#" + required(receipt, "criterion"); }
    private static JsonObject read(Path path) throws IOException { return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject(); }
    private static void write(Path path, JsonObject json) throws IOException { Files.createDirectories(path.getParent()); Files.writeString(path, GSON.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8); }
    private static JsonArray array(JsonObject json, String field) { if (!json.has(field) || !json.get(field).isJsonArray()) throw new IllegalStateException("Missing array " + field); return json.getAsJsonArray(field); }
    private static JsonObject object(JsonObject json, String field) { if (!json.has(field) || !json.get(field).isJsonObject()) throw new IllegalStateException("Missing object " + field); return json.getAsJsonObject(field); }
    private static String required(JsonObject json, String field) { if (!json.has(field) || json.get(field).isJsonNull() || json.get(field).getAsString().isBlank()) throw new IllegalStateException("Missing " + field); return json.get(field).getAsString(); }
    private static void requiredEquals(JsonObject json, String field, String expected) { if (!expected.equals(required(json, field))) throw new IllegalStateException("Expected " + field + "=" + expected); }
    private static int integer(JsonObject json, String field) { if (!json.has(field) || !json.get(field).isJsonPrimitive()) throw new IllegalStateException("Missing integer " + field); return json.get(field).getAsInt(); }
    private static void requiredInt(JsonObject json, String field, int expected) { if (integer(json, field) != expected) throw new IllegalStateException("Expected " + field + "=" + expected); }
    private static void requiredBoolean(JsonObject json, String field, boolean expected) { if (!json.has(field) || !json.get(field).isJsonPrimitive() || json.get(field).getAsBoolean() != expected) throw new IllegalStateException("Expected " + field + "=" + expected); }

    public record Artifact(String runId, List<JsonObject> entries) { }
}
