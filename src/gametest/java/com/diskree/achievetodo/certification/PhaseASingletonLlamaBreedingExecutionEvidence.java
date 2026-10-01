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

/** Isolated run-state and receipt recorder for animal/so_i_got_that_going_for_me. */
public final class PhaseASingletonLlamaBreedingExecutionEvidence {
    public static final Path TEMP = Path.of("build/tmp/phase_a_certification/singleton_llama_breeding_execution_evidence.json");
    public static final Path RUN_STATE = Path.of("build/tmp/phase_a_certification/singleton_llama_breeding_execution_evidence.run.json");
    private static final String SNAPSHOT = "phase_a_singleton_llama_breeding_execution_evidence";
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private PhaseASingletonLlamaBreedingExecutionEvidence() { }
    public static Path projectRoot() {
        String property = System.getProperty("achievetodo.phaseA.projectRoot");
        require(property != null && !property.isBlank(), "Missing project root");
        return Path.of(property).toAbsolutePath().normalize();
    }
    public static void main(String[] args) throws Exception {
        Path root = args.length > 1 ? Path.of(args[1]).toAbsolutePath().normalize() : projectRoot();
        if (args.length > 0 && "promote".equals(args[0])) { promote(root); return; }
        if (args.length > 0 && "temporary".equals(args[0])) { loadTemporary(root, false); return; }
        if (args.length > 0 && "promotable".equals(args[0])) { loadTemporary(root, true); return; }
        if (args.length > 0 && "persistent".equals(args[0])) { loadPersistent(root); return; }
        throw new IllegalArgumentException("Expected promote, temporary, promotable, or persistent");
    }
    public static String beginRun(Path root) throws IOException {
        String runId = UUID.randomUUID().toString(), hash = fingerprint(root);
        JsonObject state = common(runId, hash); state.addProperty("startedAt", Instant.now().toString());
        JsonObject artifact = common(runId, hash); artifact.addProperty("generatedAt", Instant.now().toString()); artifact.add("entries", new JsonArray());
        write(root.resolve(RUN_STATE), state); write(root.resolve(TEMP), artifact); return runId;
    }
    public static String currentFingerprint(Path root) throws IOException { return fingerprint(root); }
    public static void recordGreen(Path root, JsonObject receipt) throws IOException {
        JsonObject state = read(root.resolve(RUN_STATE)), artifact = read(root.resolve(TEMP));
        String hash = fingerprint(root), runId = string(state, "runId");
        validateCommon(state, hash); validateCommon(artifact, hash);
        require(runId.equals(string(artifact, "runId")), "run-state/TEMP mismatch");
        JsonArray entries = artifact.getAsJsonArray("entries"); require(entries != null && entries.isEmpty(), "singleton already recorded");
        validateReceipt(receipt, runId, hash); entries.add(receipt.deepCopy()); artifact.addProperty("generatedAt", Instant.now().toString());
        write(root.resolve(TEMP), artifact);
    }
    public static Artifact loadTemporary(Path root, boolean exact) throws IOException {
        String hash = fingerprint(root); JsonObject state = read(root.resolve(RUN_STATE)); validateCommon(state, hash);
        Artifact artifact = parse(read(root.resolve(TEMP)), hash, exact);
        require(string(state, "runId").equals(artifact.runId()), "run-state/TEMP collision"); return artifact;
    }
    public static Artifact loadPersistent(Path root) throws IOException {
        return parse(read(root.resolve(PhaseASingletonLlamaBreedingCertification.PERSISTENT)), fingerprint(root), true);
    }
    public static void promote(Path root) throws IOException {
        loadTemporary(root, true);
        Path target = root.resolve(PhaseASingletonLlamaBreedingCertification.PERSISTENT);
        require(!Files.exists(target), "Refusing persistent overwrite");
        Files.createDirectories(target.getParent()); Files.copy(root.resolve(TEMP), target, StandardCopyOption.COPY_ATTRIBUTES);
    }
    private static Artifact parse(JsonObject json, String hash, boolean exact) {
        validateCommon(json, hash); string(json, "generatedAt");
        JsonArray entries = json.getAsJsonArray("entries"); require(entries != null && entries.size() <= 1, "wrong singleton cardinality");
        if (exact) require(entries.size() == 1, "missing exact singleton receipt");
        if (!entries.isEmpty()) validateReceipt(entries.get(0).getAsJsonObject(), string(json, "runId"), hash);
        return new Artifact(string(json, "runId"), entries.size());
    }
    private static void validateReceipt(JsonObject r, String runId, String hash) {
        require(PhaseASingletonLlamaBreedingCertification.FAMILY.equals(string(r, "family"))
            && PhaseASingletonLlamaBreedingCertification.SOURCE.equals(string(r, "source")), "receipt ownership mismatch");
        require(PhaseASingletonLlamaBreedingCertification.ADVANCEMENT.equals(string(r, "advancementId"))
            && PhaseASingletonLlamaBreedingCertification.CRITERION.equals(string(r, "criterion")), "criterion mismatch");
        require("minecraft:bred_animals".equals(string(r, "trigger"))
            && PhaseASingletonLlamaBreedingCertification.BOUNDARY.equals(string(r, "boundary")), "native boundary mismatch");
        require(PhaseASingletonLlamaBreedingCertification.ENTITY_TAG.equals(string(r, "checkedParentTag"))
            && PhaseASingletonLlamaBreedingCertification.ENTITY_TAG.equals(string(r, "checkedPartnerTag"))
            && PhaseASingletonLlamaBreedingCertification.SELECTED_ENTITY.equals(string(r, "observedParent"))
            && PhaseASingletonLlamaBreedingCertification.SELECTED_ENTITY.equals(string(r, "observedPartner"))
            && PhaseASingletonLlamaBreedingCertification.SELECTED_ENTITY.equals(string(r, "observedChild"))
            && PhaseASingletonLlamaBreedingCertification.FEED_ITEM.equals(string(r, "observedFeedItem")), "entity/feed predicate mismatch");
        require(runId.equals(string(r, "runId")) && hash.equals(string(r, "catalogFingerprint")), "run/fingerprint mismatch");
        require("GREEN".equals(string(r, "result")) && "SURVIVAL".equals(string(r, "gameMode")), "result/mode mismatch");
        UUID.fromString(string(r, "playerUuid")); UUID.fromString(string(r, "parentUuid"));
        UUID.fromString(string(r, "partnerUuid")); UUID.fromString(string(r, "childUuid"));
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "finiteMaterials",
            "parentTagMember", "partnerTagMember", "parentTamedBefore", "partnerTamedBefore", "parentAdultBefore",
            "partnerAdultBefore", "parentFedWithHay", "partnerFedWithHay", "parentInLoveAfterFeed",
            "partnerInLoveAfterFeed", "nativeChildSpawned", "parentCooldownAfterBreeding",
            "partnerCooldownAfterBreeding", "criterionAfter", "noDirectCriterionTrigger", "noManualAward"})
            require(bool(r, field), "missing native witness " + field);
        require(!bool(r, "criterionBefore") && integer(r, "hayConsumed") == 2
            && integer(r, "ticksToBreed") > 0 && integer(r, "ticksToBreed") <= 400, "transition/material bound mismatch");
        JsonObject gate = object(r, "productionGateWitness");
        require("LANDMARK_ONLY_FOR_LLAMA_FEEDING".equals(string(gate, "productionGate"))
            && !bool(gate, "parentLockedLandmark") && !bool(gate, "partnerLockedLandmark"), "production gate mismatch");
        JsonObject cleanup = object(r, "cleanup");
        require(bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled")
            && bool(cleanup, "parentRemoved") && bool(cleanup, "partnerRemoved") && bool(cleanup, "childRemoved")
            && integer(cleanup, "warningCount") == 0 && integer(cleanup, "settlementMessages") >= 0, "cleanup mismatch");
    }
    private static JsonObject common(String runId, String hash) {
        JsonObject json = new JsonObject(); json.addProperty("snapshot", SNAPSHOT); json.addProperty("schemaVersion", 1);
        json.addProperty("family", PhaseASingletonLlamaBreedingCertification.FAMILY);
        json.addProperty("source", PhaseASingletonLlamaBreedingCertification.SOURCE);
        json.addProperty("minecraftVersion", "26.2"); json.addProperty("compatibilityMarker", "compat_26_2_r15");
        json.addProperty("catalogFingerprint", hash); json.addProperty("runId", runId); return json;
    }
    private static void validateCommon(JsonObject json, String hash) {
        require(SNAPSHOT.equals(string(json, "snapshot")) && integer(json, "schemaVersion") == 1, "snapshot/schema mismatch");
        require(PhaseASingletonLlamaBreedingCertification.FAMILY.equals(string(json, "family"))
            && PhaseASingletonLlamaBreedingCertification.SOURCE.equals(string(json, "source")), "artifact ownership mismatch");
        require("26.2".equals(string(json, "minecraftVersion")) && "compat_26_2_r15".equals(string(json, "compatibilityMarker")), "version mismatch");
        require(hash.equals(string(json, "catalogFingerprint")), "catalog fingerprint mismatch"); UUID.fromString(string(json, "runId"));
    }
    private static String fingerprint(Path root) throws IOException {
        try { return "sha-256:" + java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(root.resolve(PhaseASingletonLlamaBreedingCertification.SNAPSHOT)))); }
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
