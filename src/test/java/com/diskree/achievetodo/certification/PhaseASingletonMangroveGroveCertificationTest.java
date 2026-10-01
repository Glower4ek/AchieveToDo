package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseASingletonMangroveGroveCertificationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void frozenCatalogMatchesTheExactPropaguleAndGrovePredicate() throws Exception {
        PhaseASingletonMangroveGroveCertification.validate(ROOT);
    }

    @Test
    void frozenCatalogAndPersistentReceiptCoverOnlyTheClickSingleton() throws Exception {
        PhaseASingletonMangroveGroveCertification.validate(ROOT);
        var evidence = PhaseASingletonMangroveGroveExecutionEvidenceValidation.loadValidatedRuntimeEvidence(ROOT);
        assertEquals(Set.of(PhaseASingletonMangroveGroveCertification.ADVANCEMENT), evidence.keySet());
        assertEquals(Set.of(PhaseASingletonMangroveGroveCertification.CRITERION),
            evidence.get(PhaseASingletonMangroveGroveCertification.ADVANCEMENT).greenCriteria());
    }

    @Test
    void persistentClickReceiptChangesOnlyItsOwnStatus() throws Exception {
        var current = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);
        var previous = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);
        previous.remove(PhaseASingletonMangroveGroveCertification.ADVANCEMENT);
        JsonObject before = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, previous)).getAsJsonObject();
        JsonObject after = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, current)).getAsJsonObject();
        var oldEntries = before.getAsJsonArray("entries"); var newEntries = after.getAsJsonArray("entries");
        assertEquals(1152, oldEntries.size()); assertEquals(1152, newEntries.size());
        int changed = 0;
        for (int i = 0; i < oldEntries.size(); i++) {
            var oldEntry = oldEntries.get(i).getAsJsonObject(); var newEntry = newEntries.get(i).getAsJsonObject();
            assertEquals(oldEntry.get("id"), newEntry.get("id"));
            if (!oldEntry.get("advancementStatus").equals(newEntry.get("advancementStatus"))) {
                assertEquals(PhaseASingletonMangroveGroveCertification.ADVANCEMENT, newEntry.get("id").getAsString());
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
