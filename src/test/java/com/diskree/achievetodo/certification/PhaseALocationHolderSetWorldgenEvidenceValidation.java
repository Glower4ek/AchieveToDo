package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Artifact-local independent parser; exact coverage is a set of frozen requirement witnesses. */
public final class PhaseALocationHolderSetWorldgenEvidenceValidation {
    public static final Path TEMP = Path.of("build/tmp/phase_a_certification/location_holder_set_worldgen_execution_evidence.json");
    public static final Path RUN_STATE = Path.of("build/tmp/phase_a_certification/location_holder_set_worldgen_execution_evidence.run.json");
    private PhaseALocationHolderSetWorldgenEvidenceValidation() { }
    public static JsonObject load(Path root, Path path, boolean exact, boolean active) throws Exception {
        var cases = PhaseALocationHolderSetWorldgenCertification.cases(root);
        var expected = new LinkedHashMap<String, JsonObject>();
        for (var row : cases) expected.put(PhaseALocationHolderSetWorldgenCertification.key(row), row);
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
            JsonObject receipt = entry.getAsJsonObject(); String key = PhaseALocationHolderSetWorldgenCertification.key(receipt);
            require(allowed.contains(key) && keys.add(key), "Extra or duplicate receipt");
            validateReceipt(receipt, expected.get(key), runId, hash);
            require(players.add(string(receipt, "playerUuid")), "Player reused across cases");
        }
        require(keys.equals(new HashSet<>(allowed)), "Missing required receipts"); return artifact;
    }
    public static Set<String> canaryKeys(List<JsonObject> cases) {
        var keys = new LinkedHashSet<String>();
        for (var row : cases) {
            String id = string(row, "advancementId"), criterion = string(row, "criterion");
            if (id.endsWith("/inception") || id.endsWith("/silent_but_deadly") || id.endsWith("/from_whence_it_came") || id.endsWith("/stepping_on_legos") || id.endsWith("/titanic") || (id.endsWith("/llama_festival") && criterion.equals("white_carpet")) || (id.endsWith("/explorer_of_worlds") && criterion.equals("end_midlands"))) keys.add(PhaseALocationHolderSetWorldgenCertification.key(row));
        }
        require(keys.size() == 7, "Location canary scope changed"); return keys;
    }
    public static void validateReceipt(JsonObject receipt, JsonObject row, String runId, String hash) {
        require(row != null, "Unknown receipt");
        require(string(receipt, "family").equals(PhaseALocationHolderSetWorldgenCertification.FAMILY)
            && string(receipt, "source").equals(PhaseALocationHolderSetWorldgenCertification.SOURCE), "Receipt ownership mismatch");
        require(string(receipt, "runId").equals(runId) && string(receipt, "catalogFingerprint").equals(hash), "Receipt run/hash mismatch");
        require(string(receipt, "result").equals("GREEN") && string(receipt, "gameMode").equals("SURVIVAL"), "Result/mode mismatch"); UUID.fromString(string(receipt, "playerUuid"));
        for (String field : new String[]{"advancementId", "criterion"}) require(string(receipt, field).equals(string(row, field)), "Witness mismatch " + field);
        require(integer(receipt, "requirementGroup") == integer(row, "requirementGroup"), "Wrong requirement group");
        require(string(receipt, "trigger").equals("minecraft:location") && string(receipt, "boundary").equals(PhaseALocationHolderSetWorldgenCertification.BOUNDARY), "Native boundary mismatch");
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "finiteMaterials", "contextObservedBeforeTick", "criterionAfter", "noDirectCriterionTrigger", "noManualAward"}) require(bool(receipt, field), "Missing location witness " + field);
        require(!bool(receipt, "criterionBefore") && integer(receipt, "ticksToComplete") > 0 && integer(receipt, "ticksToComplete") <= 60, "False-to-true transition missing");
        var observed = receipt.getAsJsonObject("contextWitness"); var predicate = row.getAsJsonObject("selectedPredicate");
        var gate=receipt.getAsJsonObject("productionGateWitness");require(string(gate,"thresholdSource").equals("LIVE_OVERWORLD_SEED_CONFIGURATION")&&integer(gate,"scoreBefore")==0,"Stance configuration missing");
        require(integer(gate,"nativeScoreBeforeFixture")>=0,"Native pre-fixture score observation missing");
        var required=new TreeSet<String>();
        if(predicate.has("flags")) for(var flag:predicate.getAsJsonObject("flags").entrySet()) if(flag.getValue().getAsBoolean()) required.add(flag.getKey().equals("is_sneaking")?"SNEAK":"SPRINT");
        require(gate.getAsJsonObject("abilities").keySet().equals(required),"Stance gate scope mismatch");
        for(String ability:required){var state=gate.getAsJsonObject("abilities").getAsJsonObject(ability);require(integer(state,"requiredThreshold")>0&&bool(state,"lockedBefore")&&!bool(state,"lockedAfter")&&integer(gate,"scoreAfter")>=integer(state,"requiredThreshold"),"Live stance gate missing");}
        require(observed != null && bool(observed, "notSpectator") && bool(observed, "excludedPredicatesAbsent"), "Exclusion witness missing");
        var location = predicate.has("location") ? predicate.getAsJsonObject("location") : new JsonObject();
        if (location.has("biomes")) require(string(observed,"biome").equals(string(row,"selectedBiome"))
            && string(observed,"biomeSelector").equals(location.get("biomes").getAsString())&&bool(observed,"biomeSelectorMatched"),"Native biome/member mismatch");
        if (location.has("dimension")) require(string(observed, "dimension").equals(PhaseALocationHolderSetWorldgenCertification.namespaced(location.get("dimension").getAsString())), "Dimension mismatch");
        if (location.has("structures")) require(bool(observed, "structureLookup") && string(observed, "structure").equals(location.get("structures").getAsString()), "Structure mismatch");
        if (location.has("position")) { var y=location.getAsJsonObject("position").getAsJsonObject("y"); double actual=observed.get("y").getAsDouble(); require(actual>=y.get("min").getAsDouble()&&actual<=y.get("max").getAsDouble(), "Y mismatch"); }
        if (location.has("block")) require(bool(observed, "locationBlockMatched"), "Location block missing");
        if (predicate.has("stepping_on")) require(bool(observed, "onGround") && bool(observed, "steppingBlockMatched") && (!predicate.getAsJsonObject("stepping_on").has("dimension") || string(observed,"dimension").equals(PhaseALocationHolderSetWorldgenCertification.namespaced(predicate.getAsJsonObject("stepping_on").get("dimension").getAsString()))), "Grounded block mismatch");
        if (predicate.has("vehicle")) require(string(observed,"vehicleType").equals("minecraft:llama") && bool(observed,"vehicleTagMatched") && bool(observed,"vehicleNbtMatched") && string(observed,"vehicleBodyItem").equals("minecraft:"+string(row,"criterion")), "Vehicle context missing");
        if (predicate.has("flags")) for (var flag:predicate.getAsJsonObject("flags").entrySet()) require(bool(observed,flag.getKey())==flag.getValue().getAsBoolean(), "Stance mismatch");
        if (predicate.has("vehicle")) require(string(observed,"nativeVehicleNbt").equals(string(row,"nativeVehicleNbt"))
            && string(row,"productionMigration").equals("EXACT_FROZEN_LLAMA_CARPET_NBT_TO_EQUIPMENT_BODY"),"Native mapped vehicle NBT mismatch");
        if (predicate.has("effects")) for (var effect:predicate.getAsJsonObject("effects").entrySet()) require(observed.getAsJsonObject("effects").get(effect.getKey()).getAsInt()>=effect.getValue().getAsJsonObject().getAsJsonObject("amplifier").get("min").getAsInt(), "Effect mismatch");
        if (predicate.has("equipment")) for (var slot:predicate.getAsJsonObject("equipment").entrySet()) {
            var expected=slot.getValue().getAsJsonObject(); var actual=observed.getAsJsonObject("equipment").getAsJsonObject(slot.getKey()); require(bool(actual,"itemSelectorMatched"), "Equipment selector mismatch");
            if(expected.has("predicates")) for(var enchant:expected.getAsJsonObject("predicates").getAsJsonArray("enchantments")) {var e=enchant.getAsJsonObject(); require(actual.getAsJsonObject("enchantments").get(e.get("enchantments").getAsString()).getAsInt()>=e.getAsJsonObject("levels").get("min").getAsInt(), "Enchantment level mismatch");}
        }
        var cleanup = receipt.getAsJsonObject("cleanup");
        for (String field : new String[]{"playerRemoved", "connectionRemoved", "channelSettled" , "fixtureEntitiesRemoved"}) require(bool(cleanup, field), "Cleanup incomplete " + field);
        require(bool(cleanup, "structureRestored") && bool(cleanup, "biomeRestored"), "World context cleanup missing");
        require(integer(cleanup, "warningCount") == 0 && integer(cleanup, "settlementMessages") >= 0, "Cleanup warning debt");
    }
    public static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadValidatedRuntimeEvidence(Path root) throws java.io.IOException {
        try { return loadPersistent(root); }
        catch (Exception failure) { throw new java.io.IOException("Invalid persistent player-killed-entity remainder evidence", failure); }
    }
    private static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadPersistent(Path root) throws Exception {
        JsonObject artifact = load(root, PhaseALocationHolderSetWorldgenCertification.PERSISTENT, true, false);
        Map<String, Set<String>> criteria = new TreeMap<>();
        for (var entry : artifact.getAsJsonArray("entries")) { var receipt = entry.getAsJsonObject(); criteria.computeIfAbsent(string(receipt, "advancementId"), key -> new TreeSet<>()).add(string(receipt, "criterion")); }
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> result = new LinkedHashMap<>();
        for (var entry : criteria.entrySet()) result.put(entry.getKey(), new PhaseAAdvancementRollup.RuntimeEvidence(entry.getKey(), entry.getValue(),
            Map.of(PhaseALocationHolderSetWorldgenCertification.FAMILY, entry.getValue()), Map.of(PhaseALocationHolderSetWorldgenCertification.SOURCE, entry.getValue()),
            Set.of(PhaseALocationHolderSetWorldgenCertification.FAMILY), Set.of(PhaseALocationHolderSetWorldgenCertification.SOURCE)));
        return result;
    }
    public static String fingerprint(Path root) throws Exception { return "sha-256:" + PhaseALocationHolderSetWorldgenCertification.sha(Files.readAllBytes(root.resolve(PhaseALocationHolderSetWorldgenCertification.CATALOG))); }
    public static void common(JsonObject artifact, String hash) {
        require(string(artifact, "snapshot").equals("phase_a_location_holder_set_worldgen_execution_evidence") && integer(artifact, "schemaVersion") == 1, "Schema mismatch");
        require(string(artifact, "family").equals(PhaseALocationHolderSetWorldgenCertification.FAMILY) && string(artifact, "source").equals(PhaseALocationHolderSetWorldgenCertification.SOURCE), "Artifact ownership mismatch");
        require(string(artifact, "minecraftVersion").equals("26.2") && string(artifact, "compatibilityMarker").equals("compat_26_2_r15"), "Compatibility mismatch");
        require(string(artifact, "catalogFingerprint").equals(hash), "Catalog changed"); UUID.fromString(string(artifact, "runId"));
    }
    public static JsonObject read(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
    private static String string(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && !object.get(key).getAsString().isBlank(), "Missing " + key); return object.get(key).getAsString(); }
    private static int integer(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber(), "Missing integer " + key); return object.get(key).getAsInt(); }
    private static boolean bool(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isBoolean(), "Missing boolean " + key); return object.get(key).getAsBoolean(); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
