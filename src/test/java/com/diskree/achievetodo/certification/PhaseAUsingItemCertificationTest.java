package com.diskree.achievetodo.certification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

class PhaseAUsingItemCertificationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

    @TempDir
    Path tempDir;

    @Test
    void catalogSnapshotMatchesFrozenDerivedOutput() throws IOException {
        String generated = PhaseAUsingItemCertification.generateSnapshot(PROJECT_ROOT);
        Path snapshot = PROJECT_ROOT.resolve(PhaseAUsingItemCertification.SNAPSHOT);
        assertTrue(Files.isRegularFile(snapshot));
        assertEquals(generated, Files.readString(snapshot, StandardCharsets.UTF_8));
    }

    @Test
    void catalogHasExactCasesAndCurrentNativeBoundaries() throws IOException {
        JsonObject root = JsonParser.parseString(
            PhaseAUsingItemCertification.generateSnapshot(PROJECT_ROOT)
        ).getAsJsonObject();
        assertEquals(1152, root.get("canonicalAdvancementCount").getAsInt());
        assertEquals("USING_ITEM", root.get("family").getAsString());
        assertEquals("ServerGamePacketListenerImpl.handleUseItem", root.get("runtimeBoundary").getAsString());
        assertEquals(2, root.getAsJsonObject("summary").get("totalCases").getAsInt());
        JsonArray cases = root.getAsJsonArray("cases");
        assertEquals(2, cases.size());
        assertEquals(PhaseAUsingItemCertification.EXPECTED_KEYS.get(0), caseKey(cases.get(0).getAsJsonObject()));
        assertEquals(PhaseAUsingItemCertification.EXPECTED_KEYS.get(1), caseKey(cases.get(1).getAsJsonObject()));
        assertEquals("minecraft:goat_horn", cases.get(0).getAsJsonObject().get("selectedItem").getAsString());
        assertEquals("minecraft:riptide", cases.get(1).getAsJsonObject()
            .getAsJsonArray("requiredEnchantments").get(0).getAsJsonObject().get("id").getAsString());
        assertEquals(244, cases.get(1).getAsJsonObject()
            .getAsJsonObject("productionUnlockGate").get("unlockThreshold").getAsInt());
    }

    @Test
    void validExactArtifactIsAccepted() throws IOException {
        prepareRoot(tempDir);
        String runId = "11111111-1111-1111-1111-111111111111";
        writeRunState(tempDir, runId);
        writeArtifact(tempDir, PhaseAUsingItemExecutionEvidenceValidation.TEMPORARY_ARTIFACT, runId,
            List.of(validReceipt(0), validReceipt(1)));
        PhaseAUsingItemExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAUsingItemExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir);
        assertEquals(runId, artifact.runId());
        assertEquals(2, artifact.entries().size());
    }

    @Test
    void invalidNativeStateIsRejected() throws IOException {
        prepareRoot(tempDir);
        String runId = "22222222-2222-2222-2222-222222222222";
        writeRunState(tempDir, runId);
        JsonObject receipt = validReceipt(0);
        receipt.addProperty("usingItemAfterOrObserved", false);
        writeArtifact(tempDir, PhaseAUsingItemExecutionEvidenceValidation.TEMPORARY_ARTIFACT, runId,
            List.of(receipt, validReceipt(1)));
        assertThrows(IllegalStateException.class,
            () -> PhaseAUsingItemExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void persistentValidationIgnoresNewerActiveRunState() throws IOException {
        prepareRoot(tempDir);
        String artifactRunId = "33333333-3333-3333-3333-333333333333";
        writeRunState(tempDir, "44444444-4444-4444-4444-444444444444");
        writeArtifact(tempDir, PhaseAUsingItemExecutionEvidenceValidation.PERSISTENT_ARTIFACT, artifactRunId,
            List.of(validReceipt(0), validReceipt(1)));
        PhaseAUsingItemExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAUsingItemExecutionEvidenceValidation.loadValidatedPersistentArtifact(tempDir);
        assertEquals(artifactRunId, artifact.runId());
        assertEquals(2, artifact.entries().size());
    }

    private static String caseKey(JsonObject json) {
        return json.get("advancementId").getAsString() + "#" + json.get("criterion").getAsString();
    }

    private void prepareRoot(Path root) throws IOException {
        Path zipTarget = root.resolve(PhaseAUsingItemCertification.CANONICAL_SEMANTICS_SOURCE);
        Files.createDirectories(zipTarget.getParent());
        Files.copy(
            PROJECT_ROOT.resolve(PhaseAUsingItemCertification.CANONICAL_SEMANTICS_SOURCE),
            zipTarget,
            StandardCopyOption.REPLACE_EXISTING
        );
        Path catalog = root.resolve(PhaseAUsingItemCertification.SNAPSHOT);
        Files.createDirectories(catalog.getParent());
        Files.writeString(catalog, PhaseAUsingItemCertification.generateSnapshot(root), StandardCharsets.UTF_8);
    }

    private void writeRunState(Path root, String runId) throws IOException {
        JsonObject state = new JsonObject();
        state.addProperty("snapshot", PhaseAUsingItemExecutionEvidenceValidation.SNAPSHOT);
        state.addProperty("schemaVersion", 1);
        state.addProperty("family", PhaseAUsingItemExecutionEvidenceValidation.FAMILY);
        state.addProperty("source", PhaseAUsingItemExecutionEvidenceValidation.SOURCE);
        state.addProperty("minecraftVersion", "26.2");
        state.addProperty("compatibilityMarker", "compat_26_2_r15");
        state.addProperty("catalogFingerprint", PhaseAUsingItemExecutionEvidenceValidation.currentCatalogFingerprint(root));
        state.addProperty("runId", runId);
        state.addProperty("startedAt", "2026-09-12T00:00:00Z");
        writeJson(root.resolve(PhaseAUsingItemExecutionEvidenceValidation.RUN_STATE_ARTIFACT), state);
    }

    private void writeArtifact(Path root, Path relativePath, String runId, List<JsonObject> entries) throws IOException {
        JsonObject artifact = new JsonObject();
        artifact.addProperty("snapshot", PhaseAUsingItemExecutionEvidenceValidation.SNAPSHOT);
        artifact.addProperty("schemaVersion", 1);
        artifact.addProperty("family", PhaseAUsingItemExecutionEvidenceValidation.FAMILY);
        artifact.addProperty("source", PhaseAUsingItemExecutionEvidenceValidation.SOURCE);
        artifact.addProperty("minecraftVersion", "26.2");
        artifact.addProperty("compatibilityMarker", "compat_26_2_r15");
        artifact.addProperty("catalogFingerprint", PhaseAUsingItemExecutionEvidenceValidation.currentCatalogFingerprint(root));
        artifact.addProperty("runId", runId);
        artifact.addProperty("generatedAt", "2026-09-12T00:01:00Z");
        JsonArray array = new JsonArray();
        for (JsonObject entry : entries) {
            entry.addProperty("family", PhaseAUsingItemExecutionEvidenceValidation.FAMILY);
            entry.addProperty("source", PhaseAUsingItemExecutionEvidenceValidation.SOURCE);
            entry.addProperty("catalogFingerprint", PhaseAUsingItemExecutionEvidenceValidation.currentCatalogFingerprint(root));
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
        boolean riptide = index == 1;
        String advancementId = riptide
            ? "blazeandcave:enchanting/do_a_barrel_roll"
            : "blazeandcave:animal/loud_and_proud";
        String criterion = riptide ? "riptide" : "goat_horn";
        String item = riptide ? "minecraft:trident" : "minecraft:goat_horn";
        String action = riptide ? "RIPTIDE_TRIDENT_USE" : "GOAT_HORN_USE";
        String packetPath = PhaseAUsingItemCertification.PACKET_PATH.replace(
            "Player.startUsingItem", (riptide ? "TridentItem.use" : "InstrumentItem.use") + "->Player.startUsingItem"
        );
        String playerUuid = String.format("00000000-0000-0000-0000-%012d", index + 1);
        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", advancementId);
        receipt.addProperty("criterion", criterion);
        receipt.addProperty("requirementGroupIndex", 0);
        receipt.addProperty("trigger", "minecraft:using_item");
        receipt.addProperty("playerClass", "net.minecraft.server.level.ServerPlayer");
        receipt.addProperty("playerUuid", playerUuid);
        receipt.addProperty("profileName", "use" + (index + 1));
        receipt.addProperty("joined", true);
        receipt.addProperty("connectionRegistered", true);
        receipt.addProperty("clientLoaded", true);
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("gameMode", "SURVIVAL");
        receipt.addProperty("finiteMaterials", true);
        receipt.addProperty("abilitiesInstabuild", false);
        receipt.addProperty("spectator", false);
        receipt.addProperty("selectedItem", item);
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("slot", 0);
        receipt.addProperty("itemCountBefore", 1);
        receipt.addProperty("itemCountAfter", 1);
        receipt.addProperty("componentsBefore", "live components");
        receipt.addProperty("componentsAfter", "live components");
        receipt.addProperty("sameStackReference", true);
        receipt.addProperty("itemPredicateSatisfied", true);

        JsonObject enchantment = new JsonObject();
        enchantment.addProperty("required", riptide);
        enchantment.addProperty("id", riptide ? "minecraft:riptide" : "minecraft:none");
        enchantment.addProperty("level", riptide ? 1 : 0);
        enchantment.addProperty("componentPresent", riptide);
        enchantment.addProperty("beforeMatches", true);
        enchantment.addProperty("afterMatches", true);
        receipt.add("enchantmentWitness", enchantment);

        JsonObject production = new JsonObject();
        production.addProperty("scoreboardObjective", "bac_advancements");
        production.addProperty("itemRewardsScoreboard", "bac_settings");
        production.addProperty("itemRewardsScore", 0);
        production.addProperty("itemRewardsDisabled", true);
        production.addProperty("defaultAdvancementsMode", true);
        production.addProperty("gatePresent", riptide);
        production.addProperty("ability", riptide ? "ATTACK_WITH_TRIDENT" : "NONE");
        production.addProperty("unlockThreshold", riptide ? 244 : 0);
        production.addProperty("scoreBefore", 0);
        production.addProperty("scoreAfter", riptide ? 244 : 0);
        production.addProperty("scoreStable", true);
        production.addProperty("abilityLockedBefore", riptide);
        production.addProperty("abilityLockedAfter", false);
        production.addProperty("lockedLandmark", false);
        receipt.add("productionUnlockWitness", production);

        JsonObject environment = new JsonObject();
        environment.addProperty("required", true);
        environment.addProperty("predicateSatisfied", true);
        if (riptide) {
            environment.addProperty("waterOrRainRequired", true);
            environment.addProperty("wetBeforeUse", true);
            environment.addProperty("wetAtUse", true);
            environment.addProperty("wetStateObserved", true);
            environment.addProperty("wetStateSource", "Player.isInWaterOrRain");
        } else {
            environment.addProperty("biomeId", "minecraft:deep_dark");
            environment.addProperty("biomePredicateSatisfied", true);
            environment.addProperty("structureAlternativeSatisfied", false);
            environment.addProperty("predicate", "minecraft:deep_dark OR minecraft:ancient_city");
        }
        receipt.add("environmentWitness", environment);

        receipt.addProperty("boundary", "ServerGamePacketListenerImpl.handleUseItem");
        receipt.addProperty("packetPath", packetPath);
        receipt.addProperty("criterionBefore", false);
        receipt.addProperty("criterionAfter", true);
        receipt.addProperty("fixtureDidNotTrigger", true);
        receipt.addProperty("legitimateTrigger", true);
        receipt.addProperty("usingItemBefore", false);
        receipt.addProperty("usingItemAfterOrObserved", true);
        receipt.addProperty("actualUseItem", item);
        receipt.addProperty("actualUseTicks", 1);
        receipt.addProperty("actualUseDuration", riptide ? 72000 : 140);
        receipt.addProperty("actualUseAnimation", riptide ? "TRIDENT" : "TOOT_HORN");
        receipt.addProperty("actualActionBoundary", "ServerGamePacketListenerImpl.handleUseItem");
        receipt.addProperty("actualPacketPath", packetPath);
        receipt.addProperty("actualActionResult", "NATIVE_ACTIVE_USE_ACCEPTED");
        receipt.addProperty("usePacketAccepted", true);
        receipt.addProperty("nativeUseStateObserved", true);
        receipt.addProperty("releasePacketAccepted", true);
        receipt.addProperty("releasePacketAction", "RELEASE_USE_ITEM");
        receipt.addProperty("usingAfterRelease", false);
        receipt.addProperty("useStoppedAfterRelease", true);
        receipt.addProperty("noDirectTrigger", true);
        receipt.addProperty("noManualAward", true);
        receipt.addProperty("nativeTriggerObserved", true);
        receipt.addProperty("ticksToCriterion", 1);

        JsonObject proof = new JsonObject();
        proof.addProperty("usePacket", "ServerboundUseItemPacket");
        proof.addProperty("releasePacket", "ServerboundPlayerActionPacket(RELEASE_USE_ITEM)");
        proof.addProperty("serverHandler", "ServerGamePacketListenerImpl.handleUseItem");
        proof.addProperty("nativeCriterionPath", "ServerPlayer.updateUsingItem->CriteriaTriggers.USING_ITEM");
        proof.addProperty("realPacketPath", true);
        proof.addProperty("liveAdvancementProgress", true);
        proof.addProperty("nativeActiveUseState", true);
        proof.addProperty("noDirectTrigger", true);
        proof.addProperty("noManualAward", true);
        proof.addProperty("noManualListenerInvocation", true);
        receipt.add("packetProof", proof);

        JsonObject cleanup = new JsonObject();
        cleanup.addProperty("playerRemoved", true);
        cleanup.addProperty("connectionRemoved", true);
        cleanup.addProperty("channelSettled", true);
        cleanup.addProperty("settlementMessages", 0);
        cleanup.addProperty("warningCount", 0);
        receipt.add("cleanup", cleanup);
        receipt.addProperty("action", action);
        return receipt;
    }
}
