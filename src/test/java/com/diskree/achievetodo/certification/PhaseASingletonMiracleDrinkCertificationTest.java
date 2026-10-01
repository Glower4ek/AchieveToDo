package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseASingletonMiracleDrinkCertificationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    @Test void frozenSourceAndCatalogMatchTheAssignedSingleton() throws Exception {
        PhaseASingletonMiracleDrinkCertification.validate(ROOT);
    }
    @Test void persistentEvidenceCoversOnlyTheMiracleDrinkSingleton() throws Exception {
        var evidence = PhaseASingletonMiracleDrinkExecutionEvidenceValidation.loadValidatedRuntimeEvidence(ROOT);
        assertEquals(Set.of(PhaseASingletonMiracleDrinkCertification.ADVANCEMENT), evidence.keySet());
        assertEquals(Set.of(PhaseASingletonMiracleDrinkCertification.CRITERION),
            evidence.get(PhaseASingletonMiracleDrinkCertification.ADVANCEMENT).greenCriteria());
    }
    @Test void persistentMiracleDrinkReceiptChangesOnlyItsOwnStatus() throws Exception {
        var current = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);
        var previous = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);
        previous.remove(PhaseASingletonMiracleDrinkCertification.ADVANCEMENT);
        JsonObject before = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, previous)).getAsJsonObject();
        JsonObject after = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, current)).getAsJsonObject();
        var oldEntries = before.getAsJsonArray("entries"); var newEntries = after.getAsJsonArray("entries");
        assertEquals(1152, oldEntries.size()); assertEquals(1152, newEntries.size());
        int changed = 0;
        for (int i = 0; i < oldEntries.size(); i++) {
            var oldEntry = oldEntries.get(i).getAsJsonObject(); var newEntry = newEntries.get(i).getAsJsonObject();
            assertEquals(oldEntry.get("id"), newEntry.get("id"));
            if (!oldEntry.get("advancementStatus").equals(newEntry.get("advancementStatus"))) {
                assertEquals(PhaseASingletonMiracleDrinkCertification.ADVANCEMENT, newEntry.get("id").getAsString());
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
