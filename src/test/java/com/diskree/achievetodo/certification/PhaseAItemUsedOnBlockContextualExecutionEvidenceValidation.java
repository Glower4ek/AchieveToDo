package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
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

/** Independent test-side validation of the persisted native jukebox receipt. */
public final class PhaseAItemUsedOnBlockContextualExecutionEvidenceValidation {
    public static final Path PERSISTENT_ARTIFACT = PhaseAItemUsedOnBlockContextualCertification.PERSISTENT_EVIDENCE;
    private PhaseAItemUsedOnBlockContextualExecutionEvidenceValidation() { }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        try { PhaseAItemUsedOnBlockContextualCertification.validate(root); }
        catch (Exception e) { throw new IllegalStateException("Frozen contextual catalog invalid", e); }
        JsonObject artifact = JsonParser.parseString(Files.readString(root.resolve(PERSISTENT_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        require("phase_a_item_used_on_block_contextual_execution_evidence".equals(string(artifact, "snapshot"))
            && integer(artifact, "schemaVersion") == 1, "snapshot/schema mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.FAMILY.equals(string(artifact, "family"))
            && PhaseAItemUsedOnBlockContextualCertification.SOURCE.equals(string(artifact, "source")), "artifact ownership mismatch");
        require("26.2".equals(string(artifact, "minecraftVersion")) && "compat_26_2_r15".equals(string(artifact, "compatibilityMarker")), "version mismatch");
        String fingerprint = "sha-256:" + sha(Files.readAllBytes(root.resolve(PhaseAItemUsedOnBlockContextualCertification.SNAPSHOT)));
        require(fingerprint.equals(string(artifact, "catalogFingerprint")), "catalog fingerprint mismatch");
        String runId = string(artifact, "runId"); java.util.UUID.fromString(runId);
        JsonArray entries = artifact.getAsJsonArray("entries"); require(entries != null && entries.size() == 1, "exact singleton receipt missing");
        JsonObject receipt = entries.get(0).getAsJsonObject();
        require(runId.equals(string(receipt, "runId")) && fingerprint.equals(string(receipt, "catalogFingerprint")), "receipt identity mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.FAMILY.equals(string(receipt, "family"))
            && PhaseAItemUsedOnBlockContextualCertification.SOURCE.equals(string(receipt, "source")), "receipt ownership mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID.equals(string(receipt, "advancementId"))
            && PhaseAItemUsedOnBlockContextualCertification.CRITERION.equals(string(receipt, "criterion")), "frozen criterion mismatch");
        require("minecraft:item_used_on_block".equals(string(receipt, "trigger"))
            && PhaseAItemUsedOnBlockContextualCertification.BOUNDARY.equals(string(receipt, "boundary")), "native trigger boundary mismatch");
        require(PhaseAItemUsedOnBlockContextualCertification.BIOME.equals(string(receipt, "observedBiome"))
            && PhaseAItemUsedOnBlockContextualCertification.BLOCK.equals(string(receipt, "observedBlock"))
            && PhaseAItemUsedOnBlockContextualCertification.DISC.equals(string(receipt, "observedDisc")), "live location/tool mismatch");
        require("GREEN".equals(string(receipt, "result")) && "SURVIVAL".equals(string(receipt, "gameMode")), "result/mode mismatch");
        java.util.UUID.fromString(string(receipt, "playerUuid"));
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "finiteMaterials", "toolPlayable",
            "jukeboxAcceptedDisc", "criterionAfter", "noDirectCriterionTrigger", "noManualAward"})
            require(bool(receipt, field), "missing live precondition " + field);
        require(!bool(receipt, "criterionBefore") && integer(receipt, "discCountBefore") == 1
            && integer(receipt, "discCountAfter") == 0, "criterion/disc transition mismatch");
        JsonObject gate = object(receipt, "productionUnlockWitness");
        require("USE_JUKEBOX".equals(string(gate, "ability")) && "bac_advancements".equals(string(gate, "scoreboardObjective"))
            && "live_overworld_seed_abilities_configuration".equals(string(gate, "thresholdSource")), "production gate source mismatch");
        require(integer(gate, "requiredThreshold") > 0 && integer(gate, "scoreBefore") == 0
            && integer(gate, "scoreAfter") == integer(gate, "requiredThreshold")
            && bool(gate, "abilityLockedBefore") && !bool(gate, "abilityLockedAfter"), "production gate not proven");
        JsonObject cleanup = object(receipt, "cleanup");
        require(bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled")
            && integer(cleanup, "settlementMessages") >= 0 && integer(cleanup, "warningCount") == 0, "cleanup mismatch");
        String advancement = PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID;
        String criterion = PhaseAItemUsedOnBlockContextualCertification.CRITERION;
        String family = PhaseAItemUsedOnBlockContextualCertification.FAMILY;
        String source = PhaseAItemUsedOnBlockContextualCertification.SOURCE;
        Map<String, RuntimeEvidenceData> result = new LinkedHashMap<>();
        result.put(advancement, new RuntimeEvidenceData(advancement, new TreeSet<>(Set.of(criterion)),
            Map.of(family, new TreeSet<>(Set.of(criterion))), Map.of(source, new TreeSet<>(Set.of(criterion))),
            new LinkedHashSet<>(Set.of(family)), new LinkedHashSet<>(Set.of(source))));
        return result;
    }
    private static String string(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonPrimitive() && !o.get(f).getAsString().isBlank(), "missing " + f); return o.get(f).getAsString(); }
    private static int integer(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonPrimitive() && o.get(f).getAsJsonPrimitive().isNumber(), "missing integer " + f); return o.get(f).getAsInt(); }
    private static boolean bool(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonPrimitive() && o.get(f).getAsJsonPrimitive().isBoolean(), "missing boolean " + f); return o.get(f).getAsBoolean(); }
    private static JsonObject object(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonObject(), "missing object " + f); return o.getAsJsonObject(f); }
    private static String sha(byte[] bytes) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
    public record RuntimeEvidenceData(String advancementId, Set<String> greenCriteria, Map<String, Set<String>> criteriaByFamily,
                                      Map<String, Set<String>> criteriaBySource, Set<String> families, Set<String> sources) { }
}
