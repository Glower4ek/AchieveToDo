package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Artifact-local independent parser; exact coverage is a set of frozen requirement witnesses. */
public final class PhaseAStackAllItemsEvidenceValidation {
    public static final Path TEMP = Path.of("build/tmp/phase_a_certification/stack_all_items_inventory_backlog_execution_evidence.json");
    public static final Path RUN_STATE = Path.of("build/tmp/phase_a_certification/stack_all_items_inventory_backlog_execution_evidence.run.json");
    private PhaseAStackAllItemsEvidenceValidation() { }
    public static JsonObject load(Path root, Path path, boolean exact, boolean active) throws Exception {
        var cases = PhaseAStackAllItemsCertification.cases(root);
        var expected = new LinkedHashMap<String, JsonObject>();
        for (var row : cases) expected.put(PhaseAStackAllItemsCertification.key(row), row);
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
            JsonObject receipt = entry.getAsJsonObject(); String key = PhaseAStackAllItemsCertification.key(receipt);
            require(allowed.contains(key) && keys.add(key), "Extra or duplicate receipt");
            validateReceipt(receipt, expected.get(key), runId, hash);
            require(players.add(string(receipt, "playerUuid")), "Player reused across cases");
        }
        require(keys.equals(new HashSet<>(allowed)), "Missing required receipts"); return artifact;
    }
    public static Set<String> canaryKeys(List<JsonObject> cases) {
        var keys=new LinkedHashSet<String>();for(int count:List.of(1,16,64))for(var row:cases)if(integer(row,"requiredCount")==count){keys.add(PhaseAStackAllItemsCertification.key(row));break;}
        require(keys.size()==3,"Representative count canary scope changed");return keys;
    }
    public static void validateReceipt(JsonObject r,JsonObject row,String runId,String hash) {
        require(row!=null,"Unknown backlog witness");require(string(r,"family").equals(PhaseAStackAllItemsCertification.FAMILY)&&string(r,"source").equals(PhaseAStackAllItemsCertification.SOURCE),"Receipt ownership mismatch");
        require(string(r,"runId").equals(runId)&&string(r,"catalogFingerprint").equals(hash),"Receipt run/hash mismatch");UUID.fromString(string(r,"playerUuid"));UUID.fromString(string(r,"itemEntityUuid"));
        for(String field:List.of("advancementId","criterion","selectedItem","trigger","boundary"))require(string(r,field).equals(string(row,field)),"Frozen witness mismatch "+field);
        require(string(r,"observedItem").equals(string(row,"selectedItem"))&&integer(r,"requirementGroup")==integer(row,"requirementGroup"),"Observed item/group mismatch");int expected=integer(row,"requiredCount");
        require(integer(r,"requiredCount")==expected&&integer(r,"observedCount")==expected&&integer(r,"pickedUpCount")==expected&&integer(r,"itemEntityCountBefore")==expected&&integer(r,"inventoryCountBefore")==0&&integer(r,"inventoryCountAfter")==expected&&integer(r,"maxStackSize")>=expected,"Finite native stack mismatch");
        require(string(r,"result").equals("GREEN")&&string(r,"gameMode").equals("SURVIVAL")&&!bool(r,"criterionBefore"),"Missing false-to-true SURVIVAL proof");
        for(String field:List.of("joined","connectionRegistered","clientLoaded","finiteMaterials","notSpectator","normalScheduler","matchingStackAfter","itemEntityConsumed","criterionAfter","noDirectCriterionTrigger","noManualAward"))require(bool(r,field),"Missing native witness "+field);
        require(integer(r,"ticksToComplete")>0&&integer(r,"ticksToComplete")<=80,"Native pickup did not settle within bound");var cleanup=r.getAsJsonObject("cleanup");for(String field:List.of("playerRemoved","connectionRemoved","channelSettled","itemEntityRemoved","worldFixtureRestored"))require(bool(cleanup,field),"Cleanup incomplete "+field);require(integer(cleanup,"warningCount")==0&&integer(cleanup,"settlementMessages")>=0,"Cleanup debt");
    }
    public static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadValidatedRuntimeEvidence(Path root) throws java.io.IOException {
        try { return loadPersistent(root); }
        catch (Exception failure) { throw new java.io.IOException("Invalid persistent stack inventory backlog evidence", failure); }
    }
    private static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadPersistent(Path root) throws Exception {
        JsonObject artifact = load(root, PhaseAStackAllItemsCertification.PERSISTENT, true, false);
        Map<String, Set<String>> criteria = new TreeMap<>();
        for (var entry : artifact.getAsJsonArray("entries")) { var receipt = entry.getAsJsonObject(); criteria.computeIfAbsent(string(receipt, "advancementId"), key -> new TreeSet<>()).add(string(receipt, "criterion")); }
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> result = new LinkedHashMap<>();
        for (var entry : criteria.entrySet()) result.put(entry.getKey(), new PhaseAAdvancementRollup.RuntimeEvidence(entry.getKey(), entry.getValue(),
            Map.of(PhaseAStackAllItemsCertification.FAMILY, entry.getValue()), Map.of(PhaseAStackAllItemsCertification.SOURCE, entry.getValue()),
            Set.of(PhaseAStackAllItemsCertification.FAMILY), Set.of(PhaseAStackAllItemsCertification.SOURCE)));
        return result;
    }
    public static String fingerprint(Path root) throws Exception { return "sha-256:" + PhaseAStackAllItemsCertification.sha(Files.readAllBytes(root.resolve(PhaseAStackAllItemsCertification.CATALOG))); }
    public static void common(JsonObject artifact, String hash) {
        require(string(artifact, "snapshot").equals("phase_a_stack_all_items_inventory_backlog_execution_evidence") && integer(artifact, "schemaVersion") == 1, "Schema mismatch");
        require(string(artifact, "family").equals(PhaseAStackAllItemsCertification.FAMILY) && string(artifact, "source").equals(PhaseAStackAllItemsCertification.SOURCE), "Artifact ownership mismatch");
        require(string(artifact, "minecraftVersion").equals("26.2") && string(artifact, "compatibilityMarker").equals("compat_26_2_r15"), "Compatibility mismatch");
        require(string(artifact, "catalogFingerprint").equals(hash), "Catalog changed"); UUID.fromString(string(artifact, "runId"));
    }
    public static JsonObject read(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
    private static String string(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && !object.get(key).getAsString().isBlank(), "Missing " + key); return object.get(key).getAsString(); }
    private static int integer(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber(), "Missing integer " + key); return object.get(key).getAsInt(); }
    private static boolean bool(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isBoolean(), "Missing boolean " + key); return object.get(key).getAsBoolean(); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
