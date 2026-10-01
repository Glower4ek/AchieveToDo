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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Independent fail-closed reader for FISHING_ROD_HOOKED runtime evidence. */
public final class PhaseAFishingRodHookedExecutionEvidenceValidation {
    public static final Path TEMPORARY_ARTIFACT = Path.of("build", "tmp", "phase_a_certification", "fishing_rod_hooked_execution_evidence.json");
    public static final Path RUN_STATE_ARTIFACT = Path.of("build", "tmp", "phase_a_certification", "fishing_rod_hooked_execution_evidence.run.json");
    public static final Path DIAGNOSTIC_ARTIFACT = Path.of("build", "tmp", "phase_a_certification", "fishing_rod_hooked_canary_execution_evidence.json");
    public static final Path DIAGNOSTIC_RUN_STATE = Path.of("build", "tmp", "phase_a_certification", "fishing_rod_hooked_canary_execution_evidence.run.json");
    public static final String NATIVE_BOUNDARY = PhaseAFishingRodHookedCertification.RUNTIME_BOUNDARY;
    public static final String NATIVE_REEL_RESULT = "criterion transitioned false-to-true; native hook removed; rod durability increased by five";
    public static final String THRESHOLD_SOURCE = "LevelInfoExtension.achievetodo$getAbilitiesConfiguration(overworldSeed)";

    private PhaseAFishingRodHookedExecutionEvidenceValidation() { }

    public static Path temporaryPath(String runMode) {
        return "DIAGNOSTIC".equals(runMode) ? DIAGNOSTIC_ARTIFACT : TEMPORARY_ARTIFACT;
    }

    public static Path runStatePath(String runMode) {
        return "DIAGNOSTIC".equals(runMode) ? DIAGNOSTIC_RUN_STATE : RUN_STATE_ARTIFACT;
    }

    public static String catalogFingerprint(Path root) throws Exception {
        PhaseAFishingRodHookedCertification.validate(root);
        return "sha-256:" + sha256(Files.readAllBytes(root.resolve(PhaseAFishingRodHookedCertification.SNAPSHOT)));
    }

    public static Artifact loadTemporary(Path root, String expectedRunMode) throws Exception {
        require("DIAGNOSTIC".equals(expectedRunMode) || "PROMOTABLE".equals(expectedRunMode), "Invalid expected run mode");
        String fingerprint = catalogFingerprint(root);
        JsonObject runState = read(root.resolve(runStatePath(expectedRunMode)));
        validateCommon(runState, fingerprint, expectedRunMode);
        require(integer(runState, "beginCount") == 1, "Run-state beginRun count mismatch");
        require(integer(runState, "coordinatorCount") == 1, "Run-state coordinator count mismatch");
        JsonObject artifactJson = read(root.resolve(temporaryPath(expectedRunMode)));
        Artifact artifact = parse(artifactJson, fingerprint, expectedRunMode);
        require(artifact.runId().equals(string(runState, "runId")), "TEMP/run-state runId mismatch");
        require(integer(runState, "receiptCount") == artifact.entries().size(), "Run-state receipt count mismatch");
        validateArtifact(artifact, fingerprint);
        return artifact;
    }

    /** Validates persistent evidence from its own contents; no transient RUN_STATE is consulted. */
    public static Artifact loadPersistentArtifact(Path root) throws Exception {
        String fingerprint = catalogFingerprint(root);
        Artifact artifact = parse(read(root.resolve(PhaseAFishingRodHookedCertification.PERSISTENT_EVIDENCE)), fingerprint, "PROMOTABLE");
        validateArtifact(artifact, fingerprint);
        return artifact;
    }

    public static Map<String, RuntimeEvidenceData> loadValidatedRuntimeEvidence(Path root) throws IOException {
        Artifact artifact;
        try {
            artifact = loadPersistentArtifact(root);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid persistent FISHING_ROD_HOOKED evidence", e);
        }
        JsonObject receipt = artifact.entries().getFirst();
        String advancementId = string(receipt, "advancementId");
        String criterion = string(receipt, "criterion");
        RuntimeEvidenceData evidence = new RuntimeEvidenceData(
            advancementId,
            new TreeSet<>(Set.of(criterion)),
            new LinkedHashMap<>(Map.of(PhaseAFishingRodHookedCertification.FAMILY, new TreeSet<>(Set.of(criterion)))),
            new LinkedHashMap<>(Map.of(PhaseAFishingRodHookedCertification.SOURCE, new TreeSet<>(Set.of(criterion)))),
            new LinkedHashSet<>(Set.of(PhaseAFishingRodHookedCertification.FAMILY)),
            new LinkedHashSet<>(Set.of(PhaseAFishingRodHookedCertification.SOURCE))
        );
        return Map.of(advancementId, evidence);
    }

    public static void validateReceipt(JsonObject receipt, String runId, String fingerprint) {
        require(UUID.fromString(string(receipt, "runId")).toString().equals(runId), "receipt runId mismatch");
        require(fingerprint.equals(string(receipt, "catalogFingerprint")), "receipt catalog fingerprint mismatch");
        require(PhaseAFishingRodHookedCertification.FAMILY.equals(string(receipt, "family")), "receipt family mismatch");
        require(PhaseAFishingRodHookedCertification.SOURCE.equals(string(receipt, "source")), "receipt source mismatch");
        require("26.2".equals(string(receipt, "minecraftVersion")), "receipt Minecraft version mismatch");
        require(PhaseAFishingRodHookedCertification.COMPATIBILITY_MARKER.equals(string(receipt, "compatibilityMarker")), "receipt compatibility marker mismatch");
        require(PhaseAFishingRodHookedCertification.ADVANCEMENT_ID.equals(string(receipt, "advancementId")), "receipt advancement mismatch");
        require(PhaseAFishingRodHookedCertification.CRITERION.equals(string(receipt, "criterion")), "receipt criterion mismatch");
        require(integer(receipt, "requirementGroupIndex") == 0, "receipt requirement group mismatch");
        require(PhaseAFishingRodHookedCertification.TRIGGER.equals(string(receipt, "trigger")), "receipt trigger mismatch");
        require("GREEN".equals(string(receipt, "result")), "receipt is not GREEN");

        UUID playerUuid = UUID.fromString(string(receipt, "playerUuid"));
        UUID hookUuid = UUID.fromString(string(receipt, "fishingHookUuid"));
        UUID ownerUuid = UUID.fromString(string(receipt, "hookOwnerUuid"));
        UUID targetUuid = UUID.fromString(string(receipt, "hookedTargetUuid"));
        require(playerUuid.equals(ownerUuid), "fishing hook owner differs from joined player");
        require(!playerUuid.equals(hookUuid) && !playerUuid.equals(targetUuid) && !hookUuid.equals(targetUuid), "runtime UUID witnesses are not distinct");
        String profileName = string(receipt, "profileName");
        require(profileName.length() <= 16, "joined profile name exceeds 16 characters");
        require(bool(receipt, "joined") && bool(receipt, "connectionRegistered") && bool(receipt, "clientLoaded"), "joined-player lifecycle is incomplete");
        require(bool(receipt, "normalScheduler"), "runtime did not use the normal server scheduler");
        require("SURVIVAL".equals(string(receipt, "gameMode")) && bool(receipt, "finiteMaterials"), "player was not finite-materials SURVIVAL");
        require(!bool(receipt, "criterionBefore") && bool(receipt, "criterionAfter"), "criterion did not transition false-to-true");
        require(!bool(receipt, "criterionBeforeReel") && !bool(receipt, "criterionAfterCollisionBeforeReel"), "criterion fired before the native reel action");
        require(bool(receipt, "nativeCastSucceeded") && bool(receipt, "nativeReelSucceeded"), "legitimate rod action did not complete");
        require(bool(receipt, "noDirectCriterionTrigger") && bool(receipt, "noManualAward"), "receipt allows direct/manual criterion completion");

        require(PhaseAFishingRodHookedCertification.ROD_ITEM.equals(string(receipt, "rodItem")), "wrong rod item");
        require("MAIN_HAND".equals(string(receipt, "rodHand")), "wrong rod hand");
        require(integer(receipt, "rodSlot") == integer(receipt, "selectedSlot"), "rod slot differs from selected slot");
        require(integer(receipt, "rodDurabilityBefore") == 0, "rod was not pristine before the native cast");
        require(integer(receipt, "rodDurabilityAfter") - integer(receipt, "rodDurabilityBefore") == 5, "native entity retrieve did not produce the expected five-point rod damage");
        require(PhaseAFishingRodHookedCertification.FIXTURE_ENTITY.equals(string(receipt, "hookedTargetType")), "wrong hooked target type");
        require(PhaseAFishingRodHookedCertification.ENTITY_TAG.equals(string(receipt, "hookedTargetTag")), "wrong target tag witness");
        require(bool(receipt, "hookedTargetMatchesTag"), "hooked target did not match the frozen entity tag");
        require("FLYING".equals(string(receipt, "hookStateAfterCast")), "native cast did not create a FLYING hook");
        require("HOOKED_IN_ENTITY".equals(string(receipt, "hookStateBeforeReel")), "hook was not natively attached to an entity before reeling");
        require("HOOKED_IN_ENTITY".equals(string(receipt, "hookStateAfterReel")) && bool(receipt, "hookRemovedAfterReel"), "hook did not complete native entity retrieval");
        require(bool(receipt, "playerFishingClearedAfterReel"), "player retained the fishing hook after native retrieval");
        require(NATIVE_BOUNDARY.equals(string(receipt, "actualActionBoundary")), "native runtime boundary mismatch");
        require(NATIVE_REEL_RESULT.equals(string(receipt, "actualActionResult")), "observed reel result mismatch");
        require(integer(receipt, "ticksToHook") >= 1 && integer(receipt, "ticksToHook") <= 20, "hook collision exceeded its 20-tick bound");
        require(integer(receipt, "ticksToCriterion") >= integer(receipt, "ticksToHook") && integer(receipt, "ticksToCriterion") <= 21, "criterion timing exceeded its bounded native sequence");

        JsonObject production = object(receipt, "productionUnlockWitness");
        require(bool(production, "gatePresent"), "production fishing-rod gate was omitted");
        require("USE_FISHING_ROD".equals(string(production, "ability")), "wrong production ability");
        require("bac_advancements".equals(string(production, "scoreboardObjective")), "wrong production scoreboard");
        require(THRESHOLD_SOURCE.equals(string(production, "thresholdSource")), "wrong live threshold source");
        int threshold = integer(production, "requiredThreshold");
        require(threshold > 0 && integer(production, "scoreBefore") == 0 && integer(production, "scoreAfter") == threshold, "invalid real scoreboard threshold witness");
        require(bool(production, "scoreStable") && bool(production, "abilityLockedBefore") && !bool(production, "abilityLockedAfter"), "production ability was not unlocked at a stable live threshold");
        require(!bool(production, "criterionAfterUnlock"), "criterion was already complete after gate setup");

        JsonObject cleanup = object(receipt, "cleanup");
        require(bool(cleanup, "playerRemoved") && bool(cleanup, "connectionRemoved") && bool(cleanup, "channelSettled") && bool(cleanup, "fixtureEntitiesRemoved"), "runtime fixture cleanup incomplete");
        require(integer(cleanup, "settlementMessages") >= 0 && integer(cleanup, "settlementMessages") <= 4096, "channel settlement exceeded message bound");
        require(integer(cleanup, "stacklessClosedChannelException") == 0, "cleanup logged StacklessClosedChannelException");
        require(integer(cleanup, "failedPacketDeliveryFallback") == 0, "cleanup logged failed packet delivery fallback");
        require(integer(cleanup, "warningCount") == 0, "cleanup warning debt is nonzero");
        require(cleanup.has("errors") && cleanup.get("errors").isJsonPrimitive()
            && cleanup.get("errors").getAsString().isEmpty(), "cleanup reported an error");
    }

    private static Artifact parse(JsonObject json, String fingerprint, String runMode) {
        validateCommon(json, fingerprint, runMode);
        string(json, "generatedAt");
        JsonArray entries = array(json, "entries");
        List<JsonObject> receipts = new ArrayList<>();
        for (JsonElement element : entries) {
            require(element.isJsonObject(), "Non-object runtime receipt");
            receipts.add(element.getAsJsonObject());
        }
        return new Artifact(string(json, "runId"), runMode, List.copyOf(receipts));
    }

    private static void validateArtifact(Artifact artifact, String fingerprint) {
        Set<String> expected = Set.of(PhaseAFishingRodHookedCertification.ADVANCEMENT_ID + "#" + PhaseAFishingRodHookedCertification.CRITERION);
        Set<String> actual = new LinkedHashSet<>();
        Set<String> playerIds = new LinkedHashSet<>();
        for (JsonObject receipt : artifact.entries()) {
            String key = string(receipt, "advancementId") + "#" + string(receipt, "criterion");
            require(actual.add(key), "Duplicate fishing-rod receipt " + key);
            validateReceipt(receipt, artifact.runId(), fingerprint);
            playerIds.add(string(receipt, "playerUuid"));
        }
        require(artifact.entries().size() == 1 && actual.equals(expected), "Exact fishing-rod receipt key set mismatch");
        require(playerIds.size() == 1, "Expected one unique joined-player identity");
    }

    private static void validateCommon(JsonObject json, String fingerprint, String expectedRunMode) {
        require("phase_a_fishing_rod_hooked_execution_evidence".equals(string(json, "snapshot")), "evidence snapshot mismatch");
        require(integer(json, "schemaVersion") == 1, "evidence schema mismatch");
        require(PhaseAFishingRodHookedCertification.FAMILY.equals(string(json, "family")), "evidence family mismatch");
        require(PhaseAFishingRodHookedCertification.SOURCE.equals(string(json, "source")), "evidence source mismatch");
        require("26.2".equals(string(json, "minecraftVersion")), "evidence Minecraft version mismatch");
        require(PhaseAFishingRodHookedCertification.COMPATIBILITY_MARKER.equals(string(json, "compatibilityMarker")), "evidence compatibility marker mismatch");
        require(fingerprint.equals(string(json, "catalogFingerprint")), "evidence catalog fingerprint mismatch");
        require(expectedRunMode.equals(string(json, "runMode")), "evidence run mode mismatch");
        UUID.fromString(string(json, "runId"));
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static JsonArray array(JsonObject json, String field) {
        require(json.has(field) && json.get(field).isJsonArray(), "Missing array " + field);
        return json.getAsJsonArray(field);
    }

    private static JsonObject object(JsonObject json, String field) {
        require(json.has(field) && json.get(field).isJsonObject(), "Missing object " + field);
        return json.getAsJsonObject(field);
    }

    private static String string(JsonObject json, String field) {
        require(json != null && json.has(field) && json.get(field).isJsonPrimitive() && !json.get(field).getAsString().isBlank(), "Missing " + field);
        return json.get(field).getAsString();
    }

    private static int integer(JsonObject json, String field) {
        require(json != null && json.has(field) && json.get(field).isJsonPrimitive() && json.get(field).getAsJsonPrimitive().isNumber(), "Missing integer " + field);
        return json.get(field).getAsInt();
    }

    private static boolean bool(JsonObject json, String field) {
        require(json != null && json.has(field) && json.get(field).isJsonPrimitive() && json.get(field).getAsJsonPrimitive().isBoolean(), "Missing boolean " + field);
        return json.get(field).getAsBoolean();
    }

    private static String sha256(byte[] bytes) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public record Artifact(String runId, String runMode, List<JsonObject> entries) { }

    public record RuntimeEvidenceData(
        String advancementId,
        Set<String> greenCriteria,
        Map<String, Set<String>> criteriaByFamily,
        Map<String, Set<String>> criteriaBySource,
        Set<String> families,
        Set<String> sources
    ) { }
}
