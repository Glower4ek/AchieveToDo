package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;

class PhaseASingletonAncientRestorationCertificationTest {
    @Test void frozenSourceAndCatalogMatchTheAssignedSingleton() throws Exception {
        PhaseASingletonAncientRestorationCertification.validate(Path.of("").toAbsolutePath().normalize());
    }
}
