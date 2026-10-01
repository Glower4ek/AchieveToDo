package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidationTest {
    private static final Path FROZEN_BACAP = Path.of(
        "reference", "phase_a_preservation", "files", "final", "bacap.zip"
    );

    @Test
    void diagnosticAcceptsAValidSemanticSubset(@TempDir Path tempDir) throws IOException {
        JsonObject catalog = prepareCatalog(tempDir);
        JsonObject artifact = artifact(catalog, "diagnostic-run", List.of(receipt(catalog, 0)));
        writeArtifact(tempDir, PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.TEMPORARY_ARTIFACT, artifact);

        var validated = PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir);
        assertEquals(1, validated.receipts().size());
        assertEquals(PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.Mode.TEMP_DIAGNOSTIC, validated.mode());
    }

    @Test
    void promotableRequiresTheExactFifteenKeySet(@TempDir Path tempDir) throws IOException {
        JsonObject catalog = prepareCatalog(tempDir);
        writeArtifact(
            tempDir,
            PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            artifact(catalog, "short-run", List.of(receipt(catalog, 0)))
        );
        assertThrows(
            IllegalStateException.class,
            () -> PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir)
        );

        List<JsonObject> receipts = new ArrayList<>();
        for (int index = 0; index < 15; index++) {
            receipts.add(receipt(catalog, index));
        }
        JsonObject exact = artifact(catalog, "exact-run", receipts);
        writeArtifact(tempDir, PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.TEMPORARY_ARTIFACT, exact);
        var validated = PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir);
        assertEquals(15, validated.receipts().size());
    }

    @Test
    void persistentLoaderIsArtifactLocalAndRejectsWeakProof(@TempDir Path tempDir) throws IOException {
        JsonObject catalog = prepareCatalog(tempDir);
        List<JsonObject> receipts = new ArrayList<>();
        for (int index = 0; index < 15; index++) {
            receipts.add(receipt(catalog, index));
        }
        JsonObject exact = artifact(catalog, "persistent-run", receipts);
        writeArtifact(tempDir, PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT, exact);
        JsonObject unrelatedActiveRun = new JsonObject();
        unrelatedActiveRun.addProperty("runId", "not-the-persistent-run");
        writeArtifact(tempDir, Path.of("build", "tmp", "phase_a_certification", "unrelated.run.json"), unrelatedActiveRun);

        var evidence = PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);
        assertEquals(4, evidence.size());
        assertEquals(15, evidence.values().stream().mapToInt(value -> value.greenCriteria().size()).sum());

        JsonObject weak = exact.deepCopy();
        weak.getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("interactionStack").addProperty("countAfter", 0);
        writeArtifact(tempDir, PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT, weak);
        assertThrows(
            IllegalStateException.class,
            () -> PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir)
        );
    }

    @Test
    void duplicateAndUnknownKeysAreRejected(@TempDir Path tempDir) throws IOException {
        JsonObject catalog = prepareCatalog(tempDir);
        JsonObject duplicate = artifact(catalog, "duplicate-run", List.of(receipt(catalog, 0), receipt(catalog, 0)));
        writeArtifact(tempDir, PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.TEMPORARY_ARTIFACT, duplicate);
        assertThrows(
            IllegalStateException.class,
            () -> PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir)
        );

        JsonObject unknown = artifact(catalog, "unknown-run", List.of(receipt(catalog, 0)));
        unknown.getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("criterion", "not_in_catalog");
        writeArtifact(tempDir, PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.TEMPORARY_ARTIFACT, unknown);
        assertThrows(
            IllegalStateException.class,
            () -> PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir)
        );
    }

    private static JsonObject prepareCatalog(Path tempDir) throws IOException {
        Path frozenTarget = tempDir.resolve(FROZEN_BACAP);
        Files.createDirectories(frozenTarget.getParent());
        Files.copy(Path.of("").toAbsolutePath().normalize().resolve(FROZEN_BACAP), frozenTarget);
        Path catalogPath = tempDir.resolve(PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(catalogPath.getParent());
        String catalogText = PhaseAItemTagItemUsedOnBlockCertification.generateSnapshot(tempDir);
        Files.writeString(catalogPath, catalogText, StandardCharsets.UTF_8);
        return JsonParser.parseString(catalogText).getAsJsonObject();
    }

    private static JsonObject artifact(JsonObject catalog, String runId, List<JsonObject> entries) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.SNAPSHOT);
        root.addProperty("schemaVersion", 1);
        root.addProperty("family", "ITEM_TAG_ITEM_USED_ON_BLOCK");
        root.addProperty("source", PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.SOURCE);
        root.addProperty("minecraftVersion", "26.2");
        root.addProperty("compatibilityMarker", "compat_26_2_r15");
        root.addProperty("catalogFingerprint", PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.fingerprintFromText(catalog));
        root.addProperty("runId", runId);
        root.addProperty("generatedAt", "2026-09-06T00:00:00Z");
        JsonArray jsonEntries = new JsonArray();
        for (JsonObject entry : entries) {
            JsonObject copy = entry.deepCopy();
            copy.addProperty("family", "ITEM_TAG_ITEM_USED_ON_BLOCK");
            copy.addProperty("source", PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.SOURCE);
            copy.addProperty("catalogFingerprint", PhaseAItemTagItemUsedOnBlockExecutionEvidenceValidation.fingerprintFromText(catalog));
            copy.addProperty("runId", runId);
            copy.addProperty("minecraftVersion", "26.2");
            copy.addProperty("compatibilityMarker", "compat_26_2_r15");
            copy.addProperty("result", "GREEN");
            jsonEntries.add(copy);
        }
        root.add("entries", jsonEntries);
        return root;
    }

    private static JsonObject receipt(JsonObject catalog, int index) {
        JsonObject definition = catalog.getAsJsonArray("cases").get(index).getAsJsonObject();
        String action = definition.get("action").getAsString();
        String selectedItem = definition.get("preferredToolItem").getAsString();
        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", definition.get("advancementId").getAsString());
        receipt.addProperty("criterion", definition.get("criterion").getAsString());
        receipt.addProperty("requirementGroupIndex", definition.get("requirementGroupIndex").getAsInt());
        receipt.addProperty("trigger", "minecraft:item_used_on_block");
        receipt.addProperty("itemTag", definition.get("itemTag").getAsString());
        receipt.addProperty("selectedItem", selectedItem);
        receipt.addProperty("runtimeItemTagMembership", true);
        receipt.addProperty("runtimeItemTagMemberCount", 7);
        receipt.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        receipt.addProperty("hand", "MAIN_HAND");
        receipt.addProperty("playerClass", "net.minecraft.server.level.ServerPlayer");
        receipt.addProperty("playerUuid", "00000000-0000-0000-0000-000000000001");
        receipt.addProperty("gameMode", "SURVIVAL");
        receipt.addProperty("joined", true);
        receipt.addProperty("connectionRegistered", true);
        receipt.addProperty("clientLoaded", true);
        receipt.addProperty("normalScheduler", true);
        receipt.addProperty("buildPermission", true);
        receipt.addProperty("clickedPosition", "[1, 2, 1]");
        receipt.addProperty("blockStateBefore", "Block{" + definition.get("fixtureBlockBefore").getAsString() + "}");
        receipt.addProperty("blockStateAfter", "Block{" + definition.get("expectedPostBlock").getAsString() + "}");
        receipt.addProperty("blockIdBefore", definition.get("fixtureBlockBefore").getAsString());
        receipt.addProperty("blockIdAfter", definition.get("expectedPostBlock").getAsString());
        receipt.addProperty("interactionResult", "Success[swingSource=CLIENT, itemContext=ItemContext[wasItemInteraction=true, heldItemTransformedTo=null]]");
        receipt.addProperty("interactionConsumesAction", true);
        receipt.addProperty("semanticMutation", true);
        receipt.addProperty("criterionBefore", false);
        receipt.addProperty("criterionAfter", true);
        receipt.addProperty("ticksToCriterion", 0);
        receipt.add("blockStateBeforeProperties", state(definition.getAsJsonObject("fixtureStateBefore")));
        receipt.add("blockStateAfterProperties", state(definition.getAsJsonObject("expectedPostState")));

        JsonObject preconditions = new JsonObject();
        preconditions.addProperty("scoreboardObjective", "bac_advancements");
        preconditions.addProperty("scoreBefore", 0);
        preconditions.addProperty("scoreAfter", 1000);
        preconditions.addProperty("abilityLockedBefore", true);
        preconditions.addProperty("abilityLockedAfter", false);
        preconditions.addProperty("lockedLandmark", false);
        preconditions.addProperty("ability", "USE_DIAMOND_TOOLS");
        receipt.add("productionPreconditions", preconditions);

        JsonObject stack = new JsonObject();
        stack.addProperty("itemBefore", selectedItem);
        stack.addProperty("itemAfter", selectedItem);
        stack.addProperty("countBefore", 1);
        stack.addProperty("countAfter", 1);
        stack.addProperty("damageBefore", 0);
        stack.addProperty("damageAfter", 1);
        stack.addProperty("sameStackReference", true);
        receipt.add("interactionStack", stack);

        JsonObject proof = new JsonObject();
        proof.addProperty("action", action);
        proof.addProperty("realUseOn", true);
        proof.addProperty("triggerPath", "ServerPlayerGameMode.useItemOn->ItemUsedOnLocationTrigger");
        proof.addProperty("preBlock", definition.get("fixtureBlockBefore").getAsString());
        proof.addProperty("postBlock", definition.get("expectedPostBlock").getAsString());
        proof.addProperty("interactionConsumesAction", true);
        proof.addProperty("toolDamageDelta", 1);
        proof.addProperty("sameStackReference", true);
        proof.addProperty("criterionBefore", false);
        proof.addProperty("criterionAfter", true);
        if (action.equals("STRIP_WOOD") || action.equals("STRIP_LOG")) {
            proof.addProperty("operation", "STRIP");
            proof.addProperty("vanillaMethod", "AxeItem.evaluateNewBlockState");
        } else if (action.equals("CREATE_PATH")) {
            proof.addProperty("operation", "FLATTEN");
            proof.addProperty("vanillaMethod", "ShovelItem.useOn");
            proof.addProperty("clickedFace", "UP");
            proof.addProperty("aboveBlock", "minecraft:air");
        } else {
            proof.addProperty("operation", "DEWAX");
            proof.addProperty("vanillaMethod", "AxeItem.evaluateNewBlockState");
            proof.addProperty("litBefore", true);
            proof.addProperty("litAfter", true);
            proof.addProperty("waxedBefore", true);
            proof.addProperty("waxedAfter", false);
        }
        receipt.add("actionProof", proof);
        return receipt;
    }

    private static JsonObject state(JsonObject source) {
        return source == null ? new JsonObject() : source.deepCopy();
    }

    private static void writeArtifact(Path root, Path relative, JsonObject artifact) throws IOException {
        Path path = root.resolve(relative);
        Files.createDirectories(path.getParent());
        Files.writeString(path, artifact.toString(), StandardCharsets.UTF_8);
    }
}
