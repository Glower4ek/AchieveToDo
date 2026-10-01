package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhaseAFishingRodHookedExecutionEvidenceValidationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void acceptsOnlyACompleteNativeReelReceipt() throws Exception {
        PhaseAFishingRodHookedCertification.validate(ROOT);
        String runId = UUID.randomUUID().toString();
        String fingerprint = "sha-256:" + PhaseAFishingRodHookedCertification.catalogFingerprint(ROOT);
        assertDoesNotThrow(() -> PhaseAFishingRodHookedExecutionEvidenceValidation.validateReceipt(
            validReceipt(runId, fingerprint), runId, fingerprint));
    }

    @Test
    void rejectsEarlyCompletionWrongWitnessesAndCleanupDebt() throws Exception {
        String runId = UUID.randomUUID().toString();
        String fingerprint = "sha-256:" + PhaseAFishingRodHookedCertification.catalogFingerprint(ROOT);

        JsonObject earlyCriterion = validReceipt(runId, fingerprint);
        earlyCriterion.addProperty("criterionAfterCollisionBeforeReel", true);
        assertInvalid(earlyCriterion, runId, fingerprint);

        JsonObject wrongOwner = validReceipt(runId, fingerprint);
        wrongOwner.addProperty("hookOwnerUuid", UUID.randomUUID().toString());
        assertInvalid(wrongOwner, runId, fingerprint);

        JsonObject wrongTarget = validReceipt(runId, fingerprint);
        wrongTarget.addProperty("hookedTargetMatchesTag", false);
        assertInvalid(wrongTarget, runId, fingerprint);

        JsonObject cleanupDebt = validReceipt(runId, fingerprint);
        cleanupDebt.getAsJsonObject("cleanup").addProperty("warningCount", 1);
        assertInvalid(cleanupDebt, runId, fingerprint);
    }

    private static JsonObject validReceipt(String runId, String fingerprint) {
        UUID player = UUID.randomUUID();
        UUID hook = UUID.randomUUID();
        UUID target = UUID.randomUUID();
        JsonObject receipt = new JsonObject();
        receipt.addProperty("family", PhaseAFishingRodHookedCertification.FAMILY);
        receipt.addProperty("source", PhaseAFishingRodHookedCertification.SOURCE);
        receipt.addProperty("minecraftVersion", "26.2");
        receipt.addProperty("compatibilityMarker", PhaseAFishingRodHookedCertification.COMPATIBILITY_MARKER);
        receipt.addProperty("catalogFingerprint", fingerprint);
        receipt.addProperty("runId", runId);
        receipt.addProperty("advancementId", PhaseAFishingRodHookedCertification.ADVANCEMENT_ID);
        receipt.addProperty("criterion", PhaseAFishingRodHookedCertification.CRITERION);
        receipt.addProperty("requirementGroupIndex", 0);
        receipt.addProperty("trigger", PhaseAFishingRodHookedCertification.TRIGGER);
        receipt.addProperty("playerUuid", player.toString());
        receipt.addProperty("profileName", "fish1234567890");
        receipt.addProperty("joined", true);
        receipt.addProperty("connectionRegistered", true);
        receipt.addProperty("clientLoaded", true);
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("gameMode", "SURVIVAL");
        receipt.addProperty("finiteMaterials", true);
        receipt.addProperty("criterionBefore", false);
        receipt.addProperty("criterionAfter", true);
        receipt.addProperty("criterionBeforeReel", false);
        receipt.addProperty("criterionAfterCollisionBeforeReel", false);
        receipt.addProperty("nativeCastSucceeded", true);
        receipt.addProperty("nativeReelSucceeded", true);
        receipt.addProperty("noDirectCriterionTrigger", true);
        receipt.addProperty("noManualAward", true);
        receipt.addProperty("rodItem", PhaseAFishingRodHookedCertification.ROD_ITEM);
        receipt.addProperty("rodHand", "MAIN_HAND");
        receipt.addProperty("rodSlot", 0);
        receipt.addProperty("selectedSlot", 0);
        receipt.addProperty("rodDurabilityBefore", 0);
        receipt.addProperty("rodDurabilityAfter", 5);
        receipt.addProperty("fishingHookUuid", hook.toString());
        receipt.addProperty("hookOwnerUuid", player.toString());
        receipt.addProperty("hookedTargetType", PhaseAFishingRodHookedCertification.FIXTURE_ENTITY);
        receipt.addProperty("hookedTargetUuid", target.toString());
        receipt.addProperty("hookedTargetTag", PhaseAFishingRodHookedCertification.ENTITY_TAG);
        receipt.addProperty("hookedTargetMatchesTag", true);
        receipt.addProperty("hookStateAfterCast", "FLYING");
        receipt.addProperty("hookStateBeforeReel", "HOOKED_IN_ENTITY");
        receipt.addProperty("hookStateAfterReel", "HOOKED_IN_ENTITY");
        receipt.addProperty("hookRemovedAfterReel", true);
        receipt.addProperty("playerFishingClearedAfterReel", true);
        receipt.addProperty("actualActionBoundary", PhaseAFishingRodHookedCertification.RUNTIME_BOUNDARY);
        receipt.addProperty("actualActionResult", PhaseAFishingRodHookedExecutionEvidenceValidation.NATIVE_REEL_RESULT);
        receipt.addProperty("ticksToHook", 4);
        receipt.addProperty("ticksToCriterion", 4);
        receipt.add("productionUnlockWitness", productionWitness());
        receipt.addProperty("result", "GREEN");
        receipt.add("cleanup", cleanup());
        return receipt;
    }

    private static JsonObject productionWitness() {
        JsonObject production = new JsonObject();
        production.addProperty("gatePresent", true);
        production.addProperty("ability", "USE_FISHING_ROD");
        production.addProperty("scoreboardObjective", "bac_advancements");
        production.addProperty("thresholdSource", PhaseAFishingRodHookedExecutionEvidenceValidation.THRESHOLD_SOURCE);
        production.addProperty("requiredThreshold", 10);
        production.addProperty("scoreBefore", 0);
        production.addProperty("scoreAfter", 10);
        production.addProperty("scoreStable", true);
        production.addProperty("abilityLockedBefore", true);
        production.addProperty("abilityLockedAfter", false);
        production.addProperty("criterionAfterUnlock", false);
        return production;
    }

    private static JsonObject cleanup() {
        JsonObject cleanup = new JsonObject();
        cleanup.addProperty("playerRemoved", true);
        cleanup.addProperty("connectionRemoved", true);
        cleanup.addProperty("channelSettled", true);
        cleanup.addProperty("fixtureEntitiesRemoved", true);
        cleanup.addProperty("settlementMessages", 0);
        cleanup.addProperty("stacklessClosedChannelException", 0);
        cleanup.addProperty("failedPacketDeliveryFallback", 0);
        cleanup.addProperty("warningCount", 0);
        cleanup.addProperty("errors", "");
        return cleanup;
    }

    private static void assertInvalid(JsonObject receipt, String runId, String fingerprint) {
        assertThrows(IllegalStateException.class,
            () -> PhaseAFishingRodHookedExecutionEvidenceValidation.validateReceipt(receipt, runId, fingerprint));
    }
}
