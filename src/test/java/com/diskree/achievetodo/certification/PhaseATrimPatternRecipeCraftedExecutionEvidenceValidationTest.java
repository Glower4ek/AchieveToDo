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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhaseATrimPatternRecipeCraftedExecutionEvidenceValidationTest {
    @TempDir
    Path tempDir;

    @Test
    void partialTemporaryCanaryArtifactIsAcceptedForDiagnostics() throws IOException {
        copyCatalog();
        String fingerprint = fingerprint();
        String runId = "run-1";
        writeArtifact(artifactJson(fingerprint, runId, receiptJson(fingerprint, runId, "sentry_armor_trim")));

        PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir);

        assertEquals(1, artifact.receipts().size());
        assertEquals("sentry_armor_trim", artifact.receipts().getFirst().criterion());
    }

    @Test
    void promotableValidationRejectsPartialCanaryCoverage() throws IOException {
        copyCatalog();
        String fingerprint = fingerprint();
        writeArtifact(artifactJson(fingerprint, "run-1", receiptJson(fingerprint, "run-1", "sentry_armor_trim")));

        assertThrows(
            IllegalStateException.class,
            () -> PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir)
        );
    }

    @Test
    void exact18TemporaryArtifactIsAcceptedPromotably() throws IOException {
        copyCatalog();
        String fingerprint = fingerprint();
        writeArtifact(exactArtifact(fingerprint, "run-1"));

        assertEquals(
            18,
            PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation
                .loadValidatedPromotableTemporaryArtifact(tempDir)
                .receipts()
                .size()
        );
    }

    @Test
    void persistentExact18ArtifactIsAccepted() throws IOException {
        copyCatalog();
        String fingerprint = fingerprint();
        writePersistentArtifact(exactArtifact(fingerprint, "closed-run"));

        Map<String, PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);

        assertEquals(Set.of("minecraft:adventure/trim_with_any_armor_pattern"), evidence.keySet());
        assertEquals(expectedCriteria(), evidence.get("minecraft:adventure/trim_with_any_armor_pattern").greenCriteria());
        assertEquals(
            Set.of(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.FAMILY),
            evidence.get("minecraft:adventure/trim_with_any_armor_pattern").families()
        );
        assertEquals(
            Set.of(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.SOURCE),
            evidence.get("minecraft:adventure/trim_with_any_armor_pattern").sources()
        );
    }

    @Test
    void persistentArtifactIgnoresNewerActiveRunState() throws IOException {
        copyCatalog();
        String fingerprint = fingerprint();
        writePersistentArtifact(exactArtifact(fingerprint, "closed-run"));
        writeRunState(fingerprint, "newer-active-run");

        assertEquals(
            expectedCriteria(),
            PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation
                .loadValidatedRuntimeEvidence(tempDir)
                .get("minecraft:adventure/trim_with_any_armor_pattern")
                .greenCriteria()
        );
    }

    @Test
    void persistentArtifactRejectsInternalRunIdMismatch() throws IOException {
        copyCatalog();
        String fingerprint = fingerprint();
        List<JsonObject> receipts = exactReceipts(fingerprint, "closed-run");
        receipts.getFirst().addProperty("runId", "different-run");
        writePersistentArtifact(artifactJson(fingerprint, "closed-run", receipts.toArray(JsonObject[]::new)));

        assertThrows(
            IllegalStateException.class,
            () -> PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir)
        );
    }

    @Test
    void wrongRecipeIdIsRejected() throws IOException {
        copyCatalog();
        String fingerprint = fingerprint();
        JsonObject receipt = receiptJson(fingerprint, "run-1", "sentry_armor_trim");
        receipt.addProperty("recipeId", "minecraft:coast_armor_trim_smithing_template_smithing_trim");
        writeArtifact(artifactJson(fingerprint, "run-1", receipt));

        assertThrows(
            IllegalStateException.class,
            () -> PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir)
        );
    }

    @Test
    void duplicateReceiptIsRejected() throws IOException {
        copyCatalog();
        String fingerprint = fingerprint();
        JsonObject first = receiptJson(fingerprint, "run-1", "sentry_armor_trim");
        JsonObject second = receiptJson(fingerprint, "run-1", "sentry_armor_trim");
        writeArtifact(artifactJson(fingerprint, "run-1", first, second));

        assertThrows(
            IllegalStateException.class,
            () -> PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir)
        );
    }

    @Test
    void missingPersistentReceiptIsRejected() throws IOException {
        copyCatalog();
        String fingerprint = fingerprint();
        List<JsonObject> receipts = exactReceipts(fingerprint, "closed-run");
        receipts.removeLast();
        writePersistentArtifact(artifactJson(fingerprint, "closed-run", receipts.toArray(JsonObject[]::new)));

        assertThrows(
            IllegalStateException.class,
            () -> PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir)
        );
    }

    @Test
    void temporaryArtifactRequiresMatchingActiveRunState() throws IOException {
        copyCatalog();
        String fingerprint = fingerprint();
        writeArtifactAt(
            PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.TEMPORARY_ARTIFACT,
            artifactJson(fingerprint, "run-1", receiptJson(fingerprint, "run-1", "sentry_armor_trim"))
        );

        assertThrows(
            IllegalStateException.class,
            () -> PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir)
        );
    }

    @Test
    void missingTemporaryArtifactIsRejected() {
        assertThrows(
            IllegalStateException.class,
            () -> PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir)
        );
    }

    private void copyCatalog() throws IOException {
        Path destination = tempDir.resolve(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(destination.getParent());
        Files.copy(
            Path.of("").toAbsolutePath().normalize().resolve(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.CATALOG_PATH),
            destination
        );
    }

    private String fingerprint() throws IOException {
        return PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.CATALOG_PATH)
        );
    }

    private Set<String> expectedCriteria() throws IOException {
        Set<String> criteria = new LinkedHashSet<>();
        for (PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.CatalogCase catalogCase : catalogCases()) {
            criteria.add(catalogCase.criterion());
        }
        return criteria;
    }

    private List<JsonObject> exactReceipts(String fingerprint, String runId) throws IOException {
        List<JsonObject> receipts = new ArrayList<>();
        for (PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.CatalogCase catalogCase : catalogCases()) {
            receipts.add(receiptJson(fingerprint, runId, catalogCase.criterion()));
        }
        return receipts;
    }

    private List<PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.CatalogCase> catalogCases() throws IOException {
        return new ArrayList<>(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation
            .loadCatalog(tempDir.resolve(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.CATALOG_PATH))
            .values());
    }

    private JsonObject exactArtifact(String fingerprint, String runId) throws IOException {
        return artifactJson(fingerprint, runId, exactReceipts(fingerprint, runId).toArray(JsonObject[]::new));
    }

    private void writeArtifact(JsonObject artifact) throws IOException {
        writeArtifactAt(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.TEMPORARY_ARTIFACT, artifact);
        writeRunState(
            artifact.get("catalogFingerprint").getAsString(),
            artifact.get("runId").getAsString()
        );
    }

    private void writePersistentArtifact(JsonObject artifact) throws IOException {
        writeArtifactAt(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.PERSISTENT_ARTIFACT, artifact);
    }

    private void writeRunState(String fingerprint, String runId) throws IOException {
        JsonObject runState = new JsonObject();
        runState.addProperty("snapshot", "trim_pattern_recipe_crafted_execution_evidence");
        runState.addProperty("minecraftVersion", "26.2");
        runState.addProperty("compatibilityMarker", "compat_26_2_r15");
        runState.addProperty("catalogFingerprint", fingerprint);
        runState.addProperty("runId", runId);
        runState.addProperty("generatedAt", "2026-08-30T00:00:00Z");
        writeArtifactAt(PhaseATrimPatternRecipeCraftedExecutionEvidenceValidation.RUN_STATE_ARTIFACT, runState);
    }

    private void writeArtifactAt(Path relativeArtifactPath, JsonObject artifact) throws IOException {
        Path destination = tempDir.resolve(relativeArtifactPath);
        Files.createDirectories(destination.getParent());
        Files.writeString(destination, artifact.toString(), StandardCharsets.UTF_8);
    }

    private static JsonObject artifactJson(String fingerprint, String runId, JsonObject... receipts) {
        JsonObject root = new JsonObject();
        root.addProperty("snapshot", "trim_pattern_recipe_crafted_execution_evidence");
        root.addProperty("minecraftVersion", "26.2");
        root.addProperty("compatibilityMarker", "compat_26_2_r15");
        root.addProperty("catalogFingerprint", fingerprint);
        root.addProperty("runId", runId);
        root.addProperty("generatedAt", "2026-08-30T00:00:00Z");
        JsonArray entries = new JsonArray();
        for (JsonObject receipt : receipts) {
            entries.add(receipt);
        }
        root.add("entries", entries);
        return root;
    }

    private static JsonObject receiptJson(String fingerprint, String runId, String criterion) {
        String pattern = criterion.substring(0, criterion.length() - "_armor_trim".length());
        JsonObject receipt = new JsonObject();
        receipt.addProperty("advancementId", "minecraft:adventure/trim_with_any_armor_pattern");
        receipt.addProperty("criterion", criterion);
        receipt.addProperty("trigger", "minecraft:recipe_crafted");
        receipt.addProperty("recipeId", "minecraft:" + criterion + "_smithing_template_smithing_trim");
        receipt.addProperty("expectedTrimPattern", "minecraft:" + pattern);
        receipt.addProperty("templateItem", "minecraft:" + criterion + "_smithing_template");
        receipt.addProperty("baseItem", "minecraft:iron_helmet");
        receipt.addProperty("additionItem", "minecraft:lapis_lazuli");
        receipt.addProperty("producedItem", "minecraft:iron_helmet");
        receipt.addProperty("producedTrimPattern", "minecraft:" + pattern);
        receipt.addProperty("producedTrimMaterial", "minecraft:lapis");
        receipt.addProperty("criterionBefore", false);
        receipt.addProperty("criterionAfter", true);
        receipt.addProperty("normalResultTaken", true);
        receipt.addProperty("smithingTableInteraction", true);
        receipt.addProperty("family", "TRIM_PATTERN_RECIPE_CRAFTED");
        receipt.addProperty("source", "PhaseATrimPatternRecipeCraftedGameTest");
        receipt.addProperty("result", "GREEN");
        receipt.addProperty("ticksToCriterion", 1);
        receipt.addProperty("catalogFingerprint", fingerprint);
        receipt.addProperty("runId", runId);
        return receipt;
    }
}
