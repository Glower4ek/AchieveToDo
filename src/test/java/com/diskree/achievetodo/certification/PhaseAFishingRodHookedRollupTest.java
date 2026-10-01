package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class PhaseAFishingRodHookedRollupTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();
    private static final String ADVANCEMENT = PhaseAFishingRodHookedCertification.ADVANCEMENT_ID;
    private static final String CRITERION = PhaseAFishingRodHookedCertification.CRITERION;

    @Test
    void exactSingletonEvidenceClassifiesTheMappedAdvancementAsRuntimeCertified() {
        var source = new PhaseAFishingRodHookedExecutionEvidenceValidation.RuntimeEvidenceData(
            ADVANCEMENT,
            new TreeSet<>(Set.of(CRITERION)),
            new LinkedHashMap<>(Map.of(PhaseAFishingRodHookedCertification.FAMILY, new TreeSet<>(Set.of(CRITERION)))),
            new LinkedHashMap<>(Map.of(PhaseAFishingRodHookedCertification.SOURCE, new TreeSet<>(Set.of(CRITERION)))),
            new LinkedHashSet<>(Set.of(PhaseAFishingRodHookedCertification.FAMILY)),
            new LinkedHashSet<>(Set.of(PhaseAFishingRodHookedCertification.SOURCE))
        );
        var evidence = PhaseAAdvancementRollup.adaptFishingRodHookedEvidence(Map.of(ADVANCEMENT, source)).get(ADVANCEMENT);
        JsonObject rawAdvancement = new JsonObject();
        JsonObject criteria = new JsonObject();
        criteria.add(CRITERION, new JsonObject());
        rawAdvancement.add("criteria", criteria);

        assertEquals(1, PhaseAAdvancementRollup.completionRequirements(rawAdvancement).size());
        assertEquals(PhaseAAdvancementRollup.RollupStatus.RUNTIME_CERTIFIED,
            PhaseAAdvancementRollup.classify(PhaseACertification.StaticValidationStatus.RUNTIME_DEFERRED, rawAdvancement, evidence));
        assertTrue(evidence.families().contains(PhaseAFishingRodHookedCertification.FAMILY));
        assertTrue(evidence.sources().contains(PhaseAFishingRodHookedCertification.SOURCE));
    }

    @Test
    void persistentEvidenceIsArtifactLocalAndEntersTheCanonicalRuntimeMerge(@TempDir Path tempDir) throws Exception {
        Path persistent = ROOT.resolve(PhaseAFishingRodHookedCertification.PERSISTENT_EVIDENCE);
        assumeTrue(Files.exists(persistent), "persistent evidence is created only after exact promotion");

        Path temporaryCatalog = tempDir.resolve(PhaseAFishingRodHookedCertification.SNAPSHOT);
        Path temporaryPersistent = tempDir.resolve(PhaseAFishingRodHookedCertification.PERSISTENT_EVIDENCE);
        Path temporaryBacap = tempDir.resolve("reference/phase_a_preservation/files/final/bacap.zip");
        Files.createDirectories(temporaryCatalog.getParent());
        Files.createDirectories(temporaryPersistent.getParent());
        Files.createDirectories(temporaryBacap.getParent());
        Files.copy(ROOT.resolve(PhaseAFishingRodHookedCertification.SNAPSHOT), temporaryCatalog);
        Files.copy(ROOT.resolve("reference/phase_a_preservation/files/final/bacap.zip"),
            temporaryBacap);
        Files.copy(persistent, temporaryPersistent);

        var artifactLocal = PhaseAFishingRodHookedExecutionEvidenceValidation.loadPersistentArtifact(tempDir);
        assertEquals(1, artifactLocal.entries().size());
        assertEquals(ADVANCEMENT, artifactLocal.entries().getFirst().get("advancementId").getAsString());

        var runtimeEvidence = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT).get(ADVANCEMENT);
        assertTrue(runtimeEvidence.greenCriteria().contains(CRITERION));
        assertTrue(runtimeEvidence.families().contains(PhaseAFishingRodHookedCertification.FAMILY));
    }
}
