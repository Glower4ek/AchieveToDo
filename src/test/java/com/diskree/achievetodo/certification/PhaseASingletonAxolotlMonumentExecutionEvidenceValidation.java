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
import java.util.UUID;

/** Independent persistent validator for the animal/thanks_a_lotl singleton. */
public final class PhaseASingletonAxolotlMonumentExecutionEvidenceValidation {
    public static final Path PERSISTENT_ARTIFACT = PhaseASingletonAxolotlMonumentCertification.PERSISTENT;
    private PhaseASingletonAxolotlMonumentExecutionEvidenceValidation() { }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        try { PhaseASingletonAxolotlMonumentCertification.validate(root); }
        catch (Exception e) { throw new IllegalStateException("Frozen axolotl monument catalog invalid", e); }
        JsonObject artifact = JsonParser.parseString(Files.readString(root.resolve(PERSISTENT_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        require("phase_a_singleton_axolotl_monument_execution_evidence".equals(string(artifact, "snapshot"))
            && integer(artifact, "schemaVersion") == 1, "snapshot/schema mismatch");
        require(PhaseASingletonAxolotlMonumentCertification.FAMILY.equals(string(artifact, "family"))
            && PhaseASingletonAxolotlMonumentCertification.SOURCE.equals(string(artifact, "source")), "artifact ownership mismatch");
        require("26.2".equals(string(artifact, "minecraftVersion"))
            && "compat_26_2_r15".equals(string(artifact, "compatibilityMarker")), "version mismatch");
        String hash = "sha-256:" + sha(Files.readAllBytes(root.resolve(PhaseASingletonAxolotlMonumentCertification.SNAPSHOT)));
        require(hash.equals(string(artifact, "catalogFingerprint")), "catalog hash mismatch");
        String runId = string(artifact, "runId"); UUID.fromString(runId);
        JsonArray entries = artifact.getAsJsonArray("entries");
        require(entries != null && entries.size() == 1, "exact singleton receipt missing or extra");
        JsonObject r = entries.get(0).getAsJsonObject();
        require(runId.equals(string(r, "runId")) && hash.equals(string(r, "catalogFingerprint")), "receipt run/hash mismatch");
        require(PhaseASingletonAxolotlMonumentCertification.FAMILY.equals(string(r, "family"))
            && PhaseASingletonAxolotlMonumentCertification.SOURCE.equals(string(r, "source")), "receipt ownership mismatch");
        require(PhaseASingletonAxolotlMonumentCertification.ADVANCEMENT.equals(string(r, "advancementId"))
            && PhaseASingletonAxolotlMonumentCertification.CRITERION.equals(string(r, "criterion")), "criterion mismatch");
        require("minecraft:effects_changed".equals(string(r, "trigger"))
            && PhaseASingletonAxolotlMonumentCertification.BOUNDARY.equals(string(r, "boundary")), "native boundary mismatch");
        require(PhaseASingletonAxolotlMonumentCertification.SOURCE_ENTITY.equals(string(r, "observedSourceEntity"))
            && PhaseASingletonAxolotlMonumentCertification.STRUCTURE.equals(string(r, "observedStructure"))
            && "minecraft:tropical_fish".equals(string(r, "observedTargetEntity")), "source/structure/target mismatch");
        require("GREEN".equals(string(r, "result")) && "SURVIVAL".equals(string(r, "gameMode")), "result/mode mismatch");
        UUID player = UUID.fromString(string(r, "playerUuid"));
        UUID axolotl = UUID.fromString(string(r, "axolotlUuid"));
        UUID target = UUID.fromString(string(r, "targetUuid"));
        require(!player.equals(axolotl) && !player.equals(target) && !axolotl.equals(target), "entity UUIDs not unique");
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "finiteMaterials", "emptyHand",
            "sourceStructureLookup", "playerStructureLookup", "nativeAxolotlTargetAcquired", "targetKilledByPlayer",
            "regenerationFromAxolotl", "criterionAfter", "noDirectCriterionTrigger", "noManualAward"})
            require(bool(r, field), "missing native witness " + field);
        require(!bool(r, "criterionBefore") && integer(r, "ticksToAssist") > 0
            && integer(r, "ticksToAssist") <= 240, "transition or tick bound mismatch");
        JsonObject gate = object(r, "productionGateWitness");
        require("LANDMARK_ONLY_FOR_PLAYER_ATTACK".equals(string(gate, "productionGate"))
            && !bool(gate, "lockedLandmark"), "production gate mismatch");
        JsonObject cleanup = object(r, "cleanup");
        require(bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled")
            && bool(cleanup, "structureRestored") && bool(cleanup, "axolotlRemoved") && bool(cleanup, "targetRemoved")
            && integer(cleanup, "settlementMessages") >= 0 && integer(cleanup, "warningCount") == 0,
            "cleanup mismatch");
        String advancement = PhaseASingletonAxolotlMonumentCertification.ADVANCEMENT;
        String criterion = PhaseASingletonAxolotlMonumentCertification.CRITERION;
        String family = PhaseASingletonAxolotlMonumentCertification.FAMILY;
        String source = PhaseASingletonAxolotlMonumentCertification.SOURCE;
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
