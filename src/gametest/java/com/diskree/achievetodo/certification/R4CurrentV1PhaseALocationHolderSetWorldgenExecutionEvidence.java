package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

public final class R4CurrentV1PhaseALocationHolderSetWorldgenExecutionEvidence {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static JsonObject active;
    private static Map<String, JsonObject> rows;
    private R4CurrentV1PhaseALocationHolderSetWorldgenExecutionEvidence() { }
    public static Path root() { return Path.of(Objects.requireNonNull(System.getProperty("achievetodo.phaseA.projectRoot"))).toAbsolutePath().normalize(); }
    public static String begin(Path root, boolean exact) throws Exception {
        if (R4CurrentRunGuard.claim("PhaseALocationHolderSetWorldgenGameTest")) throw new IllegalStateException("Refusing CLOSED family rerun");
        var cases = PhaseALocationHolderSetWorldgenCertification.cases(root); rows = new LinkedHashMap<>();
        var canary = PhaseALocationHolderSetWorldgenEvidenceValidation.canaryKeys(cases);
        for (var row : cases) { var key = PhaseALocationHolderSetWorldgenCertification.key(row); if (exact || canary.contains(key)) rows.put(key, row); }
        active = new JsonObject(); active.addProperty("snapshot", "phase_a_location_holder_set_worldgen_execution_evidence"); active.addProperty("schemaVersion", 1);
        active.addProperty("family", PhaseALocationHolderSetWorldgenCertification.FAMILY); active.addProperty("source", PhaseALocationHolderSetWorldgenCertification.SOURCE);
        active.addProperty("minecraftVersion", "26.2"); active.addProperty("compatibilityMarker", "compat_26_2_r15"); active.addProperty("catalogFingerprint", PhaseALocationHolderSetWorldgenEvidenceValidation.fingerprint(root));
        active.addProperty("runId", UUID.randomUUID().toString()); active.addProperty("runMode", exact ? "EXACT" : "DIAGNOSTIC"); active.addProperty("startedAt", Instant.now().toString());
        JsonArray keys = new JsonArray(); for (var key : rows.keySet()) keys.add(key); active.add("selectedKeys", keys);
        write(root.resolve(PhaseALocationHolderSetWorldgenEvidenceValidation.RUN_STATE), active);
        active.add("entries", new JsonArray()); write(root.resolve(PhaseALocationHolderSetWorldgenEvidenceValidation.TEMP), active);
        return active.get("runId").getAsString();
    }
    public static List<JsonObject> selected() { return new ArrayList<>(rows.values()); }
    public static void append(Path root, JsonObject receipt) throws Exception {
        String runId = active.get("runId").getAsString(), hash = active.get("catalogFingerprint").getAsString();
        var state = PhaseALocationHolderSetWorldgenEvidenceValidation.read(root.resolve(PhaseALocationHolderSetWorldgenEvidenceValidation.RUN_STATE));
        if (!runId.equals(state.get("runId").getAsString())) throw new IllegalStateException("Competing evidence writer");
        String key = PhaseALocationHolderSetWorldgenCertification.key(receipt);
        R4CurrentLocationContract.validateReceipt(receipt, rows.get(key), runId, hash);
        for (var old : active.getAsJsonArray("entries")) if (PhaseALocationHolderSetWorldgenCertification.key(old.getAsJsonObject()).equals(key)) throw new IllegalStateException("Duplicate receipt");
        active.getAsJsonArray("entries").add(receipt); active.addProperty("generatedAt", Instant.now().toString());
        write(root.resolve(PhaseALocationHolderSetWorldgenEvidenceValidation.TEMP), active);
    }
    public static void main(String[] args) throws Exception { throw new IllegalStateException("R4 current-only recorder has no historical promotion authority"); }
    private static void write(Path path, JsonObject json) throws Exception { Files.createDirectories(path.getParent()); Files.writeString(path, GSON.toJson(json) + "\n"); }
}
