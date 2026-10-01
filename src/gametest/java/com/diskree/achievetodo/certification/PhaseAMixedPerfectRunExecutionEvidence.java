package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class PhaseAMixedPerfectRunExecutionEvidence {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static JsonObject active;
    private static Map<String, JsonObject> rows;
    private PhaseAMixedPerfectRunExecutionEvidence() { }
    public static Path root() { return Path.of(Objects.requireNonNull(System.getProperty("achievetodo.phaseA.projectRoot"))).toAbsolutePath().normalize(); }
    public static String begin(Path root, boolean exact) throws Exception {
        if (Files.exists(root.resolve(PhaseAMixedPerfectRunCertification.PERSISTENT))) throw new IllegalStateException("Refusing CLOSED family rerun");
        var cases = PhaseAMixedPerfectRunCertification.cases(root); rows = new LinkedHashMap<>();
        var canary = PhaseAMixedPerfectRunEvidenceValidation.canaryKeys(cases);
        for (var row : cases) { var key = PhaseAMixedPerfectRunCertification.key(row); if (exact || canary.contains(key)) rows.put(key, row); }
        active = new JsonObject(); active.addProperty("snapshot", "phase_a_mixed_perfect_run_execution_evidence"); active.addProperty("schemaVersion", 1);
        active.addProperty("family", PhaseAMixedPerfectRunCertification.FAMILY); active.addProperty("source", PhaseAMixedPerfectRunCertification.SOURCE);
        active.addProperty("minecraftVersion", "26.2"); active.addProperty("compatibilityMarker", "compat_26_2_r15"); active.addProperty("catalogFingerprint", PhaseAMixedPerfectRunEvidenceValidation.fingerprint(root));
        active.addProperty("runId", UUID.randomUUID().toString()); active.addProperty("runMode", exact ? "EXACT" : "DIAGNOSTIC"); active.addProperty("startedAt", Instant.now().toString());
        JsonArray keys = new JsonArray(); for (var key : rows.keySet()) keys.add(key); active.add("selectedKeys", keys);
        write(root.resolve(PhaseAMixedPerfectRunEvidenceValidation.RUN_STATE), active);
        active.add("entries", new JsonArray()); write(root.resolve(PhaseAMixedPerfectRunEvidenceValidation.TEMP), active);
        return active.get("runId").getAsString();
    }
    public static List<JsonObject> selected() { return new ArrayList<>(rows.values()); }
    public static void append(Path root, JsonObject receipt) throws Exception {
        String runId = active.get("runId").getAsString(), hash = active.get("catalogFingerprint").getAsString();
        var state = PhaseAMixedPerfectRunEvidenceValidation.read(root.resolve(PhaseAMixedPerfectRunEvidenceValidation.RUN_STATE));
        if (!runId.equals(state.get("runId").getAsString())) throw new IllegalStateException("Competing evidence writer");
        String key = PhaseAMixedPerfectRunCertification.key(receipt);
        PhaseAMixedPerfectRunEvidenceValidation.validateReceipt(receipt, rows.get(key), runId, hash);
        for (var old : active.getAsJsonArray("entries")) if (PhaseAMixedPerfectRunCertification.key(old.getAsJsonObject()).equals(key)) throw new IllegalStateException("Duplicate receipt");
        active.getAsJsonArray("entries").add(receipt); active.addProperty("generatedAt", Instant.now().toString());
        write(root.resolve(PhaseAMixedPerfectRunEvidenceValidation.TEMP), active);
    }
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[1]).toAbsolutePath().normalize(); String mode = args[0];
        if (mode.equals("persistent")) { PhaseAMixedPerfectRunEvidenceValidation.loadValidatedRuntimeEvidence(root); return; }
        PhaseAMixedPerfectRunEvidenceValidation.load(root, PhaseAMixedPerfectRunEvidenceValidation.TEMP, !mode.equals("temporary"), true);
        if (mode.equals("promote")) { var target = root.resolve(PhaseAMixedPerfectRunCertification.PERSISTENT); if (Files.exists(target)) throw new IllegalStateException("Refusing persistent overwrite"); Files.copy(root.resolve(PhaseAMixedPerfectRunEvidenceValidation.TEMP), target); }
    }
    private static void write(Path path, JsonObject json) throws Exception { Files.createDirectories(path.getParent()); Files.writeString(path, GSON.toJson(json) + "\n"); }
}
