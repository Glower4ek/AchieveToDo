package com.diskree.achievetodo.certification;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PhaseAFishingRodHookedCertificationTest {
    private static final Path ROOT = Path.of("").toAbsolutePath().normalize();

    @Test
    void frozenSingletonDefinitionAndCatalogRemainExact() throws Exception {
        PhaseAFishingRodHookedCertification.validate(ROOT);
        JsonObject catalog = JsonParser.parseString(Files.readString(ROOT.resolve(PhaseAFishingRodHookedCertification.SNAPSHOT))).getAsJsonObject();
        assertEquals(1, catalog.get("advancementCount").getAsInt());
        assertEquals(1, catalog.get("criterionCount").getAsInt());
        assertEquals(1, catalog.get("requirementGroupCount").getAsInt());
        assertEquals(1, catalog.getAsJsonArray("cases").size());
        JsonObject oneCase = catalog.getAsJsonArray("cases").get(0).getAsJsonObject();
        assertEquals("blazeandcave:weaponry/indiana_jones#indiana_jones",
            oneCase.get("advancementId").getAsString() + "#" + oneCase.get("criterion").getAsString());
        assertEquals(0, oneCase.get("requirementGroupIndex").getAsInt());
        assertEquals("NONE", oneCase.get("itemPredicate").getAsString());
    }
}
