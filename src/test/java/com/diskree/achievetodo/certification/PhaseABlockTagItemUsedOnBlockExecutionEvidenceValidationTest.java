package com.diskree.achievetodo.certification;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static com.diskree.achievetodo.certification.PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.Mode.*;

class PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    @TempDir Path tempDir;

    // Exact accepted bytes are used only as parser input under JUnit's isolated directory.
    // No synthetic receipt is ever written to the project's evidence paths.
    static byte[] acceptedFixtureBytes() throws IOException {
        Path persistent = ROOT.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Path source = Files.exists(persistent) ? persistent : ROOT.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.TEMP);
        byte[] bytes = Files.readAllBytes(source);
        assertEquals(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.ACCEPTED_TEMP_SHA256,
            PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.sha256(bytes));
        return bytes;
    }

    static void installPersistentFixture(Path root) throws IOException {
        for (Path relative : java.util.List.of(
            PhaseABlockTagItemUsedOnBlockCertification.SNAPSHOT,
            Path.of("reference/phase_a_preservation/files/final/bacap.zip")
        )) {
            Files.createDirectories(root.resolve(relative).getParent());
            Files.copy(ROOT.resolve(relative), root.resolve(relative));
        }
        Files.write(root.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT), acceptedFixtureBytes());
    }

    private JsonObject exactFixture() throws IOException {
        return JsonParser.parseString(new String(acceptedFixtureBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private void rejectPersistent(JsonObject artifact) throws IOException {
        Files.writeString(tempDir.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT), artifact.toString());
        assertThrows(IllegalStateException.class,
            () -> PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test void persistentMissingFileRejected() {
        assertThrows(java.nio.file.NoSuchFileException.class,
            () -> PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test void exactPersistentAndPromotableAcceptedWithoutAnyTemporaryRun() throws Exception {
        installPersistentFixture(tempDir);
        assertFalse(Files.exists(tempDir.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.TEMP)));
        PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.validate(tempDir, exactFixture(), TEMP_PROMOTABLE);
        var loaded = PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);
        assertEquals(PhaseABlockTagItemUsedOnBlockCertification.EXPECTED.keySet(), loaded.keySet());
        assertEquals(7, loaded.size());
        assertEquals(30, loaded.values().stream().mapToInt(data -> data.greenCriteria().size()).sum());
        loaded.forEach((id, data) -> {
            Set<String> criteria = Set.copyOf(PhaseABlockTagItemUsedOnBlockCertification.EXPECTED.get(id));
            assertEquals(id, data.advancementId());
            assertEquals(criteria, data.greenCriteria());
            assertEquals(Set.of(PhaseABlockTagItemUsedOnBlockCertification.FAMILY), data.families());
            assertEquals(Set.of(PhaseABlockTagItemUsedOnBlockCertification.SOURCE), data.sources());
            assertEquals(criteria, data.criteriaByFamily().get(PhaseABlockTagItemUsedOnBlockCertification.FAMILY));
            assertEquals(criteria, data.criteriaBySource().get(PhaseABlockTagItemUsedOnBlockCertification.SOURCE));
        });
        Path temp = tempDir.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.TEMP);
        Files.createDirectories(temp.getParent());
        Files.writeString(temp, "unrelated newer corrupt TEMP");
        assertEquals(loaded, PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
        var independent = exactFixture();
        independent.addProperty("runId", "independent-persistent-run");
        independent.getAsJsonArray("entries").forEach(e -> e.getAsJsonObject().addProperty("runId", "independent-persistent-run"));
        PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.validate(tempDir, independent, PERSISTENT);
    }

    @Test void persistentRejectsPartialDuplicateUnknownAndExcludedKeys() throws Exception {
        installPersistentFixture(tempDir);
        var partial = exactFixture(); partial.getAsJsonArray("entries").remove(0); rejectPersistent(partial);
        var duplicate = exactFixture(); duplicate.getAsJsonArray("entries").add(entry(duplicate).deepCopy()); rejectPersistent(duplicate);
        for (String criterion : new String[]{"unknown", "bone_meal_propagule"}) {
            var unknown = exactFixture();
            entry(unknown).addProperty("advancementId", "blazeandcave:farming/one_course_meal");
            entry(unknown).addProperty("criterion", criterion); rejectPersistent(unknown);
        }
    }

    @Test void persistentRejectsEveryOwnershipMismatchAtBothLevels() throws Exception {
        installPersistentFixture(tempDir);
        for (String field : new String[]{"family", "source", "catalogFingerprint", "minecraftVersion", "compatibilityMarker"}) {
            for (boolean root : new boolean[]{true, false}) {
                var a = exactFixture(); (root ? a : entry(a)).addProperty(field, "stale-or-wrong"); rejectPersistent(a);
            }
        }
        for (boolean root : new boolean[]{true, false}) {
            var a = exactFixture(); (root ? a : entry(a)).addProperty("runId", "mismatched-run"); rejectPersistent(a);
            a = exactFixture(); (root ? a : entry(a)).addProperty("runId", " "); rejectPersistent(a);
        }
    }

    @Test void persistentRetainsResultLifecycleAndEachActionProofGate() throws Exception {
        installPersistentFixture(tempDir);
        for (String field : new String[]{"criterionAfter", "joined", "semanticMutation", "productionPreconditions"}) {
            var a = exactFixture(); entry(a).addProperty(field, false); rejectPersistent(a);
        }
        var red = exactFixture(); entry(red).addProperty("result", "RED"); rejectPersistent(red);
        for (String action : new String[]{"WAX_SIGN", "DYE_SIGN", "GLOW_SIGN", "MAP_BANNER", "PLACE_FOOD", "GROW_OAK_SAPLING", "BOTTLE_HONEY"}) {
            var a = exactFixture();
            var receipt = a.getAsJsonArray("entries").asList().stream().map(JsonElement::getAsJsonObject)
                .filter(e -> action.equals(e.get("action").getAsString())).findFirst().orElseThrow();
            receipt.add("actionProof", new JsonObject()); rejectPersistent(a);
        }
    }

    private Path preparePromotionFixture() throws IOException {
        installPersistentFixture(tempDir);
        Path destination = tempDir.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Path source = tempDir.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.TEMP);
        Files.createDirectories(source.getParent());
        Files.move(destination, source);
        return source;
    }

    @Test void firstPromotionPreservesAcceptedBytesAndRejectsRepeat() throws Exception {
        Path source = preparePromotionFixture();
        byte[] before = Files.readAllBytes(source);
        PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.promoteTemporaryArtifact(tempDir);
        Path destination = tempDir.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        assertArrayEquals(before, Files.readAllBytes(destination));
        assertArrayEquals(before, Files.readAllBytes(source));
        assertEquals(30, PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.loadValidatedPersistentArtifact(tempDir).getAsJsonArray("entries").size());
        assertThrows(IllegalStateException.class,
            () -> PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.promoteTemporaryArtifact(tempDir));
        assertArrayEquals(before, Files.readAllBytes(destination));
    }

    @Test void promotionRejectsChangedSourceAndExistingManualDestination() throws Exception {
        Path source = preparePromotionFixture();
        Path destination = tempDir.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.writeString(source, "changed TEMP");
        assertEquals("PROMOTION_SOURCE_TEMP_CHANGED", assertThrows(IllegalStateException.class,
            () -> PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.promoteTemporaryArtifact(tempDir)).getMessage());
        assertFalse(Files.exists(destination));
        Files.write(source, acceptedFixtureBytes());
        Files.writeString(destination, "manual evidence");
        assertThrows(IllegalStateException.class,
            () -> PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.promoteTemporaryArtifact(tempDir));
        assertEquals("manual evidence", Files.readString(destination));
    }

    @Test void promotionRejectsExistingSiblingWithoutTouchingIt() throws Exception {
        preparePromotionFixture();
        Path destination = tempDir.resolve(PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Path sibling = destination.resolveSibling(destination.getFileName() + ".promotion.tmp");
        Files.writeString(sibling, "existing scratch");
        assertThrows(java.nio.file.FileAlreadyExistsException.class,
            () -> PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.promoteTemporaryArtifact(tempDir));
        assertFalse(Files.exists(destination));
        assertEquals("existing scratch", Files.readString(sibling));
    }
    // In-memory parser fixture only: never written, never counted as runtime evidence.
    private JsonObject partialFixture() throws Exception {
        JsonObject root = new JsonObject();
        root.addProperty("family", "BLOCK_TAG_ITEM_USED_ON_BLOCK"); root.addProperty("source", "PhaseABlockTagItemUsedOnBlockGameTest");
        root.addProperty("runId", "unit-test-only"); root.addProperty("catalogFingerprint", PhaseABlockTagItemUsedOnBlockCertification.fingerprint(ROOT));
        root.addProperty("minecraftVersion", "26.2"); root.addProperty("compatibilityMarker", "compat_26_2_r15");
        JsonObject r = root.deepCopy(); r.addProperty("advancementId", "blazeandcave:building/sign_off"); r.addProperty("criterion", "honeycomb");
        r.addProperty("requirementGroupIndex", 0); r.addProperty("blockTag", "#minecraft:all_signs"); r.addProperty("heldItem", "minecraft:honeycomb"); r.addProperty("action", "WAX_SIGN");
        r.addProperty("blockTagSampling", "POST_USE_CLICKED_POSITION"); r.addProperty("hand", "MAIN_HAND"); r.addProperty("trigger", "minecraft:item_used_on_block");
        r.addProperty("result", "GREEN"); r.addProperty("boundary", "ServerPlayerGameMode.useItemOn");
        r.addProperty("playerClass", "net.minecraft.server.level.ServerPlayer"); r.addProperty("playerUuid", "00000000-0000-0000-0000-000000000001");
        r.addProperty("gameMode", "SURVIVAL"); r.addProperty("clickedPosition", "1, 2, 1");
        r.addProperty("blockStateBefore", "Block{minecraft:oak_sign}[rotation=0,waterlogged=false]"); r.addProperty("blockStateAfter", r.get("blockStateBefore").getAsString());
        r.addProperty("interactionResult", "SUCCESS_SERVER");
        for (String field : java.util.List.of("joined", "connectionRegistered", "clientLoaded", "normalScheduler", "buildPermission", "productionPreconditions", "runtimeTagMembership", "interactionConsumesAction", "semanticMutation", "criterionAfter")) r.addProperty(field, true);
        r.addProperty("criterionBefore", false); r.addProperty("heldCountBefore", 1); r.addProperty("heldCountAfter", 0);
        JsonObject proof = new JsonObject(); proof.addProperty("waxedBefore", false); proof.addProperty("waxedAfter", true); r.add("actionProof", proof);
        JsonArray entries = new JsonArray(); entries.add(r); root.add("entries", entries); return root;
    }
    private JsonObject entry(JsonObject a) { return a.getAsJsonArray("entries").get(0).getAsJsonObject(); }
    private void reject(JsonObject a) { assertThrows(IllegalStateException.class, () -> PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.validate(ROOT, a, TEMP_DIAGNOSTIC)); }
    @Test void validPartialCanaryTemp() throws Exception { PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.validate(ROOT, partialFixture(), TEMP_DIAGNOSTIC); }
    @Test void revisedGlowRecognizedAndOldGlowKeyRejected() throws Exception {
        var a = partialFixture(); var r = entry(a);
        r.addProperty("advancementId", "minecraft:husbandry/make_a_sign_glow");
        r.addProperty("criterion", "glow_ink_sac");
        r.addProperty("heldItem", "minecraft:glow_ink_sac"); r.addProperty("action", "GLOW_SIGN");
        JsonObject proof = new JsonObject();
        proof.addProperty("textPresent", true); proof.addProperty("waxedBefore", false);
        proof.addProperty("glowingBefore", false); proof.addProperty("glowingAfter", true);
        r.add("actionProof", proof);
        assertTrue(PhaseABlockTagItemUsedOnBlockCertification.expectedKeys().contains("minecraft:husbandry/make_a_sign_glow#glow_ink_sac"));
        PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.validate(ROOT, a, TEMP_DIAGNOSTIC);
        r.addProperty("criterion", "make_a_sign_glow");
        var failure = assertThrows(IllegalStateException.class, () -> PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.validate(ROOT, a, TEMP_DIAGNOSTIC));
        assertEquals("Unknown criterion: minecraft:husbandry/make_a_sign_glow#make_a_sign_glow", failure.getMessage());
    }
    @Test void oldMixedProvenanceFingerprintRejectedAtBothLevels() throws Exception {
        for (boolean root : new boolean[]{true, false}) {
            var a = partialFixture();
            (root ? a : entry(a)).addProperty("catalogFingerprint", "sha-256:05983665e91b84c5b866fd807e7b0366ddfc9f8a7e884d34339ab807d79edf99");
            var failure = assertThrows(IllegalStateException.class, () -> PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.validate(ROOT, a, TEMP_DIAGNOSTIC));
            assertEquals("Wrong catalogFingerprint", failure.getMessage());
        }
    }
    @Test void dyeContractStillAcceptedUnderRevisedCatalog() throws Exception {
        var a = partialFixture(); var r = entry(a);
        r.addProperty("advancementId", "blazeandcave:building/colors_of_the_wind");
        r.addProperty("criterion", "white_dye"); r.addProperty("heldItem", "minecraft:white_dye"); r.addProperty("action", "DYE_SIGN");
        JsonObject proof = new JsonObject(); proof.addProperty("textPresent", true); proof.addProperty("waxedBefore", false);
        proof.addProperty("colorBefore", "black"); proof.addProperty("colorAfter", "white"); r.add("actionProof", proof);
        PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.validate(ROOT, a, TEMP_DIAGNOSTIC);
    }
    @Test void duplicateRejected() throws Exception { var a = partialFixture(); a.getAsJsonArray("entries").add(entry(a).deepCopy()); reject(a); }
    @Test void unknownAndExcludedCriteriaRejected() throws Exception { for (String c : new String[]{"extra", "bone_meal_propagule"}) { var a = partialFixture(); entry(a).addProperty("criterion", c); reject(a); } }
    @Test void wrongFamilyAndSourceRejectedAtBothLevels() throws Exception {
        for (String field : new String[]{"family", "source"}) for (boolean root : new boolean[]{true, false}) { var a = partialFixture(); (root ? a : entry(a)).addProperty(field, "wrong"); reject(a); }
    }
    @Test void wrongFingerprintRejectedAtBothLevels() throws Exception { for (boolean root : new boolean[]{true, false}) { var a = partialFixture(); (root ? a : entry(a)).addProperty("catalogFingerprint", "sha-256:stale"); reject(a); } }
    @Test void requiresFalseToTrue() throws Exception {
        var a = partialFixture(); entry(a).addProperty("criterionBefore", true); reject(a);
        a = partialFixture(); entry(a).addProperty("criterionAfter", false); reject(a);
    }
    @Test void missingSemanticMutationRejected() throws Exception {
        var a = partialFixture(); entry(a).remove("semanticMutation"); reject(a);
        a = partialFixture(); entry(a).getAsJsonObject("actionProof").remove("waxedAfter"); reject(a);
        a = partialFixture(); entry(a).getAsJsonObject("actionProof").addProperty("waxedAfter", false); reject(a);
        a = partialFixture(); entry(a).addProperty("heldCountAfter", 1); reject(a);
    }
    @Test void runIdMismatchRejected() throws Exception { var a = partialFixture(); entry(a).addProperty("runId", "other-run"); reject(a); }
    @Test void promotablePartialRejectedWithoutFabricatingExact30Receipts() throws Exception {
        var a = partialFixture();
        var failure = assertThrows(IllegalStateException.class, () -> PhaseABlockTagItemUsedOnBlockExecutionEvidenceValidation.validate(ROOT, a, TEMP_PROMOTABLE));
        assertEquals("TEMP promotable requires exact30", failure.getMessage());
        assertEquals(30, PhaseABlockTagItemUsedOnBlockCertification.expectedKeys().size());
        assertEquals(1, a.getAsJsonArray("entries").size());
    }
    @Test void redAndLifecycleFailuresRejected() throws Exception {
        var a = partialFixture(); entry(a).addProperty("result", "RED"); reject(a);
        for (String field : new String[]{"joined", "connectionRegistered", "clientLoaded", "buildPermission", "runtimeTagMembership", "productionPreconditions"}) { a = partialFixture(); entry(a).addProperty(field, false); reject(a); }
    }
    @Test void wrongBoundaryItemAndSamplingRejected() throws Exception {
        for (String field : new String[]{"boundary", "heldItem", "blockTag", "blockTagSampling", "hand", "trigger"}) {
            var a = partialFixture(); entry(a).addProperty(field, "wrong"); reject(a);
        }
    }
    @Test void campfireConsumeWithoutActualFoodPlacementRejected() throws Exception {
        var a = partialFixture(); var r = entry(a);
        var catalog = JsonParser.parseString(PhaseABlockTagItemUsedOnBlockCertification.generateSnapshot(ROOT)).getAsJsonObject();
        JsonObject food = catalog.getAsJsonArray("cases").asList().stream().map(JsonElement::getAsJsonObject)
            .filter(c -> c.get("action").getAsString().equals("PLACE_FOOD")).findFirst().orElseThrow();
        for (String field : new String[]{"advancementId", "criterion", "requirementGroupIndex", "action", "heldItem", "blockTag"}) r.add(field, food.get(field).deepCopy());
        r.addProperty("interactionResult", "CONSUME");
        JsonObject proof = new JsonObject();
        for (String field : new String[]{"placementAllowed", "campfireRecipe", "useCampfireUnlocked", "slotEmptyBefore"}) proof.addProperty(field, true);
        proof.addProperty("lockedLandmark", false); proof.addProperty("slotItemAfter", "minecraft:air");
        proof.addProperty("slotCountAfter", 0); proof.addProperty("slotIndex", 0);
        r.add("actionProof", proof); reject(a);
    }
}
