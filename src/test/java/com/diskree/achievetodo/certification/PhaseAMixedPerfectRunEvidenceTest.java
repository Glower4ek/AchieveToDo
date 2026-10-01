package com.diskree.achievetodo.certification;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PhaseAMixedPerfectRunEvidenceTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    @Test void rejectsDamageIdentityTimerAndNativeLifecycleCorruption(@TempDir Path fixture) throws Exception {
        for (var file : List.of(PhaseAMixedPerfectRunCertification.PERSISTENT, PhaseAMixedPerfectRunCertification.CATALOG, Path.of("reference/phase_a_preservation/files/final/bacap.zip"))) {
            Files.createDirectories(fixture.resolve(file).getParent()); Files.copy(ROOT.resolve(file), fixture.resolve(file));
        }
        var target = fixture.resolve(PhaseAMixedPerfectRunCertification.PERSISTENT);
        String accepted = Files.readString(target);
        for (String field : List.of("criterionBefore", "playerUuid", "runId", "frozenTimerAfterExpiry", "noDamageWitness.bac_pr_dmgt", "raidWitness.nativeVictory", "cleanup.warningCount", "cleanup.chunkTicketsRestored")) {
            var json = JsonParser.parseString(accepted).getAsJsonObject(); var receipt = json.getAsJsonArray("entries").get(0).getAsJsonObject();
            var parts = field.split("\\."); var owner = parts.length == 1 ? receipt : receipt.getAsJsonObject(parts[0]); String key = parts[parts.length - 1];
            switch (field) {
                case "playerUuid", "runId" -> owner.addProperty(key, UUID.randomUUID().toString());
                case "criterionBefore" -> owner.addProperty(key, true);
                case "frozenTimerAfterExpiry", "noDamageWitness.bac_pr_dmgt", "cleanup.warningCount" -> owner.addProperty(key, 1);
                default -> owner.addProperty(key, false);
            }
            Files.writeString(target, json.toString());
            assertThrows(Exception.class, () -> PhaseAMixedPerfectRunEvidenceValidation.loadValidatedRuntimeEvidence(fixture), field);
        }
    }
    @Test void persistentExactSetIgnoresActiveStateAndRejectsMissingDuplicateOrCorruptedWitnesses(@TempDir Path fixture) throws Exception {
        Path evidence = PhaseAMixedPerfectRunCertification.PERSISTENT;
        for (var file : List.of(evidence, PhaseAMixedPerfectRunCertification.CATALOG, Path.of("reference/phase_a_preservation/files/final/bacap.zip"))) {
            Files.createDirectories(fixture.resolve(file).getParent()); Files.copy(ROOT.resolve(file), fixture.resolve(file));
        }
        var state = fixture.resolve(PhaseAMixedPerfectRunEvidenceValidation.RUN_STATE);
        Files.createDirectories(state.getParent()); Files.writeString(state, "{\"runId\":\"different-new-active-state\"}");
        assertEquals(1, PhaseAMixedPerfectRunEvidenceValidation.loadValidatedRuntimeEvidence(fixture).size());
        String accepted = Files.readString(fixture.resolve(evidence));
        var json = JsonParser.parseString(accepted).getAsJsonObject(); json.getAsJsonArray("entries").remove(0); Files.writeString(fixture.resolve(evidence), json.toString());
        assertThrows(Exception.class, () -> PhaseAMixedPerfectRunEvidenceValidation.loadValidatedRuntimeEvidence(fixture));
        json = JsonParser.parseString(accepted).getAsJsonObject(); var entries = json.getAsJsonArray("entries"); entries.add(entries.get(0).deepCopy()); Files.writeString(fixture.resolve(evidence), json.toString());
        assertThrows(Exception.class, () -> PhaseAMixedPerfectRunEvidenceValidation.loadValidatedRuntimeEvidence(fixture));
        json = JsonParser.parseString(accepted).getAsJsonObject(); json.getAsJsonArray("entries").get(0).getAsJsonObject().addProperty("spawnPrerequisiteAfter", false); Files.writeString(fixture.resolve(evidence), json.toString());
        assertThrows(Exception.class, () -> PhaseAMixedPerfectRunEvidenceValidation.loadValidatedRuntimeEvidence(fixture));
    }
    @Test void evidenceCompletesAllFrozenGroupsAndPreservesOtherCanonicalEntries() throws Exception {
        var all = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);
        for (var id : PhaseAMixedPerfectRunCertification.FROZEN.keySet()) {
            assertEquals(Set.of(PhaseAMixedPerfectRunCertification.FAMILY), all.get(id).families());
        }
        var with = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, all)).getAsJsonObject();
        var beforeEvidence = new LinkedHashMap<>(all); PhaseAMixedPerfectRunCertification.FROZEN.keySet().forEach(beforeEvidence::remove);
        var before = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, beforeEvidence)).getAsJsonObject();
        var oldEntries = new HashMap<String, Object>(); for (var entry : before.getAsJsonArray("entries")) oldEntries.put(entry.getAsJsonObject().get("id").getAsString(), entry);
        int transitions = 0;
        for (var entry : with.getAsJsonArray("entries")) {
            var row = entry.getAsJsonObject(); String id = row.get("id").getAsString();
            if (PhaseAMixedPerfectRunCertification.FROZEN.containsKey(id)) {
                assertEquals("RUNTIME_CERTIFIED", row.get("advancementStatus").getAsString()); assertTrue(row.get("requirementsSatisfied").getAsBoolean()); transitions++;
            } else assertEquals(oldEntries.get(id), row, id);
        }
        assertEquals(1, transitions);
        assertEquals(1, with.getAsJsonObject("summary").get("totalCertified").getAsInt() - before.getAsJsonObject("summary").get("totalCertified").getAsInt());
    }
}
