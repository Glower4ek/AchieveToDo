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

/** Independent test-side validator for the promoted native wax_on recipe receipt. */
public final class PhaseARecipeCraftedWaxOnExecutionEvidenceValidation {
    public static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification", "recipe_crafted_wax_on_execution_evidence.json");
    private PhaseARecipeCraftedWaxOnExecutionEvidenceValidation() { }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        JsonObject artifact = JsonParser.parseString(Files.readString(root.resolve(PERSISTENT_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        require("phase_a_recipe_crafted_wax_on_execution_evidence".equals(string(artifact, "snapshot")), "snapshot mismatch");
        require("RECIPE_CRAFTED_WAX_ON".equals(string(artifact, "family")), "family mismatch");
        require("PhaseARecipeCraftedWaxOnGameTest".equals(string(artifact, "source")), "source mismatch");
        String fingerprint = "sha-256:" + sha256(Files.readAllBytes(root.resolve(PhaseARecipeCraftedWaxOnCertification.SNAPSHOT)));
        require(fingerprint.equals(string(artifact, "catalogFingerprint")), "catalog fingerprint mismatch");
        String runId = string(artifact, "runId");
        String expectedKey = PhaseARecipeCraftedWaxOnCertification.ADVANCEMENT_ID + "#" + PhaseARecipeCraftedWaxOnCertification.SELECTED_CRITERION;
        JsonArray entries = artifact.getAsJsonArray("entries");
        require(entries != null && entries.size() == 1, "Expected exactly one wax_on receipt");
        Set<String> actual = new LinkedHashSet<>();
        Map<String, RuntimeEvidenceData> result = new LinkedHashMap<>();
        for (JsonElement element : entries) {
            require(element.isJsonObject(), "Non-object receipt"); JsonObject receipt = element.getAsJsonObject();
            String advancement = string(receipt, "advancementId"); String criterion = string(receipt, "criterion"); String key = advancement + "#" + criterion;
            require(expectedKey.equals(key) && actual.add(key), "Unexpected or duplicate receipt " + key);
            require(runId.equals(string(receipt, "runId")) && fingerprint.equals(string(receipt, "catalogFingerprint")), "run or fingerprint mismatch");
            require("GREEN".equals(string(receipt, "result")) && "minecraft:recipe_crafted".equals(string(receipt, "trigger")), "result or trigger mismatch");
            require(PhaseARecipeCraftedWaxOnCertification.SELECTED_RECIPE.equals(string(receipt, "recipeId")), "recipe mismatch");
            require("minecraft:copper_block".equals(string(receipt, "inputA")) && "minecraft:honeycomb".equals(string(receipt, "inputB")) && "minecraft:waxed_copper_block".equals(string(receipt, "producedItem")), "crafted state mismatch");
            require(!bool(receipt, "criterionBefore") && bool(receipt, "criterionAfter") && bool(receipt, "craftingTableInteraction") && bool(receipt, "normalResultTaken") && bool(receipt, "noDirectCriterionTrigger") && bool(receipt, "noManualAward"), "native transition mismatch");
            require("SURVIVAL".equals(string(receipt, "gameMode")) && bool(receipt, "joined") && bool(receipt, "connectionRegistered") && bool(receipt, "clientLoaded"), "lifecycle mismatch");
            int ticks = receipt.get("ticksToCriterion").getAsInt(); require(ticks >= 1 && ticks <= 100, "invalid criterion delay");
            JsonObject cleanup = receipt.getAsJsonObject("cleanup"); require(cleanup != null && bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled") && cleanup.get("warningCount").getAsInt() == 0, "cleanup mismatch");
            RuntimeEvidenceData data = result.computeIfAbsent(advancement, RuntimeEvidenceData::empty);
            data.greenCriteria().add(criterion); data.criteriaByFamily().computeIfAbsent("RECIPE_CRAFTED_WAX_ON", ignored -> new TreeSet<>()).add(criterion); data.criteriaBySource().computeIfAbsent("PhaseARecipeCraftedWaxOnGameTest", ignored -> new TreeSet<>()).add(criterion); data.families().add("RECIPE_CRAFTED_WAX_ON"); data.sources().add("PhaseARecipeCraftedWaxOnGameTest");
        }
        require(actual.equals(Set.of(expectedKey)), "Exact wax_on receipt coverage mismatch");
        return result;
    }
    private static String string(JsonObject json, String field) { require(json.has(field) && json.get(field).isJsonPrimitive() && !json.get(field).getAsString().isBlank(), "Missing " + field); return json.get(field).getAsString(); }
    private static boolean bool(JsonObject json, String field) { require(json.has(field) && json.get(field).isJsonPrimitive() && json.get(field).getAsJsonPrimitive().isBoolean(), "Missing boolean " + field); return json.get(field).getAsBoolean(); }
    private static String sha256(byte[] bytes) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
    public record RuntimeEvidenceData(String advancementId, Set<String> greenCriteria, Map<String, Set<String>> criteriaByFamily, Map<String, Set<String>> criteriaBySource, Set<String> families, Set<String> sources) { static RuntimeEvidenceData empty(String advancementId) { return new RuntimeEvidenceData(advancementId, new TreeSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashSet<>(), new LinkedHashSet<>()); } }
}
