package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhaseAItemTagPlacedBlockExecutionEvidenceValidationTest {
    @TempDir
    Path tempDir;

    @Test
    void partialTemporaryArtifactAcceptedForDiagnosticSlice() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            entryJson("blazeandcave:building/en_garde", "fence", "minecraft:fences", "minecraft:acacia_fence", "minecraft:acacia_fence")
        ));

        PhaseAItemTagPlacedBlockExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir);

        assertEquals(1, artifact.receipts().size());
        assertEquals("minecraft:acacia_fence", artifact.receipts().getFirst().selectedItem());
    }

    @Test
    void promotableValidationStillRequiresExactSupportedSet() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            entryJson("blazeandcave:building/en_garde", "fence", "minecraft:fences", "minecraft:acacia_fence", "minecraft:acacia_fence")
        ));

        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void wrongItemTagRejected() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"itemTag\":\"minecraft:fences\"", "\"itemTag\":\"minecraft:stairs\"")));

        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void duplicateKeyRejected() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            entryJson("blazeandcave:building/en_garde", "fence", "minecraft:fences", "minecraft:acacia_fence", "minecraft:acacia_fence"),
            entryJson("blazeandcave:building/en_garde", "fence", "minecraft:fences", "minecraft:oak_fence", "minecraft:oak_fence")
        ));

        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void wrongFamilyRejected() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"family\":\"ITEM_TAG_PLACED_BLOCK\"", "\"family\":\"WRONG_FAMILY\"")));

        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void wrongSourceRejected() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"source\":\"PhaseAItemTagPlacedBlockGameTest\"", "\"source\":\"WrongSource\"")));

        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void redReceiptRejected() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"result\":\"GREEN\"", "\"result\":\"RED\"")));

        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void criterionBooleansRejectedWhenInvalid() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"criterionBefore\":false", "\"criterionBefore\":true")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"criterionAfter\":true", "\"criterionAfter\":false")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void badHandCountsRejected() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"handCountBefore\":1", "\"handCountBefore\":2")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"handCountAfter\":0", "\"handCountAfter\":1")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void tagMembershipAndCountRejectedWhenInvalid() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"tagMembership\":true", "\"tagMembership\":false")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"tagMemberCount\":13", "\"tagMemberCount\":0")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void placementStateRejectedWhenInvalid() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"ticksToCriterion\":0", "\"ticksToCriterion\":-1")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"placedBlockBefore\":\"minecraft:air\"", "\"placedBlockBefore\":\"minecraft:stone\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"placedBlockAfter\":\"minecraft:acacia_fence\"", "\"placedBlockAfter\":\"minecraft:air\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"placedBlockAfter\":\"minecraft:acacia_fence\"", "\"placedBlockAfter\":\"minecraft:oak_fence\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void rootAndEntryMetadataMismatchesRejected() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> entry.replaceFirst("\"runId\":\"run-1\"", "\"runId\":\"run-2\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        String fingerprint = currentFingerprint();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"catalogFingerprint\":\"" + fingerprint + "\"", "\"catalogFingerprint\":\"sha-256:stale\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"minecraftVersion\":\"26.2\"", "\"minecraftVersion\":\"26.1\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"compatibilityMarker\":\"compat_26_2_r15\"", "\"compatibilityMarker\":\"compat_26_2_r14\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void missingAndExtraSupportedReceiptsRejectedForPromotableValidation() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(exactArtifactJson());
        assertEquals(5, PhaseAItemTagPlacedBlockExecutionEvidenceValidation
            .loadValidatedPromotableTemporaryArtifact(tempDir)
            .receipts()
            .size());

        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            exactEntries()[0], exactEntries()[1], exactEntries()[2], exactEntries()[3]
        ));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir));

        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            exactEntries()[0], exactEntries()[1], exactEntries()[2], exactEntries()[3], exactEntries()[4],
            entryJson("blazeandcave:building/fake", "fake", "minecraft:fences", "minecraft:acacia_fence", "minecraft:acacia_fence")
        ));
        assertThrows(IllegalStateException.class,
            () -> PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void persistentLoaderRequiresExactCoverage() throws IOException {
        writeFrozenCatalog();
        writePersistentArtifact(exactArtifactJson());

        assertEquals(5, PhaseAItemTagPlacedBlockExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir).size());
    }

    private void writeFrozenCatalog() throws IOException {
        writeCatalog(catalogJson(
            caseJson("blazeandcave:building/en_garde", "fence", "minecraft:fences"),
            caseJson("blazeandcave:building/hanging_around", "hanging_sign", "minecraft:hanging_signs"),
            caseJson("blazeandcave:building/its_a_sign", "sign", "minecraft:signs"),
            caseJson("blazeandcave:building/its_a_trap", "trapdoor", "minecraft:trapdoors"),
            caseJson("blazeandcave:building/raise_the_flag", "banner", "minecraft:banners")
        ));
    }

    private void writeCatalog(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAItemTagPlacedBlockExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeTemporaryArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAItemTagPlacedBlockExecutionEvidenceValidation.TEMPORARY_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writePersistentArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAItemTagPlacedBlockExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private String currentFingerprint() throws IOException {
        return PhaseAItemTagPlacedBlockExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseAItemTagPlacedBlockExecutionEvidenceValidation.CATALOG_PATH)
        );
    }

    private String exactArtifactJson() throws IOException {
        return artifactJson(currentFingerprint(), "run-1", exactEntries());
    }

    private String mutatedExactArtifact(UnaryOperator<String> mutator) throws IOException {
        return mutator.apply(exactArtifactJson());
    }

    private static String[] exactEntries() {
        return new String[]{
            entryJson("blazeandcave:building/en_garde", "fence", "minecraft:fences", "minecraft:acacia_fence", "minecraft:acacia_fence"),
            entryJson("blazeandcave:building/hanging_around", "hanging_sign", "minecraft:hanging_signs", "minecraft:acacia_hanging_sign", "minecraft:acacia_hanging_sign"),
            entryJson("blazeandcave:building/its_a_sign", "sign", "minecraft:signs", "minecraft:acacia_sign", "minecraft:acacia_sign"),
            entryJson("blazeandcave:building/its_a_trap", "trapdoor", "minecraft:trapdoors", "minecraft:acacia_trapdoor", "minecraft:acacia_trapdoor"),
            entryJson("blazeandcave:building/raise_the_flag", "banner", "minecraft:banners", "minecraft:acacia_banner", "minecraft:acacia_banner")
        };
    }

    private static String replace(String source, String from, String to) {
        return source.replace(from, to);
    }

    private static String catalogJson(String... casesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_item_tag_placed_block_case_catalog\","
            + "\"cases\":[" + String.join(",", casesJson) + "]"
            + "}";
    }

    private static String caseJson(String advancementId, String criterion, String itemTag) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"itemTag\":\"" + itemTag + "\","
            + "\"automationEligibility\":\"SUPPORTED\""
            + "}";
    }

    private static String artifactJson(String fingerprint, String runId, String... entriesJson) {
        String[] resolvedEntries = new String[entriesJson.length];
        for (int i = 0; i < entriesJson.length; i++) {
            resolvedEntries[i] = entriesJson[i].replace("__ENTRY_FINGERPRINT__", fingerprint).replace("\"runId\":\"run-1\"", "\"runId\":\"" + runId + "\"");
        }
        return "{"
            + "\"snapshot\":\"phase_a_item_tag_placed_block_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"" + runId + "\","
            + "\"generatedAt\":\"2026-08-23T00:00:00Z\","
            + "\"entries\":[" + String.join(",", resolvedEntries) + "]"
            + "}";
    }

    private static String entryJson(String advancementId, String criterion, String itemTag, String selectedItem, String placedBlock) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"itemTag\":\"" + itemTag + "\","
            + "\"selectedItem\":\"" + selectedItem + "\","
            + "\"placedBlock\":\"" + placedBlock + "\","
            + "\"family\":\"ITEM_TAG_PLACED_BLOCK\","
            + "\"source\":\"PhaseAItemTagPlacedBlockGameTest\","
            + "\"result\":\"GREEN\","
            + "\"criterionBefore\":false,"
            + "\"criterionAfter\":true,"
            + "\"handCountBefore\":1,"
            + "\"handCountAfter\":0,"
            + "\"interactionResult\":\"Success[swingSource=CLIENT, itemContext=ItemContext[wasItemInteraction=true, heldItemTransformedTo=null]]\","
            + "\"targetPos\":\"1, 2, 1\","
            + "\"supportPos\":\"1, 1, 1\","
            + "\"placedBlockBefore\":\"minecraft:air\","
            + "\"placedBlockAfter\":\"" + placedBlock + "\","
            + "\"tagMembership\":true,"
            + "\"tagMemberCount\":13,"
            + "\"ticksToCriterion\":0,"
            + "\"catalogFingerprint\":\"__ENTRY_FINGERPRINT__\","
            + "\"runId\":\"run-1\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\""
            + "}";
    }
}
