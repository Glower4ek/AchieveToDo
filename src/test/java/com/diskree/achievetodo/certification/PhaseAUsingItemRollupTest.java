package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAUsingItemRollupTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void usingItemAdapterPreservesFamilyAndSourceAttribution() {
        PhaseAUsingItemExecutionEvidenceValidation.RuntimeEvidenceData loud =
            PhaseAUsingItemExecutionEvidenceValidation.RuntimeEvidenceData.empty(
                "blazeandcave:animal/loud_and_proud"
            );
        loud.greenCriteria().add("goat_horn");
        loud.criteriaByFamily().computeIfAbsent("USING_ITEM", ignored -> new HashSet<>()).add("goat_horn");
        loud.criteriaBySource().computeIfAbsent("PhaseAUsingItemGameTest", ignored -> new HashSet<>()).add("goat_horn");
        loud.families().add("USING_ITEM");
        loud.sources().add("PhaseAUsingItemGameTest");

        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> adapted =
            PhaseAAdvancementRollup.adaptUsingItemEvidence(Map.of(loud.advancementId(), loud));
        PhaseAAdvancementRollup.RuntimeEvidence result = adapted.get(loud.advancementId());
        assertEquals(Set.of("goat_horn"), result.greenCriteria());
        assertEquals(Set.of("USING_ITEM"), result.families());
        assertEquals(Set.of("PhaseAUsingItemGameTest"), result.sources());
    }

    @Test
    void persistedUsingItemEvidenceIsVisibleToCanonicalRollupLoader() throws IOException {
        Map<String, PhaseAAdvancementRollup.RuntimeEvidence> evidence =
            PhaseAAdvancementRollup.loadRuntimeEvidence(PROJECT_ROOT);
        PhaseAAdvancementRollup.RuntimeEvidence loud = evidence.get("blazeandcave:animal/loud_and_proud");
        PhaseAAdvancementRollup.RuntimeEvidence barrel = evidence.get("blazeandcave:enchanting/do_a_barrel_roll");
        assertTrue(loud != null && loud.greenCriteria().contains("goat_horn"));
        assertTrue(barrel != null && barrel.greenCriteria().contains("riptide"));
        assertTrue(loud.families().contains("USING_ITEM"));
        assertTrue(barrel.sources().contains("PhaseAUsingItemGameTest"));
    }
}
