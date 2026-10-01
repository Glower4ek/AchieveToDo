package com.diskree.achievetodo.certification;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseARecipeCraftedWaxOnCertificationTest {
    @Test void catalogMatchesFrozenDefinition() throws Exception {
        Path root = Path.of("").toAbsolutePath().normalize();
        assertEquals(JsonParser.parseString(PhaseARecipeCraftedWaxOnCertification.generateSnapshot(root)),
            JsonParser.parseString(Files.readString(root.resolve(PhaseARecipeCraftedWaxOnCertification.SNAPSHOT))));
    }
}
