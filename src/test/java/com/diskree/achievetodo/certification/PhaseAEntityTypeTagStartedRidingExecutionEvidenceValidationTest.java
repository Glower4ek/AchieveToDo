package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidationTest {
    @TempDir
    Path tempDir;

    @Test
    void diagnosticReceiptValidatesAndPromotableCoverageIsExact() throws IOException {
        JsonObject catalog = prepareCatalog();
        String fingerprint = PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.CATALOG_PATH)
        );
        JsonObject validReceipt = receiptJson(catalog, 1);
        validReceipt.addProperty("catalogFingerprint", fingerprint);
        writeArtifact(artifactJson(fingerprint, "run-1", validReceipt, false));

        PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir);
        assertEquals("run-1", artifact.runId());
        assertEquals(1, artifact.receipts().size());
        assertEquals("blazeandcave:biomes/boaty_mcboatface", artifact.receipts().getFirst().advancementId());
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void invalidBoundaryAndVehicleWitnessFailClosed() throws IOException {
        JsonObject catalog = prepareCatalog();
        String fingerprint = PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.CATALOG_PATH)
        );
        JsonObject receipt = receiptJson(catalog, 1);
        receipt.addProperty("catalogFingerprint", fingerprint);
        receipt.addProperty("boundary", "CriteriaTriggers.START_RIDING_TRIGGER.trigger");
        writeArtifact(artifactJson(fingerprint, "run-1", receipt, false));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        receipt = receiptJson(catalog, 1);
        receipt.addProperty("catalogFingerprint", fingerprint);
        receipt.addProperty("actualVehicleTypeAfter", "minecraft:minecart");
        writeArtifact(artifactJson(fingerprint, "run-1", receipt, false));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    private JsonObject prepareCatalog() throws IOException {
        Path frozen = tempDir.resolve(PhaseAEntityTypeTagStartedRidingCertification.CANONICAL_SEMANTICS_SOURCE);
        Files.createDirectories(frozen.getParent());
        Files.copy(
            Path.of("").toAbsolutePath().normalize().resolve(PhaseAEntityTypeTagStartedRidingCertification.CANONICAL_SEMANTICS_SOURCE),
            frozen
        );
        String catalogText = PhaseAEntityTypeTagStartedRidingCertification.generateSnapshot(tempDir);
        Path catalogPath = tempDir.resolve(PhaseAEntityTypeTagStartedRidingCertification.SNAPSHOT);
        Files.createDirectories(catalogPath.getParent());
        Files.writeString(catalogPath, catalogText, StandardCharsets.UTF_8);
        return JsonParser.parseString(catalogText).getAsJsonObject();
    }

    private void writeArtifact(JsonObject artifact) throws IOException {
        Path path = tempDir.resolve(PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.TEMPORARY_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, artifact.toString(), StandardCharsets.UTF_8);
    }

    private static JsonObject artifactJson(String fingerprint, String runId, JsonObject receipt, boolean allCases) {
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.SNAPSHOT);
        root.addProperty("schemaVersion", 1);
        root.addProperty("family", PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.FAMILY);
        root.addProperty("source", PhaseAEntityTypeTagStartedRidingExecutionEvidenceValidation.SOURCE);
        root.addProperty("minecraftVersion", "26.2");
        root.addProperty("compatibilityMarker", "compat_26_2_r15");
        root.addProperty("catalogFingerprint", fingerprint);
        root.addProperty("runId", runId);
        root.addProperty("generatedAt", "2026-09-07T00:00:00Z");
        JsonArray entries = new JsonArray();
        entries.add(receipt);
        root.add("entries", entries);
        return root;
    }

    private static JsonObject receiptJson(JsonObject catalog, int index) {
        JsonObject definition = catalog.getAsJsonArray("cases").get(index).getAsJsonObject();
        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", definition.get("advancementId").getAsString());
        receipt.addProperty("criterion", definition.get("criterion").getAsString());
        receipt.addProperty("requirementGroupIndex", 0);
        receipt.addProperty("trigger", "minecraft:started_riding");
        receipt.addProperty("entityTypeTag", definition.get("entityTypeTag").getAsString());
        receipt.addProperty("selectedEntityType", definition.get("selectedEntityType").getAsString());
        receipt.addProperty("runtimeEntityTypeTagMembership", true);
        receipt.addProperty("runtimeEntityTypeTagMemberCount", 3);
        receipt.addProperty("playerClass", "net.minecraft.server.level.ServerPlayer");
        receipt.addProperty("playerUuid", "00000000-0000-0000-0000-000000000001");
        receipt.addProperty("profileName", "ride000000000001");
        receipt.addProperty("joined", true);
        receipt.addProperty("connectionRegistered", true);
        receipt.addProperty("clientLoaded", true);
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("gameMode", "SURVIVAL");
        receipt.addProperty("buildPermission", true);
        receipt.addProperty("wasPassengerBefore", false);
        receipt.addProperty("isPassengerAfter", true);
        receipt.addProperty("actualVehicleTypeAfter", definition.get("directVehicleType").getAsString());
        receipt.addProperty("criterionBefore", false);
        receipt.addProperty("criterionAfter", true);
        receipt.addProperty("ticksToCriterion", 0);
        receipt.addProperty("boundary", "Entity.startRiding");
        receipt.addProperty("action", definition.get("action").getAsString());
        receipt.addProperty("legitimateTrigger", true);
        receipt.addProperty("interactionResult", "SUCCESS");
        receipt.addProperty("semanticMutation", true);
        receipt.addProperty("sourceArtifactClassification", "FROZEN_BACAP_DEFINITION");
        JsonArray chain = new JsonArray();
        for (var element : definition.getAsJsonArray("vehiclePredicateChain")) {
            String value = element.getAsString();
            chain.add(value.startsWith("#") ? definition.get("selectedEntityType").getAsString() : value);
        }
        receipt.add("vehicleChainAfter", chain);
        receipt.addProperty("requiredPassengerCount", 0);
        JsonObject preconditions = new JsonObject();
        preconditions.addProperty("scoreboardObjective", "bac_advancements");
        preconditions.addProperty("scoreBefore", 0);
        preconditions.addProperty("scoreAfter", 1000);
        preconditions.addProperty("boatAbilityLockedBefore", true);
        preconditions.addProperty("boatAbilityLockedAfter", false);
        preconditions.addProperty("minecartAbilityLockedBefore", true);
        preconditions.addProperty("minecartAbilityLockedAfter", false);
        preconditions.addProperty("lockedLandmark", false);
        receipt.add("productionPreconditions", preconditions);
        receipt.addProperty("family", "ENTITY_TYPE_TAG_STARTED_RIDING");
        receipt.addProperty("source", "PhaseAEntityTypeTagStartedRidingGameTest");
        receipt.addProperty("result", "GREEN");
        receipt.addProperty("catalogFingerprint", "pending");
        receipt.addProperty("runId", "run-1");
        receipt.addProperty("minecraftVersion", "26.2");
        receipt.addProperty("compatibilityMarker", "compat_26_2_r15");
        return receipt;
    }
}
