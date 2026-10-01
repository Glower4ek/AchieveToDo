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

/** Independent persistent validator for monsters/maximum_resistance. */
public final class PhaseASingletonMaximumResistanceExecutionEvidenceValidation {
    public static final Path PERSISTENT_ARTIFACT = PhaseASingletonMaximumResistanceCertification.PERSISTENT;
    private PhaseASingletonMaximumResistanceExecutionEvidenceValidation() { }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        try { PhaseASingletonMaximumResistanceCertification.validate(root); }
        catch (Exception e) { throw new IllegalStateException("Frozen maximum resistance catalog invalid", e); }
        JsonObject artifact = JsonParser.parseString(Files.readString(root.resolve(PERSISTENT_ARTIFACT), StandardCharsets.UTF_8)).getAsJsonObject();
        require("phase_a_singleton_maximum_resistance_execution_evidence".equals(string(artifact, "snapshot"))
            && integer(artifact, "schemaVersion") == 1, "snapshot/schema mismatch");
        require(PhaseASingletonMaximumResistanceCertification.FAMILY.equals(string(artifact, "family"))
            && PhaseASingletonMaximumResistanceCertification.SOURCE.equals(string(artifact, "source")), "artifact ownership mismatch");
        require("26.2".equals(string(artifact, "minecraftVersion"))
            && "compat_26_2_r15".equals(string(artifact, "compatibilityMarker")), "version mismatch");
        String hash = "sha-256:" + sha(Files.readAllBytes(root.resolve(PhaseASingletonMaximumResistanceCertification.SNAPSHOT)));
        require(hash.equals(string(artifact, "catalogFingerprint")), "catalog hash mismatch");
        String runId = string(artifact, "runId"); UUID.fromString(runId);
        JsonArray entries = artifact.getAsJsonArray("entries");
        require(entries != null && entries.size() == 1, "exact singleton receipt missing or extra");
        JsonObject r = entries.get(0).getAsJsonObject();
        require(runId.equals(string(r, "runId")) && hash.equals(string(r, "catalogFingerprint")), "receipt run/hash mismatch");
        require(PhaseASingletonMaximumResistanceCertification.FAMILY.equals(string(r, "family"))
            && PhaseASingletonMaximumResistanceCertification.SOURCE.equals(string(r, "source")), "receipt ownership mismatch");
        require(PhaseASingletonMaximumResistanceCertification.ADVANCEMENT.equals(string(r, "advancementId"))
            && PhaseASingletonMaximumResistanceCertification.CRITERION.equals(string(r, "criterion")), "criterion mismatch");
        require("minecraft:entity_hurt_player".equals(string(r, "trigger"))
            && PhaseASingletonMaximumResistanceCertification.BOUNDARY.equals(string(r, "boundary")), "native boundary mismatch");
        require(PhaseASingletonMaximumResistanceCertification.WARDEN.equals(string(r, "observedSourceEntity"))
            && PhaseASingletonMaximumResistanceCertification.ENCHANTMENT.equals(string(r, "observedEnchantment"))
            && PhaseASingletonMaximumResistanceCertification.EFFECT.equals(string(r, "observedEffect")), "source/enchantment/effect mismatch");
        require("GREEN".equals(string(r, "result")) && "SURVIVAL".equals(string(r, "gameMode")), "result/mode mismatch");
        require(!UUID.fromString(string(r, "playerUuid")).equals(UUID.fromString(string(r, "wardenUuid"))), "entity UUIDs not unique");
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "finiteMaterials", "armorVerified",
            "resistanceBeforeHit", "nativeWardenTargetAcquired", "nativeWardenDamage", "unblocked", "sourceWarden",
            "criterionAfter", "noDirectCriterionTrigger", "noManualAward"})
            require(bool(r, field), "missing native witness " + field);
        require(!bool(r, "criterionBefore") && integer(r, "armorPieces") == 4 && integer(r, "protectionLevelEach") == 4
            && integer(r, "resistanceAmplifier") >= 3 && integer(r, "ticksToHit") > 0 && integer(r, "ticksToHit") <= 240
            && number(r, "healthBefore") > number(r, "healthAfter"), "predicate or transition mismatch");
        require("NO_WARDEN_DAMAGE_GATE".equals(string(object(r, "productionGateWitness"), "productionGate")), "production gate mismatch");
        JsonObject cleanup = object(r, "cleanup");
        require(bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled")
            && bool(cleanup, "wardenRemoved") && integer(cleanup, "settlementMessages") >= 0
            && integer(cleanup, "warningCount") == 0, "cleanup mismatch");
        String advancement = PhaseASingletonMaximumResistanceCertification.ADVANCEMENT;
        String criterion = PhaseASingletonMaximumResistanceCertification.CRITERION;
        String family = PhaseASingletonMaximumResistanceCertification.FAMILY;
        String source = PhaseASingletonMaximumResistanceCertification.SOURCE;
        Map<String, RuntimeEvidenceData> result = new LinkedHashMap<>();
        result.put(advancement, new RuntimeEvidenceData(advancement, new TreeSet<>(Set.of(criterion)),
            Map.of(family, new TreeSet<>(Set.of(criterion))), Map.of(source, new TreeSet<>(Set.of(criterion))),
            new LinkedHashSet<>(Set.of(family)), new LinkedHashSet<>(Set.of(source))));
        return result;
    }

    private static String string(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonPrimitive() && !o.get(f).getAsString().isBlank(), "missing " + f); return o.get(f).getAsString(); }
    private static int integer(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonPrimitive() && o.get(f).getAsJsonPrimitive().isNumber(), "missing integer " + f); return o.get(f).getAsInt(); }
    private static double number(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonPrimitive() && o.get(f).getAsJsonPrimitive().isNumber(), "missing number " + f); return o.get(f).getAsDouble(); }
    private static boolean bool(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonPrimitive() && o.get(f).getAsJsonPrimitive().isBoolean(), "missing boolean " + f); return o.get(f).getAsBoolean(); }
    private static JsonObject object(JsonObject o, String f) { require(o.has(f) && o.get(f).isJsonObject(), "missing object " + f); return o.getAsJsonObject(f); }
    private static String sha(byte[] bytes) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static void require(boolean yes, String why) { if (!yes) throw new IllegalStateException(why); }
    public record RuntimeEvidenceData(String advancementId, Set<String> greenCriteria, Map<String, Set<String>> criteriaByFamily,
                                      Map<String, Set<String>> criteriaBySource, Set<String> families, Set<String> sources) { }
}
