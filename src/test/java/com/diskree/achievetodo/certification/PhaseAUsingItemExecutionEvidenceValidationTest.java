package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PhaseAUsingItemExecutionEvidenceValidationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void persistentUsingItemEvidenceIsFailClosedAndComplete() throws IOException {
        PhaseAUsingItemExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAUsingItemExecutionEvidenceValidation.loadValidatedPersistentArtifact(PROJECT_ROOT);
        assertEquals(2, artifact.entries().size());
        assertEquals(2, artifact.entries().stream()
            .map(entry -> entry.get("advancementId").getAsString() + "#" + entry.get("criterion").getAsString())
            .distinct().count());
        assertTrue(artifact.entries().stream().allMatch(entry -> "GREEN".equals(entry.get("result").getAsString())));
    }

    @Test
    void persistentArtifactUsesCatalogFingerprint() throws IOException {
        PhaseAUsingItemExecutionEvidenceValidation.RuntimeArtifact artifact =
            PhaseAUsingItemExecutionEvidenceValidation.loadValidatedPersistentArtifact(PROJECT_ROOT);
        assertEquals(
            PhaseAUsingItemExecutionEvidenceValidation.currentCatalogFingerprint(PROJECT_ROOT),
            artifact.catalogFingerprint()
        );
    }
}
