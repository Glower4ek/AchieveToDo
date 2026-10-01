package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseASingletonDiagonAllayCertificationTest {
    @Test
    void frozenAllayPotionPickupCatalogMatchesExactly() throws Exception {
        PhaseASingletonDiagonAllayCertification.validate(Path.of("").toAbsolutePath().normalize());
    }

    @Test
    void persistentAllayDeliveryReceiptCoversOnlyTheFrozenCriterion() throws Exception {
        var evidence = PhaseASingletonDiagonAllayExecutionEvidenceValidation.loadValidatedRuntimeEvidence(
            Path.of("").toAbsolutePath().normalize());
        assertEquals(Set.of(PhaseASingletonDiagonAllayCertification.ADVANCEMENT), evidence.keySet());
        assertEquals(Set.of(PhaseASingletonDiagonAllayCertification.CRITERION),
            evidence.get(PhaseASingletonDiagonAllayCertification.ADVANCEMENT).greenCriteria());
    }

    @Test
    void persistentAllayReceiptChangesOnlyItsOwnStatus() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        var current = PhaseAAdvancementRollup.loadRuntimeEvidence(root);
        var previous = PhaseAAdvancementRollup.loadRuntimeEvidence(root);
        previous.remove(PhaseASingletonDiagonAllayCertification.ADVANCEMENT);
        JsonObject before = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(root, previous)).getAsJsonObject();
        JsonObject after = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(root, current)).getAsJsonObject();
        var oldEntries = before.getAsJsonArray("entries"); var newEntries = after.getAsJsonArray("entries");
        assertEquals(1152, oldEntries.size()); assertEquals(1152, newEntries.size());
        int changed = 0;
        for (int i = 0; i < oldEntries.size(); i++) {
            var oldEntry = oldEntries.get(i).getAsJsonObject(); var newEntry = newEntries.get(i).getAsJsonObject();
            assertEquals(oldEntry.get("id"), newEntry.get("id"));
            if (!oldEntry.get("advancementStatus").equals(newEntry.get("advancementStatus"))) {
                assertEquals(PhaseASingletonDiagonAllayCertification.ADVANCEMENT, newEntry.get("id").getAsString());
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
