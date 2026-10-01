package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Artifact-local independent parser; exact coverage is a set of frozen requirement witnesses. */
public final class PhaseAPlayerKilledEntityRemainderEvidenceValidation {
    public static final Path TEMP = Path.of("build/tmp/phase_a_certification/player_killed_entity_remainder_execution_evidence.json");
    public static final Path RUN_STATE = Path.of("build/tmp/phase_a_certification/player_killed_entity_remainder_execution_evidence.run.json");
    private PhaseAPlayerKilledEntityRemainderEvidenceValidation() { }
    public static JsonObject load(Path root, Path path, boolean exact, boolean active) throws Exception {
        var cases = PhaseAPlayerKilledEntityRemainderCertification.cases(root);
        var expected = new LinkedHashMap<String, JsonObject>();
        for (var row : cases) expected.put(PhaseAPlayerKilledEntityRemainderCertification.key(row), row);
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
            JsonObject receipt = entry.getAsJsonObject(); String key = PhaseAPlayerKilledEntityRemainderCertification.key(receipt);
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
            if (id.endsWith("/heres_johnny") || id.endsWith("/spider_skeleton") || id.endsWith("/warden_frostbite") || id.endsWith("/the_high_road") || (id.endsWith("/the_mighty_hunter") && criterion.equals("rabbit"))) keys.add(PhaseAPlayerKilledEntityRemainderCertification.key(row));
        }
        require(keys.size() == 5, "Kill canary scope changed"); return keys;
    }
    public static void validateReceipt(JsonObject receipt, JsonObject row, String runId, String hash) {
        require(row != null, "Unknown receipt");
        require(string(receipt, "family").equals(PhaseAPlayerKilledEntityRemainderCertification.FAMILY)
            && string(receipt, "source").equals(PhaseAPlayerKilledEntityRemainderCertification.SOURCE), "Receipt ownership mismatch");
        require(string(receipt, "runId").equals(runId) && string(receipt, "catalogFingerprint").equals(hash), "Receipt run/hash mismatch");
        require(string(receipt, "result").equals("GREEN") && string(receipt, "gameMode").equals("SURVIVAL"), "Result/mode mismatch"); UUID.fromString(string(receipt, "playerUuid"));
        for (String field : new String[]{"advancementId", "criterion", "selectedEntity", "selectedTool", "action"}) require(string(receipt, field).equals(string(row, field)), "Witness mismatch " + field);
        require(integer(receipt, "requirementGroup") == integer(row, "requirementGroup"), "Wrong requirement group");
        boolean arrow = string(row, "action").equals("ARROW");
        require(string(receipt, "trigger").equals("minecraft:player_killed_entity") && string(receipt, "boundary").equals(arrow ? PhaseAPlayerKilledEntityRemainderCertification.ARROW : PhaseAPlayerKilledEntityRemainderCertification.MELEE), "Native boundary mismatch");
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "finiteMaterials", "victimAliveBefore", "victimKilledByPlayer", "contextObservedBeforeAttack", "damageSourceObserved", "criterionAfter", "noDirectCriterionTrigger", "noManualAward"}) require(bool(receipt, field), "Missing native kill witness " + field);
        require(!bool(receipt, "criterionBefore") && integer(receipt, "ticksToComplete") > 0 && integer(receipt, "ticksToComplete") <= 120, "False-to-true transition missing");
        JsonObject context = receipt.getAsJsonObject("contextWitness"), predicate = row.getAsJsonObject("entityPredicate");
        require(context != null && bool(context, "entityTypeMatched") && bool(context, "toolTagMatched"), "Entity/tool selector witness missing");
        if (predicate.has("location")) {
            var location = predicate.getAsJsonObject("location");
            if (location.has("structures")) require(string(context, "structure").equals(PhaseAPlayerKilledEntityRemainderCertification.namespaced(location.get("structures").getAsString())) && bool(context, "structureLookup"), "Structure witness mismatch");
            if (location.has("biomes")) require(string(context, "biome").equals(PhaseAPlayerKilledEntityRemainderCertification.namespaced(location.get("biomes").getAsString())), "Biome witness mismatch");
            if (location.has("dimension")) require(string(context, "dimension").equals(PhaseAPlayerKilledEntityRemainderCertification.namespaced(location.get("dimension").getAsString())), "Dimension witness mismatch");
            if (location.has("position")) require(integer(context, "victimY") >= location.getAsJsonObject("position").getAsJsonObject("y").get("min").getAsInt(), "Y bound not observed");
        }
        if (predicate.has("passenger")) require(bool(context, "passengerTagMatched"), "Passenger context missing");
        if (predicate.has("effects")) require(bool(context, "invisibility"), "Effect context missing");
        if (predicate.has("equipment")) require(string(context, "victimChest").equals("minecraft:elytra"), "Victim equipment missing");
        if (predicate.has("stepping_on")) require(bool(context, "victimOnDripleaf") && bool(context, "playerOnDripleaf"), "Stepping context missing");
        var frozen = row.getAsJsonObject("frozenConditions");
        if (frozen.has("player") && frozen.getAsJsonObject("player").has("vehicle")) require(string(context, "playerVehicle").equals("minecraft:camel"), "Mounted source context missing");
        var damage = receipt.getAsJsonObject("damageWitness"); require(bool(damage, "sourceIsJoinedPlayer"), "Wrong kill credit source");
        var gate = receipt.getAsJsonObject("productionGateWitness");
        require(string(gate, "ability").equals(arrow ? "SHOOT_BOW" : "USE_WOODEN_TOOLS")
            && string(gate, "thresholdSource").equals("LIVE_OVERWORLD_SEED_CONFIGURATION")
            && integer(gate, "requiredThreshold") > 0 && bool(gate, "lockedBefore") && !bool(gate, "lockedAfter")
            && integer(gate, "scoreAfter") == integer(gate, "requiredThreshold"), "Live production combat gate missing");
        if (predicate.has("distance")) require(context.get("distance").getAsDouble() <= 5, "Relative distance context missing");
        if (arrow) require(bool(damage, "isProjectile") && bool(damage, "directArrowTagMatched") && integer(damage, "finiteArrowsConsumed") == 1, "Native arrow kill not observed");
        else require(string(damage, "damageType").equals("minecraft:player_attack") && bool(damage, "directIsJoinedPlayer"), "Native melee kill not observed");
        var cleanup = receipt.getAsJsonObject("cleanup");
        for (String field : new String[]{"playerRemoved", "connectionRemoved", "channelSettled", "fixtureEntitiesRemoved"}) require(bool(cleanup, field), "Cleanup incomplete " + field);
        require(bool(cleanup, "structureRestored") && bool(cleanup, "biomeRestored"), "World context cleanup missing");
        require(integer(cleanup, "warningCount") == 0 && integer(cleanup, "settlementMessages") >= 0, "Cleanup warning debt");
    }
    public static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadValidatedRuntimeEvidence(Path root) throws java.io.IOException {
        try { return loadPersistent(root); }
        catch (Exception failure) { throw new java.io.IOException("Invalid persistent player-killed-entity remainder evidence", failure); }
    }
    private static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadPersistent(Path root) throws Exception {
        JsonObject artifact = load(root, PhaseAPlayerKilledEntityRemainderCertification.PERSISTENT, true, false);
        Map<String, Set<String>> criteria = new TreeMap<>();
        for (var entry : artifact.getAsJsonArray("entries")) { var receipt = entry.getAsJsonObject(); criteria.computeIfAbsent(string(receipt, "advancementId"), key -> new TreeSet<>()).add(string(receipt, "criterion")); }
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> result = new LinkedHashMap<>();
        for (var entry : criteria.entrySet()) result.put(entry.getKey(), new PhaseAAdvancementRollup.RuntimeEvidence(entry.getKey(), entry.getValue(),
            Map.of(PhaseAPlayerKilledEntityRemainderCertification.FAMILY, entry.getValue()), Map.of(PhaseAPlayerKilledEntityRemainderCertification.SOURCE, entry.getValue()),
            Set.of(PhaseAPlayerKilledEntityRemainderCertification.FAMILY), Set.of(PhaseAPlayerKilledEntityRemainderCertification.SOURCE)));
        return result;
    }
    public static String fingerprint(Path root) throws Exception { return "sha-256:" + PhaseAPlayerKilledEntityRemainderCertification.sha(Files.readAllBytes(root.resolve(PhaseAPlayerKilledEntityRemainderCertification.CATALOG))); }
    public static void common(JsonObject artifact, String hash) {
        require(string(artifact, "snapshot").equals("phase_a_player_killed_entity_remainder_execution_evidence") && integer(artifact, "schemaVersion") == 1, "Schema mismatch");
        require(string(artifact, "family").equals(PhaseAPlayerKilledEntityRemainderCertification.FAMILY) && string(artifact, "source").equals(PhaseAPlayerKilledEntityRemainderCertification.SOURCE), "Artifact ownership mismatch");
        require(string(artifact, "minecraftVersion").equals("26.2") && string(artifact, "compatibilityMarker").equals("compat_26_2_r15"), "Compatibility mismatch");
        require(string(artifact, "catalogFingerprint").equals(hash), "Catalog changed"); UUID.fromString(string(artifact, "runId"));
    }
    public static JsonObject read(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
    private static String string(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && !object.get(key).getAsString().isBlank(), "Missing " + key); return object.get(key).getAsString(); }
    private static int integer(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber(), "Missing integer " + key); return object.get(key).getAsInt(); }
    private static boolean bool(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isBoolean(), "Missing boolean " + key); return object.get(key).getAsBoolean(); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
