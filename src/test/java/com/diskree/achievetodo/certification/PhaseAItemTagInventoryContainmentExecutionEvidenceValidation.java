package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Independent fail-closed reader for promoted ITEM_TAG_INVENTORY_CONTAINMENT evidence. */
public final class PhaseAItemTagInventoryContainmentExecutionEvidenceValidation {
    public static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "item_tag_inventory_containment_execution_evidence.json");
    private PhaseAItemTagInventoryContainmentExecutionEvidenceValidation() { }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        JsonObject artifact = JsonParser.parseString(Files.readString(root.resolve(PERSISTENT_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        require("phase_a_item_tag_inventory_containment_execution_evidence".equals(string(artifact, "snapshot")), "snapshot mismatch");
        require(artifact.get("schemaVersion").getAsInt() == 1, "schema mismatch");
        require("ITEM_TAG_INVENTORY_CONTAINMENT".equals(string(artifact, "family")), "family mismatch");
        require("PhaseAItemTagInventoryContainmentGameTest".equals(string(artifact, "source")), "source mismatch");
        require("26.2".equals(string(artifact, "minecraftVersion")) && "compat_26_2_r15".equals(string(artifact, "compatibilityMarker")), "compatibility mismatch");
        String fingerprint = "sha-256:" + sha256(Files.readAllBytes(root.resolve(PhaseAItemTagInventoryContainmentCertification.SNAPSHOT)));
        require(fingerprint.equals(string(artifact, "catalogFingerprint")), "catalog fingerprint mismatch");
        String runId = string(artifact, "runId");
        Set<String> expected = new LinkedHashSet<>(); Map<String, PhaseAItemTagInventoryContainmentCertification.Case> cases = new LinkedHashMap<>();
        for (PhaseAItemTagInventoryContainmentCertification.Case definition : PhaseAItemTagInventoryContainmentCertification.CASES) { expected.add(definition.key()); cases.put(definition.key(), definition); }
        JsonArray entries = artifact.getAsJsonArray("entries"); require(entries != null && entries.size() == expected.size(), "receipt count mismatch");
        Set<String> actual = new LinkedHashSet<>(); Set<String> playerUuids = new LinkedHashSet<>(); Map<String, RuntimeEvidenceData> result = new LinkedHashMap<>();
        for (JsonElement element : entries) {
            require(element.isJsonObject(), "Non-object receipt"); JsonObject receipt = element.getAsJsonObject();
            String advancement = string(receipt, "advancementId"), criterion = string(receipt, "criterion"), key = advancement + "#" + criterion;
            PhaseAItemTagInventoryContainmentCertification.Case definition = cases.get(key); require(definition != null && actual.add(key), "Unexpected or duplicate receipt " + key);
            require(runId.equals(string(receipt, "runId")) && fingerprint.equals(string(receipt, "catalogFingerprint")), "run or fingerprint mismatch");
            require(definition.itemTag().equals(string(receipt, "itemTag")) && definition.predicate().equals(string(receipt, "predicate")), "catalog semantic mismatch " + key);
            require("GREEN".equals(string(receipt, "result")) && "minecraft:inventory_changed".equals(string(receipt, "trigger")) && "ItemEntity.playerTouch->Inventory.add->InventoryChangedTrigger".equals(string(receipt, "boundary")), "runtime path mismatch");
            require("SURVIVAL".equals(string(receipt, "gameMode")) && bool(receipt, "joined") && bool(receipt, "connectionRegistered") && bool(receipt, "clientLoaded"), "lifecycle mismatch");
            require(bool(receipt, "fixtureMatchesPredicate") && bool(receipt, "itemEntityConsumed") && !bool(receipt, "criterionBefore") && bool(receipt, "criterionAfter") && bool(receipt, "noDirectCriterionTrigger") && bool(receipt, "noManualAward"), "native transition mismatch");
            require(receipt.get("ticksToCriterion").getAsInt() >= 1 && receipt.get("ticksToCriterion").getAsInt() <= 100, "invalid criterion delay");
            JsonObject cleanup = receipt.getAsJsonObject("cleanup"); require(cleanup != null && bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled") && cleanup.get("warningCount").getAsInt() == 0, "cleanup mismatch");
            require(playerUuids.add(string(receipt, "playerUuid")), "joined player identity reused");
            RuntimeEvidenceData data = result.computeIfAbsent(advancement, RuntimeEvidenceData::empty); data.greenCriteria().add(criterion); data.criteriaByFamily().computeIfAbsent("ITEM_TAG_INVENTORY_CONTAINMENT", ignored -> new TreeSet<>()).add(criterion); data.criteriaBySource().computeIfAbsent("PhaseAItemTagInventoryContainmentGameTest", ignored -> new TreeSet<>()).add(criterion); data.families().add("ITEM_TAG_INVENTORY_CONTAINMENT"); data.sources().add("PhaseAItemTagInventoryContainmentGameTest");
        }
        require(actual.equals(expected) && playerUuids.size() == expected.size(), "Exact containment receipt coverage mismatch");
        return result;
    }
    private static String string(JsonObject json, String field) { require(json.has(field) && json.get(field).isJsonPrimitive() && !json.get(field).getAsString().isBlank(), "Missing " + field); return json.get(field).getAsString(); }
    private static boolean bool(JsonObject json, String field) { require(json.has(field) && json.get(field).isJsonPrimitive() && json.get(field).getAsJsonPrimitive().isBoolean(), "Missing boolean " + field); return json.get(field).getAsBoolean(); }
    private static String sha256(byte[] bytes) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    public record RuntimeEvidenceData(String advancementId, Set<String> greenCriteria, Map<String, Set<String>> criteriaByFamily, Map<String, Set<String>> criteriaBySource, Set<String> families, Set<String> sources) { static RuntimeEvidenceData empty(String advancementId) { return new RuntimeEvidenceData(advancementId, new TreeSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashSet<>(), new LinkedHashSet<>()); } }
}
