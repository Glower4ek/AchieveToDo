package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidationTest {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    @Test
    void diagnosticArtifactAcceptsOneFullyValidatedReceipt(@TempDir Path root) throws IOException {
        List<PhaseAItemTagPlayerInteractedWithEntityCertification.CaseDefinition> cases = prepareRoot(root);
        String runId = UUID.randomUUID().toString();
        String fingerprint = PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.currentCatalogFingerprint(root);
        writeRunState(root, runId, fingerprint);
        writeArtifact(root, PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            runId, fingerprint, List.of(validReceipt(cases.getFirst(), runId, fingerprint)));

        PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(root);
        assertEquals(1, artifact.entries().size());
        assertEquals(
            PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.Mode.TEMP_DIAGNOSTIC,
            artifact.mode()
        );
    }

    @Test
    void promotableArtifactRequiresExactCurrentCaseSet(@TempDir Path root) throws IOException {
        List<PhaseAItemTagPlayerInteractedWithEntityCertification.CaseDefinition> cases = prepareRoot(root);
        String runId = UUID.randomUUID().toString();
        String fingerprint = PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.currentCatalogFingerprint(root);
        writeRunState(root, runId, fingerprint);
        writeArtifact(root, PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            runId, fingerprint, List.of(validReceipt(cases.getFirst(), runId, fingerprint)));

        assertThrows(
            IllegalStateException.class,
            () -> PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(root)
        );
    }

    @Test
    void validatorRejectsWrongTriggerPrecompletedCriterionAndFalseTagWitness(@TempDir Path root) throws IOException {
        List<PhaseAItemTagPlayerInteractedWithEntityCertification.CaseDefinition> cases = prepareRoot(root);
        String runId = UUID.randomUUID().toString();
        String fingerprint = PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.currentCatalogFingerprint(root);
        writeRunState(root, runId, fingerprint);
        JsonObject wrongTrigger = validReceipt(cases.getFirst(), runId, fingerprint);
        wrongTrigger.addProperty("trigger", "minecraft:player_killed_entity");
        writeArtifact(root, PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            runId, fingerprint, List.of(wrongTrigger));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(root));

        JsonObject precompleted = validReceipt(cases.getFirst(), runId, fingerprint);
        precompleted.addProperty("criterionBefore", true);
        writeArtifact(root, PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            runId, fingerprint, List.of(precompleted));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(root));

        JsonObject falseMembership = validReceipt(cases.getFirst(), runId, fingerprint);
        falseMembership.addProperty("runtimeItemTagMembership", false);
        writeArtifact(root, PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            runId, fingerprint, List.of(falseMembership));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(root));

        JsonObject infiniteMaterials = validReceipt(cases.getFirst(), runId, fingerprint);
        infiniteMaterials.addProperty("finiteMaterials", false);
        writeArtifact(root, PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            runId, fingerprint, List.of(infiniteMaterials));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(root));
    }

    @Test
    void persistentArtifactValidationIsIndependentOfNewerRunState(@TempDir Path root) throws IOException {
        List<PhaseAItemTagPlayerInteractedWithEntityCertification.CaseDefinition> cases = prepareRoot(root);
        String runId = UUID.randomUUID().toString();
        String fingerprint = PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.currentCatalogFingerprint(root);
        List<JsonObject> receipts = cases.stream()
            .map(definition -> validReceipt(definition, runId, fingerprint))
            .toList();
        writeArtifact(root, PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.PERSISTENT_ARTIFACT,
            runId, fingerprint, receipts);
        writeRunState(root, UUID.randomUUID().toString(), fingerprint);

        PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.loadValidatedPersistentArtifact(root);
        assertEquals(4, artifact.entries().size());
        assertEquals(
            PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.Mode.PERSISTENT,
            artifact.mode()
        );
    }

    private static List<PhaseAItemTagPlayerInteractedWithEntityCertification.CaseDefinition> prepareRoot(Path root) throws IOException {
        Path frozen = root.resolve(PhaseAItemTagPlayerInteractedWithEntityCertification.CANONICAL_SEMANTICS_SOURCE);
        Files.createDirectories(frozen.getParent());
        Files.copy(
            Path.of("").toAbsolutePath().normalize().resolve(PhaseAItemTagPlayerInteractedWithEntityCertification.CANONICAL_SEMANTICS_SOURCE),
            frozen,
            StandardCopyOption.REPLACE_EXISTING
        );
        Path catalog = root.resolve(PhaseAItemTagPlayerInteractedWithEntityCertification.SNAPSHOT);
        Files.createDirectories(catalog.getParent());
        Files.writeString(catalog, PhaseAItemTagPlayerInteractedWithEntityCertification.generateSnapshot(root), StandardCharsets.UTF_8);
        return PhaseAItemTagPlayerInteractedWithEntityCertification.deriveCases(root);
    }

    private static void writeRunState(Path root, String runId, String fingerprint) throws IOException {
        JsonObject state = new JsonObject();
        state.addProperty("snapshot", PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.SNAPSHOT);
        state.addProperty("schemaVersion", 1);
        state.addProperty("family", PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.FAMILY);
        state.addProperty("source", PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.SOURCE);
        state.addProperty("minecraftVersion", "26.2");
        state.addProperty("compatibilityMarker", "compat_26_2_r15");
        state.addProperty("catalogFingerprint", fingerprint);
        state.addProperty("runId", runId);
        state.addProperty("startedAt", "2026-09-07T00:00:00Z");
        writeJson(root.resolve(PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.RUN_STATE_ARTIFACT), state);
    }

    private static void writeArtifact(Path root, Path relativePath, String runId, String fingerprint, List<JsonObject> receipts)
        throws IOException {
        JsonObject artifact = new JsonObject();
        artifact.addProperty("snapshot", PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.SNAPSHOT);
        artifact.addProperty("schemaVersion", 1);
        artifact.addProperty("family", PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.FAMILY);
        artifact.addProperty("source", PhaseAItemTagPlayerInteractedWithEntityExecutionEvidenceValidation.SOURCE);
        artifact.addProperty("minecraftVersion", "26.2");
        artifact.addProperty("compatibilityMarker", "compat_26_2_r15");
        artifact.addProperty("catalogFingerprint", fingerprint);
        artifact.addProperty("runId", runId);
        artifact.addProperty("generatedAt", "2026-09-07T00:00:00Z");
        JsonArray entries = new JsonArray();
        receipts.forEach(entries::add);
        artifact.add("entries", entries);
        writeJson(root.resolve(relativePath), artifact);
    }

    private static JsonObject validReceipt(
        PhaseAItemTagPlayerInteractedWithEntityCertification.CaseDefinition definition,
        String runId,
        String fingerprint
    ) {
        UUID targetUuid = UUID.randomUUID();
        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", definition.advancementId());
        receipt.addProperty("criterion", definition.criterion());
        receipt.addProperty("requirementGroupIndex", 0);
        receipt.addProperty("trigger", "minecraft:player_interacted_with_entity");
        receipt.addProperty("itemTag", definition.itemTag());
        receipt.addProperty("selectedItem", definition.selectedItem());
        receipt.addProperty("runtimeItemTagMembership", true);
        receipt.addProperty("runtimeItemTagMemberCount", 1);
        JsonArray members = new JsonArray();
        members.add(definition.selectedItem());
        receipt.add("runtimeItemTagMembers", members);
        receipt.addProperty("selectedEntityType", definition.selectedEntityType());
        receipt.addProperty("observedEntityType", definition.selectedEntityType());
        receipt.addProperty("targetEntityId", 42);
        receipt.addProperty("targetEntityUuid", targetUuid.toString());
        receipt.addProperty("targetAliveBefore", true);
        receipt.addProperty("targetAliveAfter", true);
        receipt.addProperty("entityPredicateSatisfied", true);
        receipt.addProperty("boundary", "ServerGamePacketListenerImpl.handleInteract");
        receipt.addProperty("packetPath", "ServerboundInteractPacket.handle->ServerGamePacketListenerImpl.handleInteract->CriteriaTriggers.PLAYER_INTERACTED_WITH_ENTITY");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", "net.minecraft.server.level.ServerPlayer");
        receipt.addProperty("playerUuid", UUID.randomUUID().toString());
        receipt.addProperty("profileName", "ient123456789012");
        receipt.addProperty("gameMode", "SURVIVAL");
        receipt.addProperty("finiteMaterials", true);
        receipt.addProperty("joined", true);
        receipt.addProperty("connectionRegistered", true);
        receipt.addProperty("clientLoaded", true);
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("criterionBefore", false);
        receipt.addProperty("criterionAfter", true);
        receipt.addProperty("interactionPerformed", true);
        receipt.addProperty("legitimateTrigger", true);
        receipt.addProperty("interactionResult", "SUCCESS");
        receipt.addProperty("interactionConsumesAction", true);
        receipt.addProperty("ticksToCriterion", 0);
        receipt.add("productionPreconditions", validPreconditions(definition));
        receipt.add("interactionStack", validStack(definition));
        receipt.add("entityStateBefore", entityState(definition, targetUuid, false));
        receipt.add("entityStateAfter", entityState(definition, targetUuid, true));
        JsonObject predicate = new JsonObject();
        predicate.addProperty("requiredEntityType", definition.selectedEntityType());
        predicate.addProperty("observedEntityType", definition.selectedEntityType());
        predicate.addProperty("satisfied", true);
        receipt.add("entityPredicateObservation", predicate);
        JsonObject proof = new JsonObject();
        proof.addProperty("action", definition.action());
        proof.addProperty("realPacketPath", true);
        proof.addProperty("noDirectCriterionTrigger", true);
        proof.addProperty("noManualAward", true);
        proof.addProperty("itemTagWitness", true);
        proof.addProperty("targetIdentityWitness", true);
        proof.addProperty("criterionBefore", false);
        proof.addProperty("criterionAfter", true);
        receipt.add("actionProof", proof);
        receipt.addProperty("family", "ITEM_TAG_PLAYER_INTERACTED_WITH_ENTITY");
        receipt.addProperty("source", "PhaseAItemTagPlayerInteractedWithEntityGameTest");
        receipt.addProperty("catalogFingerprint", fingerprint);
        receipt.addProperty("runId", runId);
        receipt.addProperty("minecraftVersion", "26.2");
        receipt.addProperty("compatibilityMarker", "compat_26_2_r15");
        receipt.addProperty("result", "GREEN");
        JsonObject cleanup = new JsonObject();
        cleanup.addProperty("playerRemoved", true);
        cleanup.addProperty("connectionRemoved", true);
        cleanup.addProperty("channelSettled", true);
        cleanup.addProperty("settlementMessages", 0);
        cleanup.addProperty("warningCount", 0);
        receipt.add("cleanup", cleanup);
        return receipt;
    }

    private static JsonObject validPreconditions(PhaseAItemTagPlayerInteractedWithEntityCertification.CaseDefinition definition) {
        JsonObject value = new JsonObject();
        value.addProperty("scoreboardObjective", "bac_advancements");
        boolean unlock = "USE_FLINT_AND_STEEL_UNLOCKED".equals(definition.productionPrecondition());
        value.addProperty("scoreBefore", 0);
        value.addProperty("scoreAfter", unlock ? 1000 : 0);
        value.addProperty("abilityLockedBefore", unlock);
        value.addProperty("abilityLockedAfter", false);
        value.addProperty("lockedLandmark", false);
        value.addProperty("ability", definition.productionPrecondition());
        return value;
    }

    private static JsonObject validStack(PhaseAItemTagPlayerInteractedWithEntityCertification.CaseDefinition definition) {
        boolean creeper = "IGNITE_CREEPER".equals(definition.action());
        JsonObject value = new JsonObject();
        value.addProperty("itemBefore", definition.selectedItem());
        value.addProperty("itemAfter", creeper ? definition.selectedItem() : "minecraft:air");
        value.addProperty("countBefore", 1);
        value.addProperty("countAfter", creeper ? 1 : 0);
        value.addProperty("damageBefore", 0);
        value.addProperty("damageAfter", creeper ? 1 : 0);
        value.addProperty("selectedHandAfter", creeper ? definition.selectedItem() : "minecraft:air");
        value.addProperty("selectedHandCountAfter", creeper ? 1 : 0);
        value.addProperty("snapshotBefore", "ItemStack[before]");
        value.addProperty("snapshotAfter", "ItemStack[after]");
        value.addProperty("sameStackReference", true);
        return value;
    }

    private static JsonObject entityState(
        PhaseAItemTagPlayerInteractedWithEntityCertification.CaseDefinition definition,
        UUID targetUuid,
        boolean after
    ) {
        JsonObject value = new JsonObject();
        value.addProperty("type", definition.selectedEntityType());
        value.addProperty("entityId", 42);
        value.addProperty("uuid", targetUuid.toString());
        value.addProperty("alive", true);
        value.addProperty("health", 20.0);
        if ("FEED_DOLPHIN".equals(definition.action())) {
            value.addProperty("gotFish", after);
        } else if ("IGNITE_CREEPER".equals(definition.action())) {
            value.addProperty("ignited", after);
        } else if ("FEED_BABY_SNIFFER".equals(definition.action())) {
            value.addProperty("isBaby", true);
        }
        return value;
    }

    private static void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
    }
}
