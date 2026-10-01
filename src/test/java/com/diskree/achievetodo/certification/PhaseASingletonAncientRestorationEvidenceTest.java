package com.diskree.achievetodo.certification;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PhaseASingletonAncientRestorationEvidenceTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test void persistentProofIsArtifactLocalAndRejectsTampering(@TempDir Path fixture) throws Exception {
        var catalog = PhaseASingletonAncientRestorationCertification.SNAPSHOT;
        var evidence = PhaseASingletonAncientRestorationCertification.PERSISTENT;
        var bacap = Path.of("reference/phase_a_preservation/files/final/bacap.zip");
        for (Path file : new Path[]{catalog, evidence, bacap}) {
            Files.createDirectories(fixture.resolve(file).getParent());
            Files.copy(ROOT.resolve(file), fixture.resolve(file));
        }
        // Active transient state deliberately differs from the accepted persistent run.
        var state = fixture.resolve("build/tmp/phase_a_certification/singleton_ancient_restoration_execution_evidence.run.json");
        Files.createDirectories(state.getParent());
        Files.writeString(state, "{\"runId\":\"different-active-run\"}");
        var valid = PhaseASingletonAncientRestorationExecutionEvidenceValidation.loadValidatedRuntimeEvidence(fixture);
        assertEquals(Set.of(PhaseASingletonAncientRestorationCertification.ADVANCEMENT), valid.keySet());
        var json = JsonParser.parseString(Files.readString(fixture.resolve(evidence))).getAsJsonObject();
        var receipt = json.getAsJsonArray("entries").get(0).getAsJsonObject();
        receipt.addProperty("runId", "different-receipt-run");
        Files.writeString(fixture.resolve(evidence), json.toString());
        assertThrows(IllegalStateException.class,
            () -> PhaseASingletonAncientRestorationExecutionEvidenceValidation.loadValidatedRuntimeEvidence(fixture));
        receipt.addProperty("runId", json.get("runId").getAsString());
        receipt.getAsJsonObject("cleanup").addProperty("warningCount", 1);
        Files.writeString(fixture.resolve(evidence), json.toString());
        assertThrows(IllegalStateException.class,
            () -> PhaseASingletonAncientRestorationExecutionEvidenceValidation.loadValidatedRuntimeEvidence(fixture));
    }

    @Test void acceptedEvidenceCompletesOnlyTheAssignedRequirement() throws Exception {
        var id = PhaseASingletonAncientRestorationCertification.ADVANCEMENT;
        var proof = PhaseAAdvancementRollup.loadRuntimeEvidence(ROOT).get(id);
        assertEquals(Set.of(PhaseASingletonAncientRestorationCertification.CRITERION), proof.greenCriteria());
        assertEquals(Set.of(PhaseASingletonAncientRestorationCertification.FAMILY), proof.families());
        assertEquals(Set.of(PhaseASingletonAncientRestorationCertification.SOURCE), proof.sources());
    }
}
