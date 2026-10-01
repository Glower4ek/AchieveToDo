package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.UnaryOperator;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @TempDir
    Path tempDir;

    @Test
    void partialTemporaryArtifactAcceptedForDiagnosticSlice() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            entryJson("minecraft:adventure/kill_a_mob", "monster_hunter", "blazeandcave:hostile_monsters", "minecraft:zombie")
        ));

        PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir);

        assertEquals(1, artifact.receipts().size());
        assertEquals("minecraft:zombie", artifact.receipts().getFirst().selectedEntityType());
    }

    @Test
    void promotableValidationStillRequiresExactSupportedSet() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(artifactJson(currentFingerprint(), "run-1",
            entryJson("minecraft:adventure/kill_a_mob", "monster_hunter", "blazeandcave:hostile_monsters", "minecraft:zombie")
        ));

        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedPromotableTemporaryArtifact(tempDir));
    }

    @Test
    void wrongFamilyAndSourceRejected() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"family\":\"ENTITY_TYPE_TAG_PLAYER_KILLED_ENTITY\"", "\"family\":\"WRONG_FAMILY\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"source\":\"PhaseAEntityTypeTagPlayerKilledEntityGameTest\"", "\"source\":\"WrongSource\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void wrongTagAndBlankSelectedEntityRejected() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"entityTypeTag\":\"blazeandcave:hostile_monsters\"", "\"entityTypeTag\":\"blazeandcave:piglins\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"selectedEntityType\":\"minecraft:zombie\"", "\"selectedEntityType\":\"\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void booleansAndCountsRejectedWhenInvalid() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"tagMembership\":true", "\"tagMembership\":false")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"tagMemberCount\":38", "\"tagMemberCount\":0")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"criterionBefore\":false", "\"criterionBefore\":true")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"criterionAfter\":true", "\"criterionAfter\":false")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"playerKillAttributed\":true", "\"playerKillAttributed\":false")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void entityKillAndMetadataRequirementsFailClosed() throws IOException {
        writeFrozenCatalog();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"entityAliveAfter\":false", "\"entityAliveAfter\":true")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"ticksToCompletion\":0", "\"ticksToCompletion\":-1")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        String fingerprint = currentFingerprint();
        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"catalogFingerprint\":\"" + fingerprint + "\"", "\"catalogFingerprint\":\"sha-256:stale\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));

        writeTemporaryArtifact(mutatedExactArtifact(entry -> replace(entry, "\"runId\":\"run-1\"", "\"runId\":\"run-2\"")));
        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedTemporaryArtifact(tempDir));
    }

    @Test
    void persistentArtifactIgnoresNewerActiveRunState() throws IOException {
        writeFrozenCatalog();
        String fingerprint = currentFingerprint();
        writePersistentArtifact(artifactJson(fingerprint, "old-run",
            entryJson("blazeandcave:nether/cultural_misunderstandings", "piglin", "blazeandcave:piglins", "minecraft:piglin"),
            entryJson("minecraft:adventure/kill_a_mob", "monster_hunter", "blazeandcave:hostile_monsters", "minecraft:zombie")
        ));
        writeRunState(fingerprint, "newer-run");

        Map<String, PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir);

        assertEquals(
            Set.of("minecraft:adventure/kill_a_mob", "blazeandcave:nether/cultural_misunderstandings"),
            evidence.keySet()
        );
    }

    @Test
    void persistentArtifactRejectsInternalRunIdMismatch() throws IOException {
        writeFrozenCatalog();
        String fingerprint = currentFingerprint();
        writePersistentArtifact(artifactJson(fingerprint, "old-run",
            entryJsonWithRunId(
                "blazeandcave:nether/cultural_misunderstandings",
                "piglin",
                "blazeandcave:piglins",
                "minecraft:piglin",
                "different-run"
            ),
            entryJson("minecraft:adventure/kill_a_mob", "monster_hunter", "blazeandcave:hostile_monsters", "minecraft:zombie")
        ));
        writeRunState(fingerprint, "old-run");

        assertThrows(IllegalStateException.class,
            () -> PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedRuntimeEvidence(tempDir));
    }

    @Test
    void persistentArtifactValidatesIndependently() throws IOException {
        Map<String, PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.RuntimeEvidenceData> evidence =
            PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.loadValidatedRuntimeEvidence(PROJECT_ROOT);

        assertEquals(2, evidence.size());
        assertEquals(
            Set.of("minecraft:adventure/kill_a_mob", "blazeandcave:nether/cultural_misunderstandings"),
            evidence.keySet()
        );
    }

    private void writeFrozenCatalog() throws IOException {
        writeCatalog(catalogJson(
            caseJson("blazeandcave:nether/cultural_misunderstandings", "piglin", "blazeandcave:piglins"),
            caseJson("minecraft:adventure/kill_a_mob", "monster_hunter", "blazeandcave:hostile_monsters")
        ));
    }

    private void writeCatalog(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.CATALOG_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeTemporaryArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.TEMPORARY_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
        Files.writeString(
            tempDir.resolve(PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.RUN_STATE_ARTIFACT),
            runStateJson(currentFingerprint(), "run-1"),
            StandardCharsets.UTF_8
        );
    }

    private void writePersistentArtifact(String json) throws IOException {
        Path path = tempDir.resolve(PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.PERSISTENT_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }

    private void writeRunState(String fingerprint, String runId) throws IOException {
        Path path = tempDir.resolve(PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.RUN_STATE_ARTIFACT);
        Files.createDirectories(path.getParent());
        Files.writeString(path, runStateJson(fingerprint, runId), StandardCharsets.UTF_8);
    }

    private String currentFingerprint() throws IOException {
        return PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.fingerprint(
            tempDir.resolve(PhaseAEntityTypeTagPlayerKilledEntityExecutionEvidenceValidation.CATALOG_PATH)
        );
    }

    private String mutatedExactArtifact(UnaryOperator<String> mutator) throws IOException {
        return mutator.apply(artifactJson(currentFingerprint(), "run-1",
            entryJson("minecraft:adventure/kill_a_mob", "monster_hunter", "blazeandcave:hostile_monsters", "minecraft:zombie")
        ));
    }

    private static String replace(String source, String from, String to) {
        return source.replace(from, to);
    }

    private static String entryJsonWithRunId(
        String advancementId,
        String criterion,
        String entityTypeTag,
        String selectedEntityType,
        String runId
    ) {
        return entryJson(advancementId, criterion, entityTypeTag, selectedEntityType)
            .replace("\"runId\":\"run-1\"", "\"runId\":\"" + runId + "\"");
    }

    private static String catalogJson(String... casesJson) {
        return "{"
            + "\"snapshot\":\"phase_a_entity_type_tag_player_killed_entity_case_catalog\","
            + "\"cases\":[" + String.join(",", casesJson) + "]"
            + "}";
    }

    private static String caseJson(String advancementId, String criterion, String entityTypeTag) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"trigger\":\"minecraft:player_killed_entity\","
            + "\"entityTypeTag\":\"" + entityTypeTag + "\","
            + "\"automationEligibility\":\"SUPPORTED\""
            + "}";
    }

    private static String artifactJson(String fingerprint, String runId, String... entriesJson) {
        String[] resolvedEntries = new String[entriesJson.length];
        for (int i = 0; i < entriesJson.length; i++) {
            resolvedEntries[i] = entriesJson[i].replace("__ENTRY_FINGERPRINT__", fingerprint).replace("\"runId\":\"run-1\"", "\"runId\":\"" + runId + "\"");
        }
        return "{"
            + "\"snapshot\":\"phase_a_entity_type_tag_player_killed_entity_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"" + runId + "\","
            + "\"generatedAt\":\"2026-08-24T00:00:00Z\","
            + "\"entries\":[" + String.join(",", resolvedEntries) + "]"
            + "}";
    }

    private static String runStateJson(String fingerprint, String runId) {
        return "{"
            + "\"snapshot\":\"phase_a_entity_type_tag_player_killed_entity_execution_evidence\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\","
            + "\"catalogFingerprint\":\"" + fingerprint + "\","
            + "\"runId\":\"" + runId + "\","
            + "\"startedAt\":\"2026-08-24T00:00:00Z\""
            + "}";
    }

    private static String entryJson(String advancementId, String criterion, String entityTypeTag, String selectedEntityType) {
        return "{"
            + "\"advancementId\":\"" + advancementId + "\","
            + "\"criterion\":\"" + criterion + "\","
            + "\"trigger\":\"minecraft:player_killed_entity\","
            + "\"entityTypeTag\":\"" + entityTypeTag + "\","
            + "\"selectedEntityType\":\"" + selectedEntityType + "\","
            + "\"tagExists\":true,"
            + "\"tagMemberCount\":38,"
            + "\"tagMembership\":true,"
            + "\"criterionBefore\":false,"
            + "\"criterionAfter\":true,"
            + "\"entityAliveBefore\":true,"
            + "\"entityAliveAfter\":false,"
            + "\"entityRemovedAfter\":true,"
            + "\"combatBoundary\":\"net.minecraft.server.level.ServerPlayer#attack(net.minecraft.world.entity.Entity)\","
            + "\"combatAction\":\"ServerPlayer.attack(minecraft:zombie)\","
            + "\"playerKillAttributed\":true,"
            + "\"ticksToCompletion\":0,"
            + "\"family\":\"ENTITY_TYPE_TAG_PLAYER_KILLED_ENTITY\","
            + "\"source\":\"PhaseAEntityTypeTagPlayerKilledEntityGameTest\","
            + "\"result\":\"GREEN\","
            + "\"catalogFingerprint\":\"__ENTRY_FINGERPRINT__\","
            + "\"runId\":\"run-1\","
            + "\"minecraftVersion\":\"26.2\","
            + "\"compatibilityMarker\":\"compat_26_2_r15\""
            + "}";
    }
}
