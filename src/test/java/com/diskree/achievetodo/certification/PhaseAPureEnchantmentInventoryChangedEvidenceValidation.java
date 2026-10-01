package com.diskree.achievetodo.certification;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;

/** Artifact-local independent parser; exact coverage is a set of frozen requirement witnesses. */
public final class PhaseAPureEnchantmentInventoryChangedEvidenceValidation {
    public static final Path TEMP = Path.of("build/tmp/phase_a_certification/pure_enchantment_inventory_changed_execution_evidence.json");
    public static final Path RUN_STATE = Path.of("build/tmp/phase_a_certification/pure_enchantment_inventory_changed_execution_evidence.run.json");
    private PhaseAPureEnchantmentInventoryChangedEvidenceValidation() { }
    public static JsonObject load(Path root, Path path, boolean exact, boolean active) throws Exception {
        var cases = PhaseAPureEnchantmentInventoryChangedCertification.cases(root);
        var expected = new LinkedHashMap<String, JsonObject>();
        for (var row : cases) expected.put(PhaseAPureEnchantmentInventoryChangedCertification.key(row), row);
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
            JsonObject receipt = entry.getAsJsonObject(); String key = PhaseAPureEnchantmentInventoryChangedCertification.key(receipt);
            require(allowed.contains(key) && keys.add(key), "Extra or duplicate receipt");
            validateReceipt(receipt, expected.get(key), runId, hash);
            require(players.add(string(receipt, "playerUuid")), "Player reused across cases");
        }
        require(keys.equals(new HashSet<>(allowed)), "Missing required receipts"); return artifact;
    }
    public static Set<String> canaryKeys(List<JsonObject> cases) {
        var keys = new LinkedHashSet<String>(); keys.add(PhaseAPureEnchantmentInventoryChangedCertification.key(cases.getFirst()));
        for (var row : cases) if (!row.get("storage").getAsString().equals("STORED_ENCHANTMENTS")) keys.add(PhaseAPureEnchantmentInventoryChangedCertification.key(row));
        require(keys.size() == 3, "Canary scope changed"); return keys;
    }
    public static void validateReceipt(JsonObject receipt, JsonObject row, String runId, String hash) {
        require(row != null, "Unknown receipt");
        require(string(receipt, "family").equals(PhaseAPureEnchantmentInventoryChangedCertification.FAMILY)
            && string(receipt, "source").equals(PhaseAPureEnchantmentInventoryChangedCertification.SOURCE), "Receipt ownership mismatch");
        require(string(receipt, "runId").equals(runId) && string(receipt, "catalogFingerprint").equals(hash), "Receipt run/hash mismatch");
        require(string(receipt, "result").equals("GREEN") && string(receipt, "gameMode").equals("SURVIVAL"), "Result/mode mismatch"); UUID.fromString(string(receipt, "playerUuid"));
        for (String field : new String[]{"advancementId", "criterion", "selectedItem", "storage", "action"}) require(string(receipt, field).equals(string(row, field)), "Witness mismatch " + field);
        require(receipt.get("observedCustomName").getAsString().equals(row.get("customName").getAsString()), "Custom name mismatch");
        require(integer(receipt, "requirementGroup") == integer(row, "requirementGroup"), "Wrong requirement group");
        require(receipt.getAsJsonObject("observedEnchantments").equals(row.getAsJsonObject("configuredEnchantments")), "Live enchantment component mismatch");
        boolean equip = string(row, "action").equals("EQUIP_HEAD");
        require(string(receipt, "trigger").equals("minecraft:inventory_changed")
            && string(receipt, "boundary").equals(equip ? PhaseAPureEnchantmentInventoryChangedCertification.EQUIP : PhaseAPureEnchantmentInventoryChangedCertification.PICKUP), "Native boundary mismatch");
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "finiteMaterials", "eventObserved", "matchingStackAfter", "componentStorageVerified", "criterionAfter", "noDirectCriterionTrigger", "noManualAward"}) require(bool(receipt, field), "Missing witness " + field);
        require(!bool(receipt, "criterionBefore") && integer(receipt, "ticksToComplete") > 0 && integer(receipt, "ticksToComplete") <= 80, "False-to-true transition missing");
        if (equip) require(bool(receipt, "headOccupiedAfter") && bool(receipt, "headEmptyBefore") && bool(receipt, "packetSent") && bool(receipt, "noProductionAbilityGate"), "Equipment event missing");
        else require(bool(receipt, "itemEntityConsumed") && integer(receipt, "pickedUpCount") == 1, "Native finite pickup missing");
        var cleanup = receipt.getAsJsonObject("cleanup");
        for (String field : new String[]{"playerRemoved", "connectionRemoved", "channelSettled", "itemEntityRemoved"}) require(bool(cleanup, field), "Cleanup incomplete " + field);
        require(integer(cleanup, "warningCount") == 0 && integer(cleanup, "settlementMessages") >= 0, "Cleanup warning debt");
    }
    public static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadValidatedRuntimeEvidence(Path root) throws java.io.IOException {
        try { return loadPersistent(root); }
        catch (Exception failure) { throw new java.io.IOException("Invalid persistent pure-enchantment evidence", failure); }
    }
    private static Map<String, PhaseAAdvancementRollup.RuntimeEvidence> loadPersistent(Path root) throws Exception {
        JsonObject artifact = load(root, PhaseAPureEnchantmentInventoryChangedCertification.PERSISTENT, true, false);
        Map<String, Set<String>> criteria = new TreeMap<>();
        for (var entry : artifact.getAsJsonArray("entries")) { var receipt = entry.getAsJsonObject(); criteria.computeIfAbsent(string(receipt, "advancementId"), key -> new TreeSet<>()).add(string(receipt, "criterion")); }
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> result = new LinkedHashMap<>();
        for (var entry : criteria.entrySet()) result.put(entry.getKey(), new PhaseAAdvancementRollup.RuntimeEvidence(entry.getKey(), entry.getValue(),
            Map.of(PhaseAPureEnchantmentInventoryChangedCertification.FAMILY, entry.getValue()), Map.of(PhaseAPureEnchantmentInventoryChangedCertification.SOURCE, entry.getValue()),
            Set.of(PhaseAPureEnchantmentInventoryChangedCertification.FAMILY), Set.of(PhaseAPureEnchantmentInventoryChangedCertification.SOURCE)));
        return result;
    }
    public static String fingerprint(Path root) throws Exception { return "sha-256:" + PhaseAPureEnchantmentInventoryChangedCertification.sha(Files.readAllBytes(root.resolve(PhaseAPureEnchantmentInventoryChangedCertification.CATALOG))); }
    public static void common(JsonObject artifact, String hash) {
        require(string(artifact, "snapshot").equals("phase_a_pure_enchantment_inventory_changed_execution_evidence") && integer(artifact, "schemaVersion") == 1, "Schema mismatch");
        require(string(artifact, "family").equals(PhaseAPureEnchantmentInventoryChangedCertification.FAMILY) && string(artifact, "source").equals(PhaseAPureEnchantmentInventoryChangedCertification.SOURCE), "Artifact ownership mismatch");
        require(string(artifact, "minecraftVersion").equals("26.2") && string(artifact, "compatibilityMarker").equals("compat_26_2_r15"), "Compatibility mismatch");
        require(string(artifact, "catalogFingerprint").equals(hash), "Catalog changed"); UUID.fromString(string(artifact, "runId"));
    }
    public static JsonObject read(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
    private static String string(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && !object.get(key).getAsString().isBlank(), "Missing " + key); return object.get(key).getAsString(); }
    private static int integer(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isNumber(), "Missing integer " + key); return object.get(key).getAsInt(); }
    private static boolean bool(JsonObject object, String key) { require(object.has(key) && object.get(key).isJsonPrimitive() && object.get(key).getAsJsonPrimitive().isBoolean(), "Missing boolean " + key); return object.get(key).getAsBoolean(); }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
}
