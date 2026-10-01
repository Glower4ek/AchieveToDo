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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAShotCrossbowExecutionEvidenceValidationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    @TempDir
    Path tempDir;

    @Test
    void catalogSnapshotMatchesFrozenDerivedOutput() throws IOException {
        String generated = PhaseAShotCrossbowCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAShotCrossbowCertification.SNAPSHOT);
        assertTrue(Files.isRegularFile(snapshot));
        assertEquals(generated, Files.readString(snapshot, StandardCharsets.UTF_8));
    }

    @Test
    void catalogHasExactTwoCasesAndNativeCrossbowBoundary() throws IOException {
        JsonObject root = com.google.gson.JsonParser.parseString(
            PhaseAShotCrossbowCertification.generateSnapshot(PROJECT_ROOT)
        ).getAsJsonObject();
        assertEquals(1152, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals("SHOT_CROSSBOW", root.get("family").getAsString());
        assertEquals("ServerGamePacketListenerImpl.handleUseItem", root.get("runtimeBoundary").getAsString());
        assertEquals(
            "ServerboundUseItemPacket.handle->ServerGamePacketListenerImpl.handleUseItem"
                + "->CrossbowItem.performShooting->CriteriaTriggers.SHOT_CROSSBOW",
            root.get("runtimePacketPath").getAsString()
        );
        JsonObject summary = root.getAsJsonObject("summary");
        assertEquals(2, summary.get("totalCases").getAsInt());
        assertEquals(2, summary.get("requirementGroups").getAsInt());
        assertEquals(2, summary.get("automationSupported").getAsInt());
        JsonArray cases = root.getAsJsonArray("cases");
        assertEquals("minecraft:quick_charge", cases.get(0).getAsJsonObject()
            .getAsJsonObject("enchantmentPredicate").get("enchantment").getAsString());
        assertEquals(1, cases.get(0).getAsJsonObject().get("expectedLoadedProjectileCount").getAsInt());
        assertEquals("minecraft:multishot", cases.get(1).getAsJsonObject()
            .getAsJsonObject("enchantmentPredicate").get("enchantment").getAsString());
        assertEquals(3, cases.get(1).getAsJsonObject().get("expectedFiredProjectileCount").getAsInt());
    }

    @Test
    void validExactArtifactIsAccepted() throws IOException {
        prepareRoot(tempDir);
        String runId = "11111111-1111-1111-1111-111111111111";
        writeRunState(tempDir, runId);
        writeArtifact(tempDir, PhaseAShotCrossbowExecutionEvidenceValidation.TEMPORARY_ARTIFACT, runId,
            List.of(validReceipt(0), validReceipt(1)));

        PhaseAShotCrossbowExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAShotCrossbowExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir);
        assertEquals(runId, artifact.runId());
        assertEquals(2, artifact.entries().size());
    }

    @Test
    void projectileCardinalityMutationIsRejected() throws IOException {
        prepareRoot(tempDir);
        String runId = "22222222-2222-2222-2222-222222222222";
        writeRunState(tempDir, runId);
        JsonObject receipt = validReceipt(0);
        receipt.getAsJsonObject("projectileObservation").addProperty("spawnedCount", 2);
        writeArtifact(tempDir, PhaseAShotCrossbowExecutionEvidenceValidation.TEMPORARY_ARTIFACT, runId,
            List.of(receipt, validReceipt(1)));

        assertThrows(IllegalStateException.class,
            () -> PhaseAShotCrossbowExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void packetPathMutationIsRejected() throws IOException {
        prepareRoot(tempDir);
        String runId = "33333333-3333-3333-3333-333333333333";
        writeRunState(tempDir, runId);
        JsonObject receipt = validReceipt(0);
        receipt.addProperty("releasePacketAction", "DIRECT_RELEASE_USING_ITEM");
        writeArtifact(tempDir, PhaseAShotCrossbowExecutionEvidenceValidation.TEMPORARY_ARTIFACT, runId,
            List.of(receipt, validReceipt(1)));

        assertThrows(IllegalStateException.class,
            () -> PhaseAShotCrossbowExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void directTriggerClaimMutationIsRejected() throws IOException {
        prepareRoot(tempDir);
        String runId = "44444444-4444-4444-4444-444444444444";
        writeRunState(tempDir, runId);
        JsonObject receipt = validReceipt(0);
        receipt.getAsJsonObject("packetProof").addProperty("noDirectCriterionTrigger", false);
        writeArtifact(tempDir, PhaseAShotCrossbowExecutionEvidenceValidation.TEMPORARY_ARTIFACT, runId,
            List.of(receipt, validReceipt(1)));

        assertThrows(IllegalStateException.class,
            () -> PhaseAShotCrossbowExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void persistentValidationIgnoresNewerActiveRunState() throws IOException {
        prepareRoot(tempDir);
        String artifactRunId = "55555555-5555-5555-5555-555555555555";
        writeRunState(tempDir, "66666666-6666-6666-6666-666666666666");
        writeArtifact(tempDir, PhaseAShotCrossbowExecutionEvidenceValidation.PERSISTENT_ARTIFACT, artifactRunId,
            List.of(validReceipt(0), validReceipt(1)));

        PhaseAShotCrossbowExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAShotCrossbowExecutionEvidenceValidation.loadValidatedPersistentArtifact(tempDir);
        assertEquals(artifactRunId, artifact.runId());
        assertEquals(2, artifact.entries().size());
    }

    private void prepareRoot(Path root) throws IOException {
        Path zipTarget = root.resolve(PhaseAShotCrossbowCertification.CANONICAL_SEMANTICS_SOURCE);
        Files.createDirectories(zipTarget.getParent());
        Files.copy(
            PROJECT_ROOT.resolve(PhaseAShotCrossbowCertification.CANONICAL_SEMANTICS_SOURCE),
            zipTarget,
            StandardCopyOption.REPLACE_EXISTING
        );
        Path catalog = root.resolve(PhaseAShotCrossbowCertification.SNAPSHOT);
        Files.createDirectories(catalog.getParent());
        Files.writeString(catalog, PhaseAShotCrossbowCertification.generateSnapshot(root), StandardCharsets.UTF_8);
    }

    private void writeRunState(Path root, String runId) throws IOException {
        JsonObject state = new JsonObject();
        state.addProperty("snapshot", PhaseAShotCrossbowExecutionEvidenceValidation.SNAPSHOT);
        state.addProperty("schemaVersion", 1);
        state.addProperty("family", PhaseAShotCrossbowExecutionEvidenceValidation.FAMILY);
        state.addProperty("source", PhaseAShotCrossbowExecutionEvidenceValidation.SOURCE);
        state.addProperty("minecraftVersion", "26.2");
        state.addProperty("compatibilityMarker", "compat_26_2_r15");
        state.addProperty("catalogFingerprint", PhaseAShotCrossbowExecutionEvidenceValidation.currentCatalogFingerprint(root));
        state.addProperty("runId", runId);
        state.addProperty("startedAt", "2026-09-12T00:00:00Z");
        writeJson(root.resolve(PhaseAShotCrossbowExecutionEvidenceValidation.RUN_STATE_ARTIFACT), state);
    }

    private void writeArtifact(Path root, Path relativePath, String runId, List<JsonObject> entries) throws IOException {
        JsonObject artifact = new JsonObject();
        artifact.addProperty("snapshot", PhaseAShotCrossbowExecutionEvidenceValidation.SNAPSHOT);
        artifact.addProperty("schemaVersion", 1);
        artifact.addProperty("family", PhaseAShotCrossbowExecutionEvidenceValidation.FAMILY);
        artifact.addProperty("source", PhaseAShotCrossbowExecutionEvidenceValidation.SOURCE);
        artifact.addProperty("minecraftVersion", "26.2");
        artifact.addProperty("compatibilityMarker", "compat_26_2_r15");
        artifact.addProperty("catalogFingerprint", PhaseAShotCrossbowExecutionEvidenceValidation.currentCatalogFingerprint(root));
        artifact.addProperty("runId", runId);
        artifact.addProperty("generatedAt", "2026-09-12T00:01:00Z");
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            entry.addProperty("family", PhaseAShotCrossbowExecutionEvidenceValidation.FAMILY);
            entry.addProperty("source", PhaseAShotCrossbowExecutionEvidenceValidation.SOURCE);
            entry.addProperty("catalogFingerprint", PhaseAShotCrossbowExecutionEvidenceValidation.currentCatalogFingerprint(root));
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
        boolean multishot = index == 1;
        String advancementId = multishot
            ? "blazeandcave:enchanting/shotbow"
            : "blazeandcave:enchanting/machine_bow";
        String enchantment = multishot ? "minecraft:multishot" : "minecraft:quick_charge";
        int count = multishot ? 3 : 1;
        int playerId = index + 1;
        String playerUuid = String.format("00000000-0000-0000-0000-%012d", playerId);

        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", advancementId);
        receipt.addProperty("criterion", "shot_crossbow");
        receipt.addProperty("requirementGroupIndex", 0);
        receipt.addProperty("trigger", "minecraft:shot_crossbow");
        receipt.addProperty("selectedItem", "minecraft:crossbow");
        receipt.addProperty("enchantment", enchantment);
        receipt.addProperty("levelRule", multishot ? "present" : "min");
        receipt.addProperty("configuredEnchantmentLevel", multishot ? 1 : 3);
        receipt.addProperty("actualEnchantmentLevel", multishot ? 1 : 3);
        receipt.addProperty("itemPredicateSatisfied", true);
        receipt.addProperty("boundary", "ServerGamePacketListenerImpl.handleUseItem");
        receipt.addProperty(
            "packetPath",
            "ServerboundUseItemPacket.handle->ServerGamePacketListenerImpl.handleUseItem"
                + "->CrossbowItem.performShooting->CriteriaTriggers.SHOT_CROSSBOW"
        );
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", "net.minecraft.server.level.ServerPlayer");
        receipt.addProperty("playerUuid", playerUuid);
        receipt.addProperty("profileName", "shot" + playerId);
        receipt.addProperty("gameMode", "SURVIVAL");
        receipt.addProperty("finiteMaterials", true);
        receipt.addProperty("abilitiesInstabuild", false);
        receipt.addProperty("spectator", false);
        receipt.addProperty("joined", true);
        receipt.addProperty("connectionRegistered", true);
        receipt.addProperty("clientLoaded", true);
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("usePacketAccepted", true);
        receipt.addProperty("releasePacketAccepted", true);
        receipt.addProperty("releasePacketAction", "RELEASE_USE_ITEM");
        receipt.addProperty("usingAfterRelease", false);
        receipt.addProperty("chargedBeforeRelease", true);
        receipt.addProperty("chargedBeforeShoot", true);
        receipt.addProperty("chargedAfterShoot", false);
        receipt.addProperty("criterionBefore", false);
        receipt.addProperty("criterionAfter", true);
        receipt.addProperty("legitimateTrigger", true);
        receipt.addProperty("noDirectCriterionTrigger", true);
        receipt.addProperty("noManualAward", true);
        receipt.addProperty("nativeTriggerObserved", true);
        receipt.addProperty("ticksToCriterion", 0);

        JsonObject preconditions = new JsonObject();
        preconditions.addProperty("scoreboardObjective", "bac_advancements");
        preconditions.addProperty("itemRewardsScoreboard", "bac_settings");
        preconditions.addProperty("itemRewardsScore", 0);
        preconditions.addProperty("itemRewardsDisabled", true);
        preconditions.addProperty("ability", "SHOOT_CROSSBOW");
        preconditions.addProperty("abilityUnlockThreshold", 269);
        preconditions.addProperty("defaultAdvancementsMode", true);
        preconditions.addProperty("scoreBefore", 0);
        preconditions.addProperty("scoreAfter", 269);
        preconditions.addProperty("abilityLockedBefore", true);
        preconditions.addProperty("abilityLockedAfter", false);
        preconditions.addProperty("lockedLandmark", false);
        receipt.add("productionPreconditions", preconditions);

        JsonObject loaded = new JsonObject();
        loaded.addProperty("count", count);
        loaded.addProperty("expectedCount", count);
        loaded.addProperty("itemType", "minecraft:arrow");
        loaded.addProperty("chargedComponentNonEmpty", true);
        receipt.add("loadedProjectiles", loaded);
        receipt.addProperty("loadedProjectileWitness", true);

        JsonObject ammo = new JsonObject();
        ammo.addProperty("item", "minecraft:arrow");
        ammo.addProperty("countBefore", 1);
        ammo.addProperty("countAfter", 0);
        ammo.addProperty("consumed", 1);
        ammo.addProperty("additionalCopiesNotConsumed", count - 1);
        ammo.addProperty("finiteSurvivalConsumption", true);
        receipt.add("ammoState", ammo);

        JsonObject crossbow = new JsonObject();
        crossbow.addProperty("itemBefore", "minecraft:crossbow");
        crossbow.addProperty("itemAfter", "minecraft:crossbow");
        crossbow.addProperty("damageBefore", 0);
        crossbow.addProperty("damageAfter", count);
        crossbow.addProperty("durabilityUse", count);
        crossbow.addProperty("chargedBeforeRelease", true);
        crossbow.addProperty("chargedAfterShoot", false);
        crossbow.addProperty("sameStackReference", true);
        receipt.add("crossbowState", crossbow);

        JsonObject projectiles = new JsonObject();
        projectiles.addProperty("expectedCount", count);
        projectiles.addProperty("spawnedCount", count);
        projectiles.addProperty("firedProjectileCount", count);
        projectiles.addProperty("allAreArrowProjectiles", true);
        projectiles.addProperty("allHavePlayerOwner", true);
        projectiles.addProperty("ownerIdentityMatches", true);
        projectiles.addProperty("nativeSpawnObserved", true);
        JsonArray ids = new JsonArray();
        JsonArray uuids = new JsonArray();
        JsonArray types = new JsonArray();
        JsonArray owners = new JsonArray();
        for (int i = 0; i < count; i++) {
            ids.add(100 + index * 10 + i);
            uuids.add(String.format("10000000-0000-0000-0000-%012d", index * 10 + i + 1));
            types.add("minecraft:arrow");
            owners.add(playerUuid);
        }
        projectiles.add("entityIds", ids);
        projectiles.add("entityUuids", uuids);
        projectiles.add("entityTypes", types);
        projectiles.add("owners", owners);
        receipt.add("projectileObservation", projectiles);

        JsonObject proof = new JsonObject();
        proof.addProperty("usePacket", "ServerboundUseItemPacket");
        proof.addProperty("releasePacket", "ServerboundPlayerActionPacket(RELEASE_USE_ITEM)");
        proof.addProperty("serverHandler", "ServerGamePacketListenerImpl.handleUseItem");
        proof.addProperty("nativeItemPath", "CrossbowItem.use->CrossbowItem.performShooting");
        proof.addProperty("nativeCriterionPath", "CriteriaTriggers.SHOT_CROSSBOW");
        proof.addProperty("realPacketPath", true);
        proof.addProperty("noDirectCriterionTrigger", true);
        proof.addProperty("noManualAward", true);
        proof.addProperty("liveAdvancementProgress", true);
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
