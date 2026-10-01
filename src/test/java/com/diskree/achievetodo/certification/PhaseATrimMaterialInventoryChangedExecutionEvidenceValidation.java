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

/** Independent persistent receipt reader for the exact 22 trim-material criteria. */
public final class PhaseATrimMaterialInventoryChangedExecutionEvidenceValidation {
    public static final Path PERSISTENT_ARTIFACT = Path.of("src/test/resources/phase_a_certification/trim_material_inventory_changed_execution_evidence.json");
    private static final String FAMILY = "TRIM_MATERIAL_INVENTORY_CHANGED";
    private static final String SOURCE = "PhaseATrimMaterialInventoryChangedGameTest";
    private PhaseATrimMaterialInventoryChangedExecutionEvidenceValidation() { }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        try { PhaseATrimMaterialInventoryChangedCertification.validate(root); }
        catch (Exception e) { throw new IllegalStateException("Trim-material catalog is invalid", e); }
        JsonObject artifact = JsonParser.parseString(Files.readString(root.resolve(PERSISTENT_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        require("phase_a_trim_material_inventory_changed_execution_evidence".equals(string(artifact, "snapshot")), "snapshot mismatch");
        require(integer(artifact, "schemaVersion") == 1, "schema mismatch");
        require(FAMILY.equals(string(artifact, "family")) && SOURCE.equals(string(artifact, "source")), "family/source mismatch");
        require("26.2".equals(string(artifact, "minecraftVersion")) && "compat_26_2_r15".equals(string(artifact, "compatibilityMarker")), "version mismatch");
        String fingerprint = "sha-256:" + sha(Files.readAllBytes(root.resolve(PhaseATrimMaterialInventoryChangedCertification.SNAPSHOT)));
        require(fingerprint.equals(string(artifact, "catalogFingerprint")), "catalog fingerprint mismatch");
        String runId = string(artifact, "runId"); java.util.UUID.fromString(runId);
        Map<String, PhaseATrimMaterialInventoryChangedCertification.Case> definitions = new LinkedHashMap<>();
        for (var definition : PhaseATrimMaterialInventoryChangedCertification.cases()) definitions.put(definition.key(), definition);
        JsonArray entries = artifact.getAsJsonArray("entries"); require(entries != null && entries.size() == definitions.size(), "receipt count mismatch");
        Set<String> keys = new LinkedHashSet<>(), players = new LinkedHashSet<>();
        Map<String, RuntimeEvidenceData> result = new LinkedHashMap<>();
        for (JsonElement element : entries) {
            require(element.isJsonObject(), "non-object receipt");
            JsonObject receipt = element.getAsJsonObject();
            String advancement = string(receipt, "advancementId"), criterion = string(receipt, "criterion");
            String key = advancement + "#" + criterion;
            var definition = definitions.get(key); require(definition != null && keys.add(key), "unexpected or duplicate receipt " + key);
            require(runId.equals(string(receipt, "runId")) && fingerprint.equals(string(receipt, "catalogFingerprint")), "run/fingerprint mismatch " + key);
            require(FAMILY.equals(string(receipt, "family")) && SOURCE.equals(string(receipt, "source")), "receipt ownership mismatch " + key);
            require("GREEN".equals(string(receipt, "result")) && "minecraft:inventory_changed".equals(string(receipt, "trigger")), "result/trigger mismatch " + key);
            require(definition.materialId().equals(string(receipt, "materialId")) && definition.materialId().equals(string(receipt, "observedTrimMaterial")), "observed material mismatch " + key);
            require(definition.requirementGroup() == integer(receipt, "requirementGroup")
                && definition.witness().equals(string(receipt, "witness")) && definition.productionGate().equals(string(receipt, "productionGate")), "catalog semantics mismatch " + key);
            String boundary = definition.witness().equals("native_item_entity_pickup")
                ? "ItemEntity.playerTouch->Inventory.add->InventoryChangedTrigger"
                : "ServerGamePacketListenerImpl.handleContainerClick->InventoryMenu->InventoryChangedTrigger";
            require(boundary.equals(string(receipt, "boundary")), "native boundary mismatch " + key);
            require("SURVIVAL".equals(string(receipt, "gameMode")) && bool(receipt, "joined") && bool(receipt, "connectionRegistered")
                && bool(receipt, "clientLoaded") && bool(receipt, "finiteMaterials"), "joined lifecycle mismatch " + key);
            require(bool(receipt, "eventObserved") && !bool(receipt, "criterionBefore") && bool(receipt, "criterionAfter")
                && bool(receipt, "noDirectCriterionTrigger") && bool(receipt, "noManualAward"), "native transition mismatch " + key);
            require(integer(receipt, "ticksToCriterion") >= 1 && integer(receipt, "ticksToCriterion") <= 100, "criterion delay mismatch " + key);
            JsonObject gate = object(receipt, "productionUnlockWitness");
            if (definition.productionGate().equals("NONE")) require(!bool(gate, "gatePresent"), "unexpected production gate " + key);
            else {
                require(bool(gate, "gatePresent") && "EQUIP_IRON_ARMOR".equals(string(gate, "ability"))
                    && "bac_advancements".equals(string(gate, "scoreboardObjective"))
                    && "live_overworld_seed_abilities_configuration".equals(string(gate, "thresholdSource")), "wrong production gate " + key);
                require(integer(gate, "requiredThreshold") > 0 && integer(gate, "scoreBefore") == 0
                    && integer(gate, "scoreAfter") == integer(gate, "requiredThreshold")
                    && bool(gate, "abilityLockedBefore") && !bool(gate, "abilityLockedAfter"), "production unlock not proven " + key);
            }
            JsonObject cleanup = object(receipt, "cleanup");
            require(bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled")
                && integer(cleanup, "warningCount") == 0 && integer(cleanup, "settlementMessages") >= 0, "cleanup mismatch " + key);
            String playerId = string(receipt, "playerUuid"); java.util.UUID.fromString(playerId);
            require(players.add(playerId), "player identity reused " + key);
            RuntimeEvidenceData data = result.computeIfAbsent(advancement, RuntimeEvidenceData::empty);
            data.greenCriteria().add(criterion);
            data.criteriaByFamily().computeIfAbsent(FAMILY, ignored -> new TreeSet<>()).add(criterion);
            data.criteriaBySource().computeIfAbsent(SOURCE, ignored -> new TreeSet<>()).add(criterion);
            data.families().add(FAMILY); data.sources().add(SOURCE);
        }
        require(keys.equals(definitions.keySet()) && players.size() == definitions.size(), "exact trim-material coverage mismatch");
        return result;
    }
    private static String string(JsonObject object, String field) { require(object.has(field) && object.get(field).isJsonPrimitive() && !object.get(field).getAsString().isBlank(), "missing " + field); return object.get(field).getAsString(); }
    private static int integer(JsonObject object, String field) { require(object.has(field) && object.get(field).isJsonPrimitive() && object.get(field).getAsJsonPrimitive().isNumber(), "missing integer " + field); return object.get(field).getAsInt(); }
    private static boolean bool(JsonObject object, String field) { require(object.has(field) && object.get(field).isJsonPrimitive() && object.get(field).getAsJsonPrimitive().isBoolean(), "missing boolean " + field); return object.get(field).getAsBoolean(); }
    private static JsonObject object(JsonObject parent, String field) { require(parent.has(field) && parent.get(field).isJsonObject(), "missing object " + field); return parent.getAsJsonObject(field); }
    private static String sha(byte[] bytes) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static void require(boolean yes, String message) { if (!yes) throw new IllegalStateException(message); }
    public record RuntimeEvidenceData(String advancementId, Set<String> greenCriteria, Map<String, Set<String>> criteriaByFamily,
                                      Map<String, Set<String>> criteriaBySource, Set<String> families, Set<String> sources) {
        static RuntimeEvidenceData empty(String advancementId) {
            return new RuntimeEvidenceData(advancementId, new TreeSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashSet<>(), new LinkedHashSet<>());
        }
    }
}
