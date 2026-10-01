package com.diskree.achievetodo.certification;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseATrimMaterialInventoryChangedCertificationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void frozenCatalogAndPersistentRuntimeCoverTheExactFamily() throws Exception {
        PhaseATrimMaterialInventoryChangedCertification.validate(ROOT);
        var validated = PhaseATrimMaterialInventoryChangedExecutionEvidenceValidation.loadValidatedRuntimeEvidence(ROOT);
        assertEquals(Set.of("blazeandcave:adventure/chromatic_armory", "blazeandcave:adventure/coordinated_flair"), validated.keySet());
        assertEquals(11, validated.get("blazeandcave:adventure/chromatic_armory").greenCriteria().size());
        assertEquals(11, validated.get("blazeandcave:adventure/coordinated_flair").greenCriteria().size());
    }

    @Test
    void exactEvidenceChangesOnlyTheTwoFamilyAdvancementStatuses() throws Exception {
        var current = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);
        var previous = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT);
        for (var definition : PhaseATrimMaterialInventoryChangedCertification.cases()) previous.remove(definition.advancementId());
        JsonObject before = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, previous)).getAsJsonObject();
        JsonObject after = JsonParser.parseString(PhaseAAdvancementRollup.generateSnapshot(ROOT, current)).getAsJsonObject();
        JsonArray oldEntries = before.getAsJsonArray("entries"), newEntries = after.getAsJsonArray("entries");
        assertEquals(1152, oldEntries.size()); assertEquals(1152, newEntries.size());
        Set<String> changed = new LinkedHashSet<>();
        for (int i = 0; i < oldEntries.size(); i++) {
            JsonObject oldEntry = oldEntries.get(i).getAsJsonObject(), newEntry = newEntries.get(i).getAsJsonObject();
            assertEquals(oldEntry.get("id"), newEntry.get("id"));
            if (!oldEntry.get("advancementStatus").equals(newEntry.get("advancementStatus"))) {
                assertEquals("RUNTIME_DEFERRED", oldEntry.get("advancementStatus").getAsString());
                assertEquals("RUNTIME_CERTIFIED", newEntry.get("advancementStatus").getAsString());
                changed.add(newEntry.get("id").getAsString());
            }
        }
        assertEquals(Set.of("blazeandcave:adventure/chromatic_armory", "blazeandcave:adventure/coordinated_flair"), changed);
        JsonObject oldSummary = before.getAsJsonObject("summary"), newSummary = after.getAsJsonObject("summary");
        assertEquals(oldSummary.get("totalCertified").getAsInt() + 2, newSummary.get("totalCertified").getAsInt());
        assertEquals(oldSummary.getAsJsonObject("statusTotals").get("RUNTIME_DEFERRED").getAsInt() - 2,
            newSummary.getAsJsonObject("statusTotals").get("RUNTIME_DEFERRED").getAsInt());
        assertTrue(newSummary.get("totalCertified").getAsInt() <= 1152);
    }
}
