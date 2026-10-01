package com.diskree.achievetodo.certification;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;

class PhaseASingletonSilkTouchNestCertificationTest {
    @Test void frozenSourceAndCatalogMatchTheAssignedSingleton() throws Exception {
        PhaseASingletonSilkTouchNestCertification.validate(Path.of("").toAbsolutePath().normalize());
    }
}
