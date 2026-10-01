package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Artifact-local independent parser; exact coverage is a set of frozen requirement witnesses. */
public final class PhaseAMixedPiglinDistractionEvidenceValidation {
    public static final Path TEMP = Path.of("build/tmp/phase_a_certification/mixed_piglin_distraction_execution_evidence.json");
    public static final Path RUN_STATE = Path.of("build/tmp/phase_a_certification/mixed_piglin_distraction_execution_evidence.run.json");
    private PhaseAMixedPiglinDistractionEvidenceValidation() { }
    public static JsonObject load(Path root, Path path, boolean exact, boolean active) throws Exception {
        var cases = PhaseAMixedPiglinDistractionCertification.cases(root);
        var expected = new LinkedHashMap<String, JsonObject>();
        for (var row : cases) expected.put(PhaseAMixedPiglinDistractionCertification.key(row), row);
        var artifact = read(root.resolve(path)); String hash = fingerprint(root);
        common(artifact, hash); String runId = string(artifact, "runId");
        if (active) { var state = read(root.resolve(RUN_STATE)); common(state, hash); require(runId.equals(string(state, "runId")), "Active run-state mismatch"); }
        String mode = string(artifact, "runMode"); require(mode.equals("EXACT") || mode.equals("DIAGNOSTIC"), "Unknown run mode");
        require(!exact || mode.equals("EXACT"), "Canary cannot be promoted");
        var allowed = mode.equals("EXACT") ? expected.keySet() : canaryKeys(cases);
        var selected = new TreeSet<String>(); for (var key : artifact.getAsJsonArray("selectedKeys")) require(selected.add(key.getAsString()), "Duplicate selected key");
        require(selected.equals(new TreeSet<>(allowed)), "Selected scope mismatch");
        var keys = new HashSet<String>(); var players = new HashSet<String>();
        for (var entry : artifact.getAsJsonArray("entries")) {
            JsonObject receipt = entry.getAsJsonObject(); String key = PhaseAMixedPiglinDistractionCertification.key(receipt);
            require(allowed.contains(key) && keys.add(key), "Extra or duplicate receipt");
            validateReceipt(receipt, expected.get(key), runId, hash);
            require(players.add(string(receipt, "playerUuid")), "Player reused across cases");
        }
        require(keys.equals(new HashSet<>(allowed)), "Missing required receipts"); return artifact;
    }
    public static Set<String> canaryKeys(List<JsonObject> cases) {
        require(cases.size()==1,"Wrong canary scope");return Set.of(PhaseAMixedPiglinDistractionCertification.key(cases.getFirst()));
    }
    public static void validateReceipt(JsonObject r,JsonObject row,String runId,String hash) {
        require(row!=null,"Unknown receipt");
        require(string(r,"family").equals(PhaseAMixedPiglinDistractionCertification.FAMILY)&&string(r,"source").equals(PhaseAMixedPiglinDistractionCertification.SOURCE),"Ownership mismatch");
        require(string(r,"runId").equals(runId)&&string(r,"catalogFingerprint").equals(hash),"Run/hash mismatch");
        require(string(r,"result").equals("GREEN")&&string(r,"gameMode").equals("SURVIVAL"),"Result/mode mismatch");UUID.fromString(string(r,"playerUuid"));
        for(String field:new String[]{"advancementId","criterion"})require(string(r,field).equals(string(row,field)),"Key mismatch");
        require(integer(r,"requirementGroup")==0&&string(r,"trigger").equals("minecraft:player_interacted_with_entity")&&string(r,"boundary").equals(PhaseAMixedPiglinDistractionCertification.BOUNDARY),"Native boundary mismatch");
        for(String field:new String[]{"joined","connectionRegistered","clientLoaded","finiteMaterials","notSpectator","adultPiglin","itemTagMatched","armorEmptyBefore","armorEmptyAfter","packetSent","criterionAfter","noDirectCriterionTrigger","noManualAward"})require(bool(r,field),"Missing witness "+field);
        require(!bool(r,"criterionBefore")&&string(r,"observedItem").equals("minecraft:gold_ingot")&&string(r,"entityType").equals("minecraft:piglin"),"Wrong native witness");
        UUID.fromString(string(r,"targetUuid"));require(!string(r,"targetUuid").equals(string(r,"playerUuid")),"Wrong target identity");
        require(integer(r,"playerItemBefore")==1&&integer(r,"playerItemAfter")==0&&integer(r,"piglinItemBefore")==0&&integer(r,"piglinItemAfter")==1&&bool(r,"admiringItem"),"Finite native gold transfer missing");
        var cleanup=r.getAsJsonObject("cleanup");for(String field:new String[]{"playerRemoved","connectionRemoved","channelSettled","fixtureEntitiesRemoved"})require(bool(cleanup,field),"Cleanup missing");
        require(integer(cleanup,"warningCount")==0&&integer(cleanup,"settlementMessages")>=0,"Cleanup warning debt");
    }
    public static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadValidatedRuntimeEvidence(Path root) throws java.io.IOException {
        try { return loadPersistent(root); }
        catch (Exception failure) { throw new java.io.IOException("Invalid persistent piglin evidence", failure); }
    }
    private static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadPersistent(Path root) throws Exception {
        JsonObject artifact = load(root, PhaseAMixedPiglinDistractionCertification.PERSISTENT, true, false);
        Map<String, Set<String>> criteria = new TreeMap<>();
        for (var entry : artifact.getAsJsonArray("entries")) { var receipt = entry.getAsJsonObject(); criteria.computeIfAbsent(string(receipt, "advancementId"), key -> new TreeSet<>()).add(string(receipt, "criterion")); }
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> result = new LinkedHashMap<>();
        for (var entry : criteria.entrySet()) result.put(entry.getKey(), new PhaseAAdvancementRollup.RuntimeEvidence(entry.getKey(), entry.getValue(),
            Map.of(PhaseAMixedPiglinDistractionCertification.FAMILY, entry.getValue()), Map.of(PhaseAMixedPiglinDistractionCertification.SOURCE, entry.getValue()),
            Set.of(PhaseAMixedPiglinDistractionCertification.FAMILY), Set.of(PhaseAMixedPiglinDistractionCertification.SOURCE)));
        return result;
    }
    public static String fingerprint(Path root) throws Exception { return "sha-256:" + PhaseAMixedPiglinDistractionCertification.sha(Files.readAllBytes(root.resolve(PhaseAMixedPiglinDistractionCertification.CATALOG))); }
    public static void common(JsonObject artifact, String hash) {
        require(string(artifact, "snapshot").equals("phase_a_mixed_piglin_distraction_execution_evidence") && integer(artifact, "schemaVersion") == 1, "Schema mismatch");
        require(string(artifact, "family").equals(PhaseAMixedPiglinDistractionCertification.FAMILY) && string(artifact, "source").equals(PhaseAMixedPiglinDistractionCertification.SOURCE), "Artifact ownership mismatch");
        require(string(artifact, "minecraftVersion").equals("26.2") && string(artifact, "compatibilityMarker").equals("compat_26_2_r15"), "Compatibility mismatch");
        require(string(artifact, "catalogFingerprint").equals(hash), "Catalog changed"); UUID.fromString(string(artifact, "runId"));
    }
    public static JsonObject read(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
    private static String string(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && !object.get(key).getAsString().isBlank(), "Missing " + key); return object.get(key).getAsString(); }
    private static int integer(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber(), "Missing integer " + key); return object.get(key).getAsInt(); }
    private static boolean bool(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isBoolean(), "Missing boolean " + key); return object.get(key).getAsBoolean(); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
