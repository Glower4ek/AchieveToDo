package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Artifact-local independent parser; exact coverage is a set of frozen requirement witnesses. */
public final class PhaseAMixedPerfectRunEvidenceValidation {
    public static final Path TEMP = Path.of("build/tmp/phase_a_certification/mixed_perfect_run_execution_evidence.json");
    public static final Path RUN_STATE = Path.of("build/tmp/phase_a_certification/mixed_perfect_run_execution_evidence.run.json");
    private PhaseAMixedPerfectRunEvidenceValidation() { }
    public static JsonObject load(Path root, Path path, boolean exact, boolean active) throws Exception {
        var cases = PhaseAMixedPerfectRunCertification.cases(root);
        var expected = new LinkedHashMap<String, JsonObject>();
        for (var row : cases) expected.put(PhaseAMixedPerfectRunCertification.key(row), row);
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
            JsonObject receipt = entry.getAsJsonObject(); String key = PhaseAMixedPerfectRunCertification.key(receipt);
            require(allowed.contains(key) && keys.add(key), "Extra or duplicate receipt");
            validateReceipt(receipt, expected.get(key), runId, hash);
            players.add(string(receipt, "playerUuid"));
        }
        require(keys.equals(new HashSet<>(allowed)), "Missing required receipts");
        require(players.size()==1,"Perfect Run must complete all AND groups on one player");
        var receipts=artifact.getAsJsonArray("entries");long frame=receipts.get(0).getAsJsonObject().get("completionGameTime").getAsLong();
        for(var entry:receipts)require(entry.getAsJsonObject().get("completionGameTime").getAsLong()==frame,"Completion frames differ");return artifact;
    }
    public static Set<String> canaryKeys(List<JsonObject> cases){Set<String> keys=new TreeSet<>();for(var row:cases)keys.add(PhaseAMixedPerfectRunCertification.key(row));require(keys.size()==3,"Canary scope changed");return keys;}
    public static void validateReceipt(JsonObject r,JsonObject row,String runId,String hash){
        require(row!=null,"Unknown receipt");require(string(r,"family").equals(PhaseAMixedPerfectRunCertification.FAMILY)&&string(r,"source").equals(PhaseAMixedPerfectRunCertification.SOURCE),"Ownership mismatch");
        require(string(r,"runId").equals(runId)&&string(r,"catalogFingerprint").equals(hash),"Run/hash mismatch");for(String f:new String[]{"advancementId","criterion","trigger","boundary"})require(string(r,f).equals(string(row,f)),"Frozen witness mismatch");
        require(integer(r,"requirementGroup")==integer(row,"requirementGroup")&&string(r,"gameMode").equals("SURVIVAL")&&string(r,"result").equals("GREEN"),"Group/mode/result mismatch");UUID.fromString(string(r,"playerUuid"));UUID.fromString(string(r,"targetUuid"));
        for(String f:new String[]{"joined","connectionRegistered","clientLoaded","finiteMaterials","notSpectator","normalScheduler","nativeSummonPacketSent","skullAndSoulSandConsumed","nativeSummonedWitherObserved","spawnPrerequisiteAfter","spawnPrerequisiteWitherCriterion","criterionAfter","nativeAttackPacketSent","allRequirementsSatisfiedAtReceipt","noDirectCriterionTrigger","noManualAward"})require(bool(r,f),"Missing native witness "+f);
        require(!bool(r,"criterionBefore")&&!bool(r,"spawnPrerequisiteBefore")&&integer(r,"lastSkullBefore")==1&&integer(r,"lastSkullAfter")==0&&integer(r,"nativeWitherInvulnerableTicksAtSpawn")==220,"Native prerequisite transition missing");UUID.fromString(string(r,"summonedWitherUuid"));
        require(integer(r,"ticksSinceSummon")>0&&integer(r,"ticksSinceSummon")<600,"Frozen failure window exceeded");require(r.get("completionGameTime").getAsLong()-r.get("summonGameTime").getAsLong()==integer(r,"ticksSinceSummon"),"Timeline mismatch");
        require(integer(r,"frozenTimerAtCompletion")>0&&integer(r,"frozenTimerAtCompletion")<30&&integer(r,"frozenTimerAfterExpiry")==-1&&bool(r,"completedProgressRetained")&&bool(r,"prerequisiteExpired")&&r.get("postWindowGameTime").getAsLong()-r.get("summonGameTime").getAsLong()>=650,"Native timer expiry/retention missing");
        var criteria=r.getAsJsonObject("allCriteriaAfter");require(criteria.keySet().equals(Set.of("dragon","wither","raid")),"AND key set missing");for(var e:criteria.entrySet())require(e.getValue().getAsBoolean(),"Incomplete live AND group");
        var damage=r.getAsJsonObject("noDamageWitness");require(damage.get("healthBefore").getAsDouble()==20&&damage.get("healthAfter").getAsDouble()==20,"Player took damage");for(String objective:new String[]{"bac_pr_dmgt","bac_pr_dmga","bac_pr_dmgr"})require(integer(damage,objective)==0,"Native damage counter nonzero");
        var raid=r.getAsJsonObject("raidWitness");require(bool(raid,"nativeVictory")&&bool(raid,"heroEffect")&&integer(raid,"groupsSpawned")==3&&integer(raid,"nativeRaidersKilled")>=3&&integer(raid,"remainingRaiders")==0&&integer(raid,"registeredRaidId")>=0,"Native raid victory missing");UUID.fromString(string(raid,"lastRaiderUuid"));
        require(string(raid,"lastRaiderKillerUuid").equals(string(r,"playerUuid"))&&bool(raid,"villageLookupBeforeOmen")&&bool(raid,"nativeOmenCreatedRaid"),"Native hero lifecycle missing");
        if(!string(row,"criterion").equals("raid")){var kill=r.getAsJsonObject("killWitness");require(string(kill,"sourceUuid").equals(string(r,"playerUuid"))&&string(kill,"targetUuid").equals(string(r,"targetUuid"))&&kill.get("healthBefore").getAsDouble()>0&&kill.get("healthAfter").getAsDouble()==0,"Native lethal blow missing");require(string(kill,"entityType").equals(string(row,"criterion").equals("dragon")?"minecraft:ender_dragon":"minecraft:wither"),"Wrong boss");}
        var gate=r.getAsJsonObject("productionGateWitness");require(string(gate,"thresholdSource").equals("LIVE_OVERWORLD_SEED_CONFIGURATION")&&integer(gate,"requiredThreshold")>0&&bool(gate,"lockedBefore")&&!bool(gate,"lockedAfter")&&integer(gate,"scoreAfter")>=integer(gate,"requiredThreshold"),"Live wooden-tool gate missing");
        var cleanup=r.getAsJsonObject("cleanup");for(String f:new String[]{"playerRemoved","connectionRemoved","channelSettled","fixtureEntitiesRemoved","raidStopped","raidUnregistered","villageRestored","difficultyRestored","gameRuleRestored","chunkTicketsRestored"})require(bool(cleanup,f),"Cleanup missing "+f);require(integer(cleanup,"warningCount")==0&&integer(cleanup,"settlementMessages")>=0,"Cleanup debt");
    }
    public static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadValidatedRuntimeEvidence(Path root) throws java.io.IOException {
        try { return loadPersistent(root); }
        catch (Exception failure) { throw new java.io.IOException("Invalid persistent perfect-run evidence", failure); }
    }
    private static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadPersistent(Path root) throws Exception {
        JsonObject artifact = load(root, PhaseAMixedPerfectRunCertification.PERSISTENT, true, false);
        Map<String, Set<String>> criteria = new TreeMap<>();
        for (var entry : artifact.getAsJsonArray("entries")) { var receipt = entry.getAsJsonObject(); criteria.computeIfAbsent(string(receipt, "advancementId"), key -> new TreeSet<>()).add(string(receipt, "criterion")); }
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> result = new LinkedHashMap<>();
        for (var entry : criteria.entrySet()) result.put(entry.getKey(), new PhaseAAdvancementRollup.RuntimeEvidence(entry.getKey(), entry.getValue(),
            Map.of(PhaseAMixedPerfectRunCertification.FAMILY, entry.getValue()), Map.of(PhaseAMixedPerfectRunCertification.SOURCE, entry.getValue()),
            Set.of(PhaseAMixedPerfectRunCertification.FAMILY), Set.of(PhaseAMixedPerfectRunCertification.SOURCE)));
        return result;
    }
    public static String fingerprint(Path root) throws Exception { return "sha-256:" + PhaseAMixedPerfectRunCertification.sha(Files.readAllBytes(root.resolve(PhaseAMixedPerfectRunCertification.CATALOG))); }
    public static void common(JsonObject artifact, String hash) {
        require(string(artifact, "snapshot").equals("phase_a_mixed_perfect_run_execution_evidence") && integer(artifact, "schemaVersion") == 1, "Schema mismatch");
        require(string(artifact, "family").equals(PhaseAMixedPerfectRunCertification.FAMILY) && string(artifact, "source").equals(PhaseAMixedPerfectRunCertification.SOURCE), "Artifact ownership mismatch");
        require(string(artifact, "minecraftVersion").equals("26.2") && string(artifact, "compatibilityMarker").equals("compat_26_2_r15"), "Compatibility mismatch");
        require(string(artifact, "catalogFingerprint").equals(hash), "Catalog changed"); UUID.fromString(string(artifact, "runId"));
    }
    public static JsonObject read(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
    private static String string(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && !object.get(key).getAsString().isBlank(), "Missing " + key); return object.get(key).getAsString(); }
    private static int integer(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber(), "Missing integer " + key); return object.get(key).getAsInt(); }
    private static boolean bool(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isBoolean(), "Missing boolean " + key); return object.get(key).getAsBoolean(); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
