package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.UUID;

/** Isolated runtime recorder for the Meadows jukebox singleton. */
public final class PhaseAItemUsedOnBlockContextualExecutionEvidence {
    public static final Path TEMP = Path.of("build/tmp/phase_a_certification/item_used_on_block_contextual_execution_evidence.json");
    public static final Path RUN_STATE = Path.of("build/tmp/phase_a_certification/item_used_on_block_contextual_execution_evidence.run.json");
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private static final String SNAPSHOT = "phase_a_item_used_on_block_contextual_execution_evidence";
    private PhaseAItemUsedOnBlockContextualExecutionEvidence() { }

    public static void main(String[] args) throws Exception {
        Path root = args.length > 1 ? Path.of(args[1]).toAbsolutePath().normalize() : projectRoot();
        if (args.length > 0 && "promote".equals(args[0])) { promote(root); return; }
        if (args.length > 0 && "temporary".equals(args[0])) { loadTemporary(root, false); return; }
        if (args.length > 0 && "promotable".equals(args[0])) { loadTemporary(root, true); return; }
        if (args.length > 0 && "persistent".equals(args[0])) { loadPersistent(root); return; }
        throw new IllegalArgumentException("Expected promote, temporary, promotable, or persistent");
    }
    public static Path projectRoot() {
        String property = System.getProperty("achievetodo.phaseA.projectRoot");
        if (property == null || property.isBlank()) throw new IllegalStateException("Missing achievetodo.phaseA.projectRoot");
        return Path.of(property).toAbsolutePath().normalize();
    }
    public static String beginRun(Path root) throws IOException {
        String runId = UUID.randomUUID().toString(), fingerprint = fingerprint(root);
        JsonObject state = common(runId, fingerprint); state.addProperty("startedAt", Instant.now().toString());
        JsonObject artifact = common(runId, fingerprint); artifact.addProperty("generatedAt", Instant.now().toString()); artifact.add("entries", new JsonArray());
        write(root.resolve(RUN_STATE), state); write(root.resolve(TEMP), artifact); return runId;
    }
    public static String currentFingerprint(Path root) throws IOException { return fingerprint(root); }
    public static void recordGreen(Path root, JsonObject receipt) throws IOException {
        JsonObject state = read(root.resolve(RUN_STATE)), artifact = read(root.resolve(TEMP));
        String fingerprint = fingerprint(root), runId = string(state, "runId");
        validateCommon(state, fingerprint); validateCommon(artifact, fingerprint);
        require(runId.equals(string(artifact, "runId")), "run-state/TEMP collision");
        validateReceipt(receipt, runId, fingerprint);
        JsonArray entries = artifact.getAsJsonArray("entries"); require(entries != null && entries.isEmpty(), "singleton receipt already recorded");
        entries.add(receipt.deepCopy()); artifact.addProperty("generatedAt", Instant.now().toString());
        write(root.resolve(TEMP), artifact);
    }
    public static Artifact loadTemporary(Path root, boolean exact) throws IOException {
        String fingerprint = fingerprint(root); JsonObject state = read(root.resolve(RUN_STATE)); validateCommon(state, fingerprint);
        Artifact artifact = parse(read(root.resolve(TEMP)), fingerprint, exact);
        require(string(state, "runId").equals(artifact.runId()), "run-state/TEMP mismatch"); return artifact;
    }
    public static Artifact loadPersistent(Path root) throws IOException {
        return parse(read(root.resolve(PhaseAItemUsedOnBlockContextualCertification.PERSISTENT_EVIDENCE)), fingerprint(root), true);
    }
    public static void promote(Path root) throws IOException {
        loadTemporary(root, true);
        Path target = root.resolve(PhaseAItemUsedOnBlockContextualCertification.PERSISTENT_EVIDENCE);
        require(!Files.exists(target), "Refusing to overwrite persistent contextual evidence");
        Files.createDirectories(target.getParent()); Files.copy(root.resolve(TEMP), target, StandardCopyOption.COPY_ATTRIBUTES);
    }
    private static Artifact parse(JsonObject json, String fingerprint, boolean exact) {
        validateCommon(json, fingerprint); string(json, "generatedAt"); JsonArray entries = json.getAsJsonArray("entries");
        require(entries != null && entries.size() <= 1, "wrong singleton receipt cardinality");
        if (exact) require(entries.size() == 1, "missing exact singleton receipt");
        if (!entries.isEmpty()) validateReceipt(entries.get(0).getAsJsonObject(), string(json, "runId"), fingerprint);
        return new Artifact(string(json, "runId"), entries.size());
    }
    private static void validateReceipt(JsonObject receipt, String runId, String fingerprint) {
        require(PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID.equals(string(receipt, "advancementId")), "advancement mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.CRITERION.equals(string(receipt, "criterion")), "criterion mismatch");
        require("minecraft:item_used_on_block".equals(string(receipt, "trigger")), "trigger mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.BIOME.equals(string(receipt, "observedBiome")), "biome mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.BLOCK.equals(string(receipt, "observedBlock")), "block mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.DISC.equals(string(receipt, "observedDisc")), "disc mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.BOUNDARY.equals(string(receipt, "boundary")), "runtime boundary mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.FAMILY.equals(string(receipt, "family"))
            && PhaseAItemUsedOnBlockContextualCertification.SOURCE.equals(string(receipt, "source")), "ownership mismatch");
        require(runId.equals(string(receipt, "runId")) && fingerprint.equals(string(receipt, "catalogFingerprint")), "run/fingerprint mismatch");
        require("GREEN".equals(string(receipt, "result")) && "SURVIVAL".equals(string(receipt, "gameMode")), "result/game mode mismatch");
        UUID.fromString(string(receipt, "playerUuid"));
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "finiteMaterials", "toolPlayable", "jukeboxAcceptedDisc", "criterionAfter", "noDirectCriterionTrigger", "noManualAward"})
            require(bool(receipt, field), "false receipt precondition " + field);
        require(!bool(receipt, "criterionBefore") && integer(receipt, "discCountBefore") == 1 && integer(receipt, "discCountAfter") == 0,
            "criterion or disc mutation mismatch");
        JsonObject gate = object(receipt, "productionUnlockWitness");
        require("USE_JUKEBOX".equals(string(gate, "ability")) && "bac_advancements".equals(string(gate, "scoreboardObjective"))
            && bool(gate, "abilityLockedBefore") && !bool(gate, "abilityLockedAfter")
            && integer(gate, "scoreBefore") == 0 && integer(gate, "requiredThreshold") > 0
            && integer(gate, "scoreAfter") == integer(gate, "requiredThreshold"), "production gate mismatch");
        JsonObject cleanup = object(receipt, "cleanup");
        require(bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled")
            && integer(cleanup, "warningCount") == 0, "cleanup mismatch");
    }
    private static JsonObject common(String runId, String fingerprint) {
        JsonObject json = new JsonObject(); json.addProperty("snapshot", SNAPSHOT); json.addProperty("schemaVersion", 1);
        json.addProperty("family", PhaseAItemUsedOnBlockContextualCertification.FAMILY); json.addProperty("source", PhaseAItemUsedOnBlockContextualCertification.SOURCE);
        json.addProperty("minecraftVersion", "26.2"); json.addProperty("compatibilityMarker", "compat_26_2_r15");
        json.addProperty("catalogFingerprint", fingerprint); json.addProperty("runId", runId); return json;
    }
    private static void validateCommon(JsonObject json, String fingerprint) {
        require(SNAPSHOT.equals(string(json, "snapshot")) && integer(json, "schemaVersion") == 1, "snapshot/schema mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.FAMILY.equals(string(json, "family"))
            && PhaseAItemUsedOnBlockContextualCertification.SOURCE.equals(string(json, "source")), "ownership mismatch");
        require("26.2".equals(string(json, "minecraftVersion")) && "compat_26_2_r15".equals(string(json, "compatibilityMarker")), "version mismatch");
        require(fingerprint.equals(string(json, "catalogFingerprint")), "catalog fingerprint mismatch"); UUID.fromString(string(json, "runId"));
    }
    private static String fingerprint(Path root) throws IOException {
        try { return "sha-256:" + java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(root.resolve(PhaseAItemUsedOnBlockContextualCertification.SNAPSHOT)))); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    private static JsonObject read(Path path) throws IOException { return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject(); }
    private static void write(Path path, JsonObject json) throws IOException { Files.createDirectories(path.getParent()); Files.writeString(path, GSON.toJson(json) + "\n", StandardCharsets.UTF_8); }
    private static String string(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonPrimitive() && !o.get(f).getAsString().isBlank(), "missing " + f); return o.get(f).getAsString(); }
    private static int integer(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonPrimitive() && o.get(f).getAsJsonPrimitive().isNumber(), "missing integer " + f); return o.get(f).getAsInt(); }
    private static boolean bool(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonPrimitive() && o.get(f).getAsJsonPrimitive().isBoolean(), "missing boolean " + f); return o.get(f).getAsBoolean(); }
    private static JsonObject object(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonObject(), "missing object " + f); return o.getAsJsonObject(f); }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
    public record Artifact(String runId, int entryCount) { }
}
