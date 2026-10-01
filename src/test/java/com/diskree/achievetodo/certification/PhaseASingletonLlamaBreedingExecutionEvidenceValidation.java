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

/** Independent persistent validator for the animal/so_i_got_that_going_for_me singleton. */
public final class PhaseASingletonLlamaBreedingExecutionEvidenceValidation {
    public static final Path PERSISTENT_ARTIFACT = PhaseASingletonLlamaBreedingCertification.PERSISTENT;
    private PhaseASingletonLlamaBreedingExecutionEvidenceValidation() { }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        try { PhaseASingletonLlamaBreedingCertification.validate(root); }
        catch (Exception e) { throw new IllegalStateException("Frozen click catalog invalid", e); }
        JsonObject artifact = JsonParser.parseString(Files.readString(root.resolve(PERSISTENT_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        require("phase_a_singleton_llama_breeding_execution_evidence".equals(string(artifact, "snapshot"))
            && integer(artifact, "schemaVersion") == 1, "snapshot/schema mismatch");
        require(PhaseASingletonLlamaBreedingCertification.FAMILY.equals(string(artifact, "family"))
            && PhaseASingletonLlamaBreedingCertification.SOURCE.equals(string(artifact, "source")), "artifact ownership mismatch");
        require("26.2".equals(string(artifact, "minecraftVersion")) && "compat_26_2_r15".equals(string(artifact, "compatibilityMarker")), "version mismatch");
        String hash = "sha-256:" + sha(Files.readAllBytes(root.resolve(PhaseASingletonLlamaBreedingCertification.SNAPSHOT)));
        require(hash.equals(string(artifact, "catalogFingerprint")), "catalog hash mismatch");
        String runId = string(artifact, "runId"); java.util.UUID.fromString(runId);
        JsonArray entries = artifact.getAsJsonArray("entries"); require(entries != null && entries.size() == 1, "exact singleton receipt missing");
        JsonObject r = entries.get(0).getAsJsonObject();
        require(runId.equals(string(r, "runId")) && hash.equals(string(r, "catalogFingerprint")), "receipt run/hash mismatch");
        require(PhaseASingletonLlamaBreedingCertification.FAMILY.equals(string(r, "family"))
            && PhaseASingletonLlamaBreedingCertification.SOURCE.equals(string(r, "source")), "receipt ownership mismatch");
        require(PhaseASingletonLlamaBreedingCertification.ADVANCEMENT.equals(string(r, "advancementId"))
            && PhaseASingletonLlamaBreedingCertification.CRITERION.equals(string(r, "criterion")), "criterion mismatch");
        require("minecraft:bred_animals".equals(string(r, "trigger"))
            && PhaseASingletonLlamaBreedingCertification.BOUNDARY.equals(string(r, "boundary")), "native boundary mismatch");
        require(PhaseASingletonLlamaBreedingCertification.ENTITY_TAG.equals(string(r, "checkedParentTag"))
            && PhaseASingletonLlamaBreedingCertification.ENTITY_TAG.equals(string(r, "checkedPartnerTag"))
            && PhaseASingletonLlamaBreedingCertification.SELECTED_ENTITY.equals(string(r, "observedParent"))
            && PhaseASingletonLlamaBreedingCertification.SELECTED_ENTITY.equals(string(r, "observedPartner"))
            && PhaseASingletonLlamaBreedingCertification.SELECTED_ENTITY.equals(string(r, "observedChild"))
            && PhaseASingletonLlamaBreedingCertification.FEED_ITEM.equals(string(r, "observedFeedItem")), "entity/feed predicate mismatch");
        require("GREEN".equals(string(r, "result")) && "SURVIVAL".equals(string(r, "gameMode")), "result/mode mismatch");
        java.util.UUID.fromString(string(r, "playerUuid")); java.util.UUID.fromString(string(r, "parentUuid"));
        java.util.UUID.fromString(string(r, "partnerUuid")); java.util.UUID.fromString(string(r, "childUuid"));
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "finiteMaterials",
            "parentTagMember", "partnerTagMember", "parentTamedBefore", "partnerTamedBefore", "parentAdultBefore",
            "partnerAdultBefore", "parentFedWithHay", "partnerFedWithHay", "parentInLoveAfterFeed",
            "partnerInLoveAfterFeed", "nativeChildSpawned", "parentCooldownAfterBreeding",
            "partnerCooldownAfterBreeding", "criterionAfter", "noDirectCriterionTrigger", "noManualAward"})
            require(bool(r, field), "missing runtime witness " + field);
        require(!bool(r, "criterionBefore") && integer(r, "hayConsumed") == 2
            && integer(r, "ticksToBreed") > 0 && integer(r, "ticksToBreed") <= 400, "transition/material bound mismatch");
        JsonObject gate = object(r, "productionGateWitness");
        require("LANDMARK_ONLY_FOR_LLAMA_FEEDING".equals(string(gate, "productionGate"))
            && !bool(gate, "parentLockedLandmark") && !bool(gate, "partnerLockedLandmark"), "production gate mismatch");
        JsonObject cleanup = object(r, "cleanup");
        require(bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled")
            && bool(cleanup, "parentRemoved") && bool(cleanup, "partnerRemoved") && bool(cleanup, "childRemoved")
            && integer(cleanup, "settlementMessages") >= 0 && integer(cleanup, "warningCount") == 0, "cleanup mismatch");
        String advancement = PhaseASingletonLlamaBreedingCertification.ADVANCEMENT, criterion = PhaseASingletonLlamaBreedingCertification.CRITERION;
        String family = PhaseASingletonLlamaBreedingCertification.FAMILY, source = PhaseASingletonLlamaBreedingCertification.SOURCE;
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
