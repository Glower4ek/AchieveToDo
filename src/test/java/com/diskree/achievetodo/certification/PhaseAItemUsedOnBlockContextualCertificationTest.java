package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseAItemUsedOnBlockContextualCertificationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void frozenCatalogAndPersistentReceiptCoverTheExactSingleton() throws Exception {
        PhaseAItemUsedOnBlockContextualCertification.validate(ROOT);
        var evidence = PhaseAItemUsedOnBlockContextualExecutionEvidenceValidation.loadValidatedRuntimeEvidence(ROOT);
        assertEquals(Set.of(PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID), evidence.keySet());
        assertEquals(Set.of(PhaseAItemUsedOnBlockContextualCertification.CRITERION),
            evidence.get(PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID).greenCriteria());
    }

    @Test
    void persistedReceiptChangesOnlyJukeboxAdvancementStatus() throws Exception {
        var current = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);
        var previous = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);
        previous.remove(PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID);
        JsonObject before = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, previous)).getAsJsonObject();
        JsonObject after = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, current)).getAsJsonObject();
        var oldEntries = before.getAsJsonArray("entries"); var newEntries = after.getAsJsonArray("entries");
        assertEquals(1152, oldEntries.size()); assertEquals(1152, newEntries.size());
        int changed = 0;
        for (int i = 0; i < oldEntries.size(); i++) {
            var oldEntry = oldEntries.get(i).getAsJsonObject(); var newEntry = newEntries.get(i).getAsJsonObject();
            assertEquals(oldEntry.get("id"), newEntry.get("id"));
            if (!oldEntry.get("advancementStatus").equals(newEntry.get("advancementStatus"))) {
                assertEquals(PhaseAItemUsedOnBlockContextualCertification.ADVANCEMENT_ID, newEntry.get("id").getAsString());
                assertEquals("RUNTIME_DEFERRED", oldEntry.get("advancementStatus").getAsString());
                assertEquals("RUNTIME_CERTIFIED", newEntry.get("advancementStatus").getAsString());
                changed++;
            }
        }
        assertEquals(1, changed);
        assertEquals(before.getAsJsonObject("summary").get("totalCertified").getAsInt() + 1,
            after.getAsJsonObject("summary").get("totalCertified").getAsInt());
    }
}
