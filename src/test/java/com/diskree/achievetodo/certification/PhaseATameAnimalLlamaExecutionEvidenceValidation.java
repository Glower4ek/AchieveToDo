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

/** Test-side independent validator for promoted llama runtime receipts. */
public final class PhaseATameAnimalLlamaExecutionEvidenceValidation {
    public static final Path PERSISTENT_ARTIFACT = Path.of("src", "test", "resources", "phase_a_certification",
        "tame_animal_llama_execution_evidence.json");

    private PhaseATameAnimalLlamaExecutionEvidenceValidation() { }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        JsonObject artifact = JsonParser.parseString(Files.readString(root.resolve(PERSISTENT_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        require("phase_a_tame_animal_llama_execution_evidence".equals(string(artifact, "snapshot")), "snapshot mismatch");
        require("TAME_ANIMAL_LLAMA".equals(string(artifact, "family")), "family mismatch");
        require("PhaseATameAnimalLlamaGameTest".equals(string(artifact, "source")), "source mismatch");
        String fingerprint = "sha-256:" + sha256(Files.readAllBytes(root.resolve(PhaseATameAnimalLlamaCertification.SNAPSHOT)));
        require(fingerprint.equals(string(artifact, "catalogFingerprint")), "catalog fingerprint mismatch");
        String runId = string(artifact, "runId");
        Set<String> expected = new LinkedHashSet<>();
        for (PhaseATameAnimalLlamaCertification.Case definition : PhaseATameAnimalLlamaCertification.CASES) expected.add(definition.key());
        Set<String> actual = new LinkedHashSet<>();
        Map<String, RuntimeEvidenceData> result = new LinkedHashMap<>();
        JsonArray entries = artifact.getAsJsonArray("entries");
        require(entries != null, "Missing receipts");
        for (JsonElement element : entries) {
            require(element.isJsonObject(), "Non-object receipt");
            JsonObject receipt = element.getAsJsonObject();
            String advancementId = string(receipt, "advancementId");
            String criterion = string(receipt, "criterion");
            String key = advancementId + "#" + criterion;
            require(expected.contains(key) && actual.add(key), "Unexpected or duplicate receipt " + key);
            require(runId.equals(string(receipt, "runId")), "runId mismatch for " + key);
            require(fingerprint.equals(string(receipt, "catalogFingerprint")), "fingerprint mismatch for " + key);
            require("GREEN".equals(string(receipt, "result")), "non-GREEN receipt " + key);
            require("minecraft:tame_animal".equals(string(receipt, "trigger")), "trigger mismatch " + key);
            require(bool(receipt, "criterionBefore") == false && bool(receipt, "criterionAfter"), "transition mismatch " + key);
            require(bool(receipt, "nativeTameTransition") && bool(receipt, "noDirectCriterionTrigger") && bool(receipt, "noManualAward"),
                "native proof mismatch " + key);
            require("SURVIVAL".equals(string(receipt, "gameMode")) && bool(receipt, "joined") && bool(receipt, "connectionRegistered")
                && bool(receipt, "clientLoaded") && bool(receipt, "normalScheduler"), "lifecycle mismatch " + key);
            int ticks = receipt.get("ticksToCriterion").getAsInt();
            require(ticks >= 1 && ticks <= 600, "unbounded tame wait " + key);
            JsonObject cleanup = receipt.getAsJsonObject("cleanup");
            require(cleanup != null && bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved")
                && bool(cleanup, "channelSettled") && cleanup.get("warningCount").getAsInt() == 0, "cleanup mismatch " + key);
            RuntimeEvidenceData data = result.computeIfAbsent(advancementId, RuntimeEvidenceData::empty);
            data.greenCriteria().add(criterion);
            data.criteriaByFamily().computeIfAbsent("TAME_ANIMAL_LLAMA", ignored -> new TreeSet<>()).add(criterion);
            data.criteriaBySource().computeIfAbsent("PhaseATameAnimalLlamaGameTest", ignored -> new TreeSet<>()).add(criterion);
            data.families().add("TAME_ANIMAL_LLAMA");
            data.sources().add("PhaseATameAnimalLlamaGameTest");
        }
        require(expected.equals(actual), "Exact llama receipt coverage mismatch expected=" + expected + " actual=" + actual);
        return result;
    }

    private static String string(JsonObject json, String field) { require(json.has(field) && json.get(field).isJsonPrimitive() && !json.get(field).getAsString().isBlank(), "Missing " + field); return json.get(field).getAsString(); }
    private static boolean bool(JsonObject json, String field) { require(json.has(field) && json.get(field).isJsonPrimitive() && json.get(field).getAsJsonPrimitive().isBoolean(), "Missing boolean " + field); return json.get(field).getAsBoolean(); }
    private static String sha256(byte[] bytes) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }

    public record RuntimeEvidenceData(String advancementId, Set<String> greenCriteria, Map<String, Set<String>> criteriaByFamily,
                                      Map<String, Set<String>> criteriaBySource, Set<String> families, Set<String> sources) {
        static RuntimeEvidenceData empty(String advancementId) { return new RuntimeEvidenceData(advancementId, new TreeSet<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashSet<>(), new LinkedHashSet<>()); }
    }
}
