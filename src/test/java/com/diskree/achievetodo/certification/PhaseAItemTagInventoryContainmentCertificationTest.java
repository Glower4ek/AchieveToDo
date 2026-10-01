package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;

class PhaseAItemTagInventoryContainmentCertificationTest {
    @Test void frozenFourCaseDefinitionRemainsExact() throws Exception {
        PhaseAItemTagInventoryContainmentCertification.validate(Path.of("").toAbsolutePath().normalize());
    }
}
