package com.diskree.achievetodo.certification;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PhaseALocationHolderSetWorldgenEvidenceTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    @Test void persistentExactSetIgnoresActiveStateAndRejectsMissingDuplicateOrCorruptedWitnesses(@TempDir Path fixture) throws Exception {
        Path evidence = PhaseALocationHolderSetWorldgenCertification.PERSISTENT;
        for (var file : List.of(evidence, PhaseALocationHolderSetWorldgenCertification.CATALOG, Path.of("reference/phase_a_preservation/files/final/bacap.zip"))) {
            Files.createDirectories(fixture.resolve(file).getParent()); Files.copy(ROOT.resolve(file), fixture.resolve(file));
        }
        var state = fixture.resolve(PhaseALocationHolderSetWorldgenEvidenceValidation.RUN_STATE);
        Files.createDirectories(state.getParent()); Files.writeString(state, "{\"runId\":\"different-new-active-state\"}");
        assertEquals(13, PhaseALocationHolderSetWorldgenEvidenceValidation.loadValidatedRuntimeEvidence(fixture).size());
        String accepted = Files.readString(fixture.resolve(evidence));
        var json = JsonParser.parseString(accepted).getAsJsonObject(); json.getAsJsonArray("entries").remove(0); Files.writeString(fixture.resolve(evidence), json.toString());
        assertThrows(Exception.class, () -> PhaseALocationHolderSetWorldgenEvidenceValidation.loadValidatedRuntimeEvidence(fixture));
        json = JsonParser.parseString(accepted).getAsJsonObject(); var entries = json.getAsJsonArray("entries"); entries.add(entries.get(0).deepCopy()); Files.writeString(fixture.resolve(evidence), json.toString());
        assertThrows(Exception.class, () -> PhaseALocationHolderSetWorldgenEvidenceValidation.loadValidatedRuntimeEvidence(fixture));
        json = JsonParser.parseString(accepted).getAsJsonObject(); json.getAsJsonArray("entries").get(0).getAsJsonObject().getAsJsonObject("contextWitness").addProperty("notSpectator", false); Files.writeString(fixture.resolve(evidence), json.toString());
        assertThrows(Exception.class, () -> PhaseALocationHolderSetWorldgenEvidenceValidation.loadValidatedRuntimeEvidence(fixture));
    }
    @Test void evidenceCompletesAllFrozenGroupsAndPreservesOtherCanonicalEntries() throws Exception {
        var all = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);
        for (var id : PhaseALocationHolderSetWorldgenCertification.FROZEN.keySet()) {
            assertEquals(Set.of(PhaseALocationHolderSetWorldgenCertification.FAMILY), all.get(id).families());
        }
        var with = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, all)).getAsJsonObject();
        var beforeEvidence = new LinkedHashMap<>(all); PhaseALocationHolderSetWorldgenCertification.FROZEN.keySet().forEach(beforeEvidence::remove);
        var before = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, beforeEvidence)).getAsJsonObject();
        var oldEntries = new HashMap<String, Object>(); for (var entry : before.getAsJsonArray("entries")) oldEntries.put(entry.getAsJsonObject().get("id").getAsString(), entry);
        int transitions = 0;
        for (var entry : with.getAsJsonArray("entries")) {
            var row = entry.getAsJsonObject(); String id = row.get("id").getAsString();
            if (PhaseALocationHolderSetWorldgenCertification.FROZEN.containsKey(id)) {
                assertEquals("RUNTIME_CERTIFIED", row.get("advancementStatus").getAsString()); assertTrue(row.get("requirementsSatisfied").getAsBoolean()); transitions++;
            } else assertEquals(oldEntries.get(id), row, id);
        }
        assertEquals(13, transitions);
        assertEquals(13, with.getAsJsonObject("summary").get("totalCertified").getAsInt() - before.getAsJsonObject("summary").get("totalCertified").getAsInt());
    }
}
