package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Artifact-local independent parser; exact coverage is a set of frozen requirement witnesses. */
public final class PhaseAMixedWeaponryMulticlassedEvidenceValidation {
    public static final Path TEMP = Path.of("build/tmp/phase_a_certification/mixed_weaponry_multiclassed_execution_evidence.json");
    public static final Path RUN_STATE = Path.of("build/tmp/phase_a_certification/mixed_weaponry_multiclassed_execution_evidence.run.json");
    private PhaseAMixedWeaponryMulticlassedEvidenceValidation() { }
    public static JsonObject load(Path root, Path path, boolean exact, boolean active) throws Exception {
        var cases = PhaseAMixedWeaponryMulticlassedCertification.cases(root);
        var expected = new LinkedHashMap<String, JsonObject>();
        for (var row : cases) expected.put(PhaseAMixedWeaponryMulticlassedCertification.key(row), row);
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
            JsonObject receipt = entry.getAsJsonObject(); String key = PhaseAMixedWeaponryMulticlassedCertification.key(receipt);
            require(allowed.contains(key) && keys.add(key), "Extra or duplicate receipt");
            validateReceipt(receipt, expected.get(key), runId, hash);
            require(players.add(string(receipt, "playerUuid")), "Player reused across cases");
        }
        require(keys.equals(new HashSet<>(allowed)), "Missing required receipts"); return artifact;
    }
    public static Set<String> canaryKeys(List<JsonObject> cases) {
        Set<String> criteria=Set.of("axe","crossbow","firework_rocket","egg","fishing_rod","lingering_potion","wind_charge");var keys=new TreeSet<String>();for(var row:cases)if(criteria.contains(string(row,"criterion")))keys.add(PhaseAMixedWeaponryMulticlassedCertification.key(row));require(keys.size()==7,"Canary scope changed");return keys;
    }
    public static void validateReceipt(JsonObject r,JsonObject row,String runId,String hash){
        require(row!=null,"Unknown receipt");require(string(r,"family").equals(PhaseAMixedWeaponryMulticlassedCertification.FAMILY)&&string(r,"source").equals(PhaseAMixedWeaponryMulticlassedCertification.SOURCE),"Ownership mismatch");
        require(string(r,"runId").equals(runId)&&string(r,"catalogFingerprint").equals(hash),"Run/hash mismatch");
        for(String f:new String[]{"advancementId","criterion","trigger","boundary","selectedItem","action"})require(string(r,f).equals(string(row,f)),"Row mismatch "+f);
        require(integer(r,"requirementGroup")==integer(row,"requirementGroup")&&string(r,"gameMode").equals("SURVIVAL")&&string(r,"result").equals("GREEN"),"Group/mode/result mismatch");UUID.fromString(string(r,"playerUuid"));UUID.fromString(string(r,"targetUuid"));
        for(String f:new String[]{"joined","connectionRegistered","clientLoaded","finiteMaterials","notSpectator","criterionAfter","nativePacketSent","noDirectCriterionTrigger","noManualAward"})require(bool(r,f),"Missing native witness "+f);
        require(!bool(r,"criterionBefore")&&integer(r,"ticksToComplete")>0&&integer(r,"ticksToComplete")<=180&&integer(r,"itemCountBefore")==1,"Transition/window/finite item mismatch");
        var gate=r.getAsJsonObject("productionGateWitness");require(string(gate,"thresholdSource").equals("LIVE_OVERWORLD_SEED_CONFIGURATION")&&integer(gate,"scoreBefore")==0,"Gate fixture mismatch");
        require(gate.getAsJsonObject("abilities").keySet().equals(string(row,"requiredAbility").equals("NONE")?Set.of():Set.of(string(row,"requiredAbility"))),"Gate scope mismatch");
        for(var e:gate.getAsJsonObject("abilities").entrySet()){var g=e.getValue().getAsJsonObject();require(integer(g,"threshold")>0&&bool(g,"lockedBefore")&&!bool(g,"lockedAfter")&&integer(gate,"scoreAfter")>=integer(g,"threshold"),"Gate not unlocked");}
        if(Set.of("BOW","CROSSBOW").contains(string(row,"action")))require(integer(r,"ammoBefore")==1&&integer(r,"ammoAfter")==0&&bool(r,"projectileSpawned"),"Finite launcher ammunition missing");
        if(string(row,"action").equals("CROSSBOW"))require(bool(r,"chargedByNativeTicks"),"Native charge missing");
        var conditions=row.getAsJsonObject("frozenConditions");
        if(string(row,"trigger").equals("minecraft:player_hurt_entity")){
            var d=r.getAsJsonObject("damageWitness");require(string(d,"sourceUuid").equals(string(r,"playerUuid")),"Wrong native source owner");UUID.fromString(string(d,"directUuid"));
            double before=d.get("healthBefore").getAsDouble(),after=d.get("healthAfter").getAsDouble();require(before>0&&after>=0&&after<=before,"Health witness invalid");
            require(before>after||string(row,"criterion").equals("egg"),"Positive damage missing");
            var spec=conditions.getAsJsonObject("damage").getAsJsonObject("type");if(spec.has("tags"))for(var term:spec.getAsJsonArray("tags")){require(term.getAsJsonObject().get("id").getAsString().equals("minecraft:is_projectile")&&bool(d,"isProjectile")==term.getAsJsonObject().get("expected").getAsBoolean(),"Damage tag mismatch");}
            if(row.has("nativeDirectEntity"))require(string(d,"directType").equals(string(row,"nativeDirectEntity")),"Direct native entity mismatch");
            if(spec.has("source_entity"))require(bool(d,"mainhandSelectorMatched")&&string(d,"mainhandItem").equals(string(row,"selectedItem")),"Native source equipment mismatch");
            if(conditions.has("entity"))require(d.get("distance").getAsDouble()<=5,"Victim distance mismatch");
        }else if(string(row,"action").equals("CROSSBOW")){require(bool(r,"chargedByNativeTicks")&&integer(r,"ammoBefore")==1&&integer(r,"ammoAfter")==0&&bool(r,"projectileSpawned"),"Native crossbow load/fire missing");}
        else require(bool(r,"hookAttachedBeforeReel")&&bool(r,"hookRemovedAfterReel")&&integer(r,"toolDamageAfter")>integer(r,"toolDamageBefore"),"Native fishing cast/collision/reel missing");
        if(string(row,"action").equals("THROW")){
            require(integer(r,"itemCountAfter")==0&&bool(r,"projectileSpawned"),"Finite thrown item missing: remaining="+integer(r,"itemCountAfter")+" projectile="+bool(r,"projectileSpawned"));
            require(string(r,"mainhandItemAfter").equals("minecraft:air")||(string(row,"criterion").endsWith("potion")&&string(r,"mainhandItemAfter").equals("minecraft:glass_bottle")),"Unexpected native return item");
        }
        var cleanup=r.getAsJsonObject("cleanup");for(String f:new String[]{"playerRemoved","connectionRemoved","channelSettled","fixtureEntitiesRemoved"})require(bool(cleanup,f),"Cleanup missing");require(integer(cleanup,"warningCount")==0&&integer(cleanup,"settlementMessages")>=0,"Cleanup debt");
    }
    public static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadValidatedRuntimeEvidence(Path root) throws java.io.IOException {
        try { return loadPersistent(root); }
        catch (Exception failure) { throw new java.io.IOException("Invalid persistent multiclassed evidence", failure); }
    }
    private static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadPersistent(Path root) throws Exception {
        JsonObject artifact = load(root, PhaseAMixedWeaponryMulticlassedCertification.PERSISTENT, true, false);
        Map<String, Set<String>> criteria = new TreeMap<>();
        for (var entry : artifact.getAsJsonArray("entries")) { var receipt = entry.getAsJsonObject(); criteria.computeIfAbsent(string(receipt, "advancementId"), key -> new TreeSet<>()).add(string(receipt, "criterion")); }
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> result = new LinkedHashMap<>();
        for (var entry : criteria.entrySet()) result.put(entry.getKey(), new PhaseAAdvancementRollup.RuntimeEvidence(entry.getKey(), entry.getValue(),
            Map.of(PhaseAMixedWeaponryMulticlassedCertification.FAMILY, entry.getValue()), Map.of(PhaseAMixedWeaponryMulticlassedCertification.SOURCE, entry.getValue()),
            Set.of(PhaseAMixedWeaponryMulticlassedCertification.FAMILY), Set.of(PhaseAMixedWeaponryMulticlassedCertification.SOURCE)));
        return result;
    }
    public static String fingerprint(Path root) throws Exception { return "sha-256:" + PhaseAMixedWeaponryMulticlassedCertification.sha(Files.readAllBytes(root.resolve(PhaseAMixedWeaponryMulticlassedCertification.CATALOG))); }
    public static void common(JsonObject artifact, String hash) {
        require(string(artifact, "snapshot").equals("phase_a_mixed_weaponry_multiclassed_execution_evidence") && integer(artifact, "schemaVersion") == 1, "Schema mismatch");
        require(string(artifact, "family").equals(PhaseAMixedWeaponryMulticlassedCertification.FAMILY) && string(artifact, "source").equals(PhaseAMixedWeaponryMulticlassedCertification.SOURCE), "Artifact ownership mismatch");
        require(string(artifact, "minecraftVersion").equals("26.2") && string(artifact, "compatibilityMarker").equals("compat_26_2_r15"), "Compatibility mismatch");
        require(string(artifact, "catalogFingerprint").equals(hash), "Catalog changed"); UUID.fromString(string(artifact, "runId"));
    }
    public static JsonObject read(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
    private static String string(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && !object.get(key).getAsString().isBlank(), "Missing " + key); return object.get(key).getAsString(); }
    private static int integer(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber(), "Missing integer " + key); return object.get(key).getAsInt(); }
    private static boolean bool(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isBoolean(), "Missing boolean " + key); return object.get(key).getAsBoolean(); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
