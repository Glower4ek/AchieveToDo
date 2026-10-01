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

/** Independent persistent validator for the Allay potion pickup singleton. */
public final class PhaseASingletonDiagonAllayExecutionEvidenceValidation {
    public static final Path PERSISTENT_ARTIFACT = PhaseASingletonDiagonAllayCertification.PERSISTENT;
    private PhaseASingletonDiagonAllayExecutionEvidenceValidation() { }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        try { PhaseASingletonDiagonAllayCertification.validate(root); }
        catch (Exception e) { throw new IllegalStateException("Frozen Allay catalog invalid", e); }
        JsonObject artifact = JsonParser.parseString(Files.readString(root.resolve(PERSISTENT_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        require("phase_a_singleton_diagon_allay_execution_evidence".equals(string(artifact, "snapshot"))
            && integer(artifact, "schemaVersion") == 1, "snapshot/schema mismatch");
        require(PhaseASingletonDiagonAllayCertification.FAMILY.equals(string(artifact, "family"))
            && PhaseASingletonDiagonAllayCertification.SOURCE.equals(string(artifact, "source")), "artifact ownership mismatch");
        require("26.2".equals(string(artifact, "minecraftVersion"))
            && "compat_26_2_r15".equals(string(artifact, "compatibilityMarker")), "version mismatch");
        String hash = "sha-256:" + sha(Files.readAllBytes(root.resolve(PhaseASingletonDiagonAllayCertification.SNAPSHOT)));
        require(hash.equals(string(artifact, "catalogFingerprint")), "catalog hash mismatch");
        String runId = string(artifact, "runId"); java.util.UUID.fromString(runId);
        JsonArray entries = artifact.getAsJsonArray("entries"); require(entries != null && entries.size() == 1, "exact singleton receipt missing");
        JsonObject r = entries.get(0).getAsJsonObject();
        require(runId.equals(string(r, "runId")) && hash.equals(string(r, "catalogFingerprint")), "receipt run/hash mismatch");
        require(PhaseASingletonDiagonAllayCertification.FAMILY.equals(string(r, "family"))
            && PhaseASingletonDiagonAllayCertification.SOURCE.equals(string(r, "source")), "receipt ownership mismatch");
        require(PhaseASingletonDiagonAllayCertification.ADVANCEMENT.equals(string(r, "advancementId"))
            && PhaseASingletonDiagonAllayCertification.CRITERION.equals(string(r, "criterion")), "criterion mismatch");
        require("minecraft:thrown_item_picked_up_by_player".equals(string(r, "trigger"))
            && PhaseASingletonDiagonAllayCertification.BOUNDARY.equals(string(r, "boundary")), "native boundary mismatch");
        require(PhaseASingletonDiagonAllayCertification.ENTITY.equals(string(r, "observedThrower"))
            && PhaseASingletonDiagonAllayCertification.ITEM.equals(string(r, "observedItem"))
            && PhaseASingletonDiagonAllayCertification.BIOME.equals(string(r, "observedPlayerBiome")), "observed predicate mismatch");
        require("GREEN".equals(string(r, "result")) && "SURVIVAL".equals(string(r, "gameMode")), "result/mode mismatch");
        java.util.UUID.fromString(string(r, "playerUuid")); java.util.UUID.fromString(string(r, "allayUuid"));
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "finiteMaterials",
            "allayGivenMatchingItem", "allayPickedUpPotion", "allayThrewPotion", "thrownEntityOwnerMatchesAllay",
            "potionEntityConsumed", "matchingPotionInPlayerInventory", "criterionAfter", "noDirectCriterionTrigger", "noManualAward"})
            require(bool(r, field), "missing native witness " + field);
        require(!bool(r, "criterionBefore"), "pre-action criterion mismatch");
        JsonObject gate = object(r, "productionGateWitness");
        require("LANDMARK_ONLY_FOR_ALLAY_INTERACTION".equals(string(gate, "productionGate"))
            && !bool(gate, "lockedLandmark"), "production gate mismatch");
        JsonObject cleanup = object(r, "cleanup");
        require(bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled")
            && bool(cleanup, "allayRemoved") && integer(cleanup, "warningCount") == 0, "cleanup mismatch");
        String advancement = PhaseASingletonDiagonAllayCertification.ADVANCEMENT;
        String criterion = PhaseASingletonDiagonAllayCertification.CRITERION;
        String family = PhaseASingletonDiagonAllayCertification.FAMILY;
        String source = PhaseASingletonDiagonAllayCertification.SOURCE;
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
