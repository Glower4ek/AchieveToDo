package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseAItemTagInventoryContainmentExecutionEvidenceValidationTest {
    @Test
    void promotedEvidenceExactlyCoversTheFrozenFourCaseCatalog() throws Exception {
        var evidence = PhaseAItemTagInventoryContainmentExecutionEvidenceValidation.loadValidatedRuntimeEvidence(
            Path.of("").toAbsolutePath().normalize()
        );
        assertEquals(4, evidence.size());
        for (PhaseAItemTagInventoryContainmentCertification.Case definition : PhaseAItemTagInventoryContainmentCertification.CASES) {
            assertEquals(java.util.Set.of(definition.criterion()), evidence.get(definition.advancementId()).greenCriteria());
        }
    }
}
