package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    @TempDir
    Path tempDir;

    @Test
    void catalogSnapshotMatchesFrozenDerivedOutput() throws IOException {
        String generated = PhaseAPlayerHurtEntityDamageSourceCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAPlayerHurtEntityDamageSourceCertification.SNAPSHOT);
        assertTrue(Files.isRegularFile(snapshot));
        assertEquals(generated, Files.readString(snapshot, StandardCharsets.UTF_8));
    }

    @Test
    void validExactArtifactIsAccepted() throws IOException {
        prepareRoot(tempDir);
        String runId = "11111111-1111-1111-1111-111111111111";
        writeRunState(tempDir, runId);
        writeArtifact(tempDir, PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            runId, List.of(validReceipt(0), validReceipt(1), validReceipt(2)));

        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation
                .loadValidatedPromotableTemporaryArtifact(tempDir);
        assertEquals(runId, artifact.runId());
        assertEquals(3, artifact.entries().size());
    }

    @Test
    void directTriggerClaimMutationIsRejected() throws IOException {
        prepareRoot(tempDir);
        String runId = "22222222-2222-2222-2222-222222222222";
        writeRunState(tempDir, runId);
        JsonObject receipt = validReceipt(0);
        receipt.getAsJsonObject("packetProof").addProperty("noDirectCriterionTrigger", false);
        writeArtifact(tempDir, PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            runId, List.of(receipt, validReceipt(1), validReceipt(2)));

        assertThrows(IllegalStateException.class, () ->
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation
                .loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void damageSourceMutationIsRejected() throws IOException {
        prepareRoot(tempDir);
        String runId = "33333333-3333-3333-3333-333333333333";
        writeRunState(tempDir, runId);
        JsonObject receipt = validReceipt(1);
        receipt.getAsJsonObject("damageSource").addProperty("type", "minecraft:generic");
        writeArtifact(tempDir, PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            runId, List.of(validReceipt(0), receipt, validReceipt(2)));

        assertThrows(IllegalStateException.class, () ->
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation
                .loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void wrongRequirementBoundaryIsRejected() throws IOException {
        prepareRoot(tempDir);
        String runId = "44444444-4444-4444-4444-444444444444";
        writeRunState(tempDir, runId);
        JsonObject receipt = validReceipt(2);
        receipt.addProperty("packetPath", "direct-listener-invocation");
        writeArtifact(tempDir, PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            runId, List.of(validReceipt(0), validReceipt(1), receipt));

        assertThrows(IllegalStateException.class, () ->
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation
                .loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void persistentValidationIgnoresNewerActiveRunState() throws IOException {
        prepareRoot(tempDir);
        String artifactRunId = "55555555-5555-5555-5555-555555555555";
        writeRunState(tempDir, "66666666-6666-6666-6666-666666666666");
        writeArtifact(tempDir, PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.PERSISTENT_ARTIFACT,
            artifactRunId, List.of(validReceipt(0), validReceipt(1), validReceipt(2)));

        PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.loadValidatedPersistentArtifact(tempDir);
        assertEquals(artifactRunId, artifact.runId());
        assertEquals(3, artifact.entries().size());
    }

    private void prepareRoot(Path root) throws IOException {
        Path zipTarget = root.resolve(PhaseAPlayerHurtEntityDamageSourceCertification.CANONICAL_SEMANTICS_SOURCE);
        Files.createDirectories(zipTarget.getParent());
        Files.copy(
            PROJECT_ROOT.resolve(PhaseAPlayerHurtEntityDamageSourceCertification.CANONICAL_SEMANTICS_SOURCE),
            zipTarget,
            StandardCopyOption.REPLACE_EXISTING
        );
        Path catalog = root.resolve(PhaseAPlayerHurtEntityDamageSourceCertification.SNAPSHOT);
        Files.createDirectories(catalog.getParent());
        Files.writeString(catalog,
            PhaseAPlayerHurtEntityDamageSourceCertification.generateSnapshot(root), StandardCharsets.UTF_8);
    }

    private void writeRunState(Path root, String runId) throws IOException {
        JsonObject state = new JsonObject();
        state.addProperty("snapshot", PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SNAPSHOT);
        state.addProperty("schemaVersion", 1);
        state.addProperty("family", PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.FAMILY);
        state.addProperty("source", PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SOURCE);
        state.addProperty("minecraftVersion", "26.2");
        state.addProperty("compatibilityMarker", "compat_26_2_r15");
        state.addProperty("catalogFingerprint",
            PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.currentCatalogFingerprint(root));
        state.addProperty("runId", runId);
        state.addProperty("startedAt", "2026-09-12T00:00:00Z");
        writeJson(root.resolve(PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.RUN_STATE_ARTIFACT), state);
    }

    private void writeArtifact(Path root, Path relativePath, String runId, List<JsonObject> entries) throws IOException {
        String fingerprint = PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.currentCatalogFingerprint(root);
        JsonObject artifact = new JsonObject();
        artifact.addProperty("snapshot", PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SNAPSHOT);
        artifact.addProperty("schemaVersion", 1);
        artifact.addProperty("family", PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.FAMILY);
        artifact.addProperty("source", PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SOURCE);
        artifact.addProperty("minecraftVersion", "26.2");
        artifact.addProperty("compatibilityMarker", "compat_26_2_r15");
        artifact.addProperty("catalogFingerprint", fingerprint);
        artifact.addProperty("runId", runId);
        artifact.addProperty("generatedAt", "2026-09-12T00:01:00Z");
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            entry.addProperty("family", PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.FAMILY);
            entry.addProperty("source", PhaseAPlayerHurtEntityDamageSourceExecutionEvidenceValidation.SOURCE);
            entry.addProperty("catalogFingerprint", fingerprint);
            entry.addProperty("runId", runId);
            entry.addProperty("minecraftVersion", "26.2");
            entry.addProperty("compatibilityMarker", "compat_26_2_r15");
            entry.addProperty("result", "GREEN");
            array.add(entry);
        }
        artifact.add("entries", array);
        writeJson(root.resolve(relativePath), artifact);
    }

    private void writeJson(Path path, JsonObject json) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(json) + "\n", StandardCharsets.UTF_8);
    }

    private static JsonObject validReceipt(int index) {
        boolean wind = index == 0;
        boolean fish = index == 1;
        String advancementId = wind
            ? "blazeandcave:adventure/from_under_your_feet"
            : fish ? "blazeandcave:weaponry/slapfish" : "blazeandcave:weaponry/viking";
        String criterion = wind ? "from_under_your_feet" : fish ? "slapfish" : "axe";
        String item = wind ? "minecraft:wind_charge" : fish ? "minecraft:cod" : "minecraft:wooden_axe";
        String damageType = wind ? "minecraft:wind_charge" : "minecraft:player_attack";
        String boundary = wind ? PhaseAPlayerHurtEntityDamageSourceCertification.WIND_BOUNDARY
            : PhaseAPlayerHurtEntityDamageSourceCertification.ATTACK_BOUNDARY;
        String packetPath = wind ? PhaseAPlayerHurtEntityDamageSourceCertification.WIND_PACKET_PATH
            : PhaseAPlayerHurtEntityDamageSourceCertification.ATTACK_PACKET_PATH;
        String playerUuid = String.format("00000000-0000-0000-0000-%012d", index + 1);
        String targetUuid = String.format("10000000-0000-0000-0000-%012d", index + 1);

        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", advancementId);
        receipt.addProperty("criterion", criterion);
        receipt.addProperty("requirementGroupIndex", 0);
        receipt.addProperty("trigger", "minecraft:player_hurt_entity");
        receipt.addProperty("boundary", boundary);
        receipt.addProperty("packetPath", packetPath);
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", "net.minecraft.server.level.ServerPlayer");
        receipt.addProperty("playerUuid", playerUuid);
        receipt.addProperty("profileName", "hurt" + (index + 1));
        receipt.addProperty("gameMode", "SURVIVAL");
        receipt.addProperty("finiteMaterials", true);
        receipt.addProperty("abilitiesInstabuild", false);
        receipt.addProperty("abilitiesMayBuild", true);
        receipt.addProperty("abilitiesInvulnerable", false);
        receipt.addProperty("spectator", false);
        receipt.addProperty("joined", true);
        receipt.addProperty("connectionRegistered", true);
        receipt.addProperty("clientLoaded", true);
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("criterionBefore", false);
        receipt.addProperty("criterionAfter", true);
        receipt.addProperty("legitimateTrigger", true);
        receipt.addProperty("nativeTriggerObserved", true);
        receipt.addProperty("ticksToCriterion", 1);

        JsonObject production = new JsonObject();
        production.addProperty("scoreboardObjective", "bac_advancements");
        production.addProperty("itemRewardsScoreboard", "bac_settings");
        production.addProperty("itemRewardsScore", 0);
        production.addProperty("itemRewardsDisabled", true);
        production.addProperty("defaultAdvancementsMode", true);
        production.addProperty("scoreBefore", 0);
        String ability = wind ? "THROW_WIND_CHARGE" : fish ? "NONE" : "USE_WOODEN_TOOLS";
        int threshold = wind ? 319 : fish ? 0 : 71;
        production.addProperty("ability", ability);
        production.addProperty("abilityUnlockThreshold", threshold);
        production.addProperty("scoreAfter", threshold);
        production.addProperty("abilityLockedBefore", threshold > 0);
        production.addProperty("abilityLockedAfter", false);
        production.addProperty("lockedLandmark", false);
        receipt.add("productionPreconditions", production);

        JsonObject weapon = new JsonObject();
        weapon.addProperty("itemBefore", item);
        weapon.addProperty("itemAfter", wind ? "minecraft:air" : item);
        weapon.addProperty("hand", "MAIN_HAND");
        weapon.addProperty("slot", 0);
        weapon.addProperty("countBefore", 1);
        weapon.addProperty("countAfter", wind ? 0 : 1);
        weapon.addProperty("componentSnapshotRecorded", true);
        weapon.addProperty("sameStackReference", true);
        receipt.add("weapon", weapon);

        JsonObject target = new JsonObject();
        target.addProperty("entityType", "minecraft:zombie");
        target.addProperty("targetWitnessType", "minecraft:zombie");
        target.addProperty("entityUuid", targetUuid);
        target.addProperty("entityId", 100 + index);
        target.addProperty("aliveBefore", true);
        target.addProperty("aliveAfter", true);
        target.addProperty("fixtureDamageBeforeAction", false);
        target.addProperty("fixtureCriterionBeforeAction", false);
        target.addProperty("healthBefore", 20.0);
        target.addProperty("healthAfter", 19.0);
        target.addProperty("actualDamage", 1.0);
        target.addProperty("onGround", true);
        target.addProperty("distanceWithinPredicate", !wind);
        target.addProperty("distanceToPlayer", fish || !wind ? 3.0 : 0.0);
        target.addProperty("steppingBlockTag", wind ? "minecraft:trapdoors" : "none");
        target.addProperty("steppingOnPredicateSatisfied", wind);
        receipt.add("target", target);

        JsonObject damage = new JsonObject();
        damage.addProperty("type", damageType);
        damage.addProperty("typeHolder", damageType);
        damage.addProperty("observedFromTargetLastDamageSource", true);
        JsonArray typeTags = new JsonArray();
        if (wind) {
            typeTags.add("minecraft:is_projectile");
        }
        damage.add("typeTags", typeTags);
        damage.addProperty("isProjectile", wind);
        damage.addProperty("directEntityPresent", true);
        damage.addProperty("directEntityType", wind ? "minecraft:wind_charge" : "minecraft:player");
        damage.addProperty("directEntityUuid", wind
            ? "20000000-0000-0000-0000-000000000001" : playerUuid);
        JsonObject source = new JsonObject();
        source.addProperty("type", "minecraft:player");
        source.addProperty("uuid", playerUuid);
        source.addProperty("isAttacker", true);
        damage.add("sourceEntity", source);
        JsonObject observedTarget = new JsonObject();
        observedTarget.addProperty("type", "minecraft:zombie");
        observedTarget.addProperty("uuid", targetUuid);
        damage.add("targetEntity", observedTarget);
        damage.addProperty("weaponItem", wind ? "minecraft:none" : item);
        damage.addProperty("actualDamage", 1.0);
        receipt.add("damageSource", damage);

        JsonObject action = new JsonObject();
        action.addProperty("packetAccepted", true);
        action.addProperty("legitimateGameplayAction", true);
        action.addProperty("targetHealthChanged", true);
        action.addProperty("packetClass", wind ? "ServerboundUseItemPacket" : "ServerboundAttackPacket");
        action.addProperty("serverBoundary", boundary);
        receipt.add("actionResult", action);

        JsonObject proof = new JsonObject();
        proof.addProperty("realPacketPath", true);
        proof.addProperty("noDirectCriterionTrigger", true);
        proof.addProperty("noManualAward", true);
        proof.addProperty("noFakeDamageSource", true);
        proof.addProperty("noManualListenerInvocation", true);
        proof.addProperty("liveAdvancementProgress", true);
        proof.addProperty("nativeCriterionPath", "CriteriaTriggers.PLAYER_HURT_ENTITY");
        receipt.add("packetProof", proof);

        JsonObject cleanup = new JsonObject();
        cleanup.addProperty("playerRemoved", true);
        cleanup.addProperty("connectionRemoved", true);
        cleanup.addProperty("channelSettled", true);
        cleanup.addProperty("settlementMessages", 0);
        cleanup.addProperty("warningCount", 0);
        receipt.add("cleanup", cleanup);
        return receipt;
    }
}
